package com.enterpriseai.backend.ai.tool.impl;

import com.enterpriseai.backend.ai.tool.Tool;
import com.enterpriseai.backend.ai.tool.ToolContext;
import com.enterpriseai.backend.ai.tool.ToolParameter;
import com.enterpriseai.backend.ai.tool.ToolResult;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BrowserTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(BrowserTool.class);
    private static final int MAX_TEXT_LENGTH = 10000;
    
    private Playwright playwright;
    private Browser browser;
    private SafeProxy proxyServer;

    @Override
    public String getId() {
        return "browser_tool";
    }

    @Override
    public boolean requiresInternet() {
        return true;
    }

    @Override
    public String getName() {
        return "Browser Web Reader";
    }

    @Override
    public String getCategory() {
        return "Research";
    }

    @Override
    public String getDescription() {
        return "Open a specific URL in a headless browser and read its content. Use this to read articles, documentation, or search results deeply. IMPORTANT: Provide a direct URL (http or https).";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
                new ToolParameter("url", "string", "The absolute URL to visit (must start with http:// or https://)", true)
        );
    }

    private synchronized void initPlaywright() {
        if (playwright == null) {
            try {
                log.info("Initializing SafeProxy...");
                proxyServer = new SafeProxy();
                log.info("SafeProxy listening on port {}", proxyServer.getPort());

                playwright = Playwright.create(new Playwright.CreateOptions()
                        .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setChannel("chrome")
                        .setProxy(new Proxy("http://127.0.0.1:" + proxyServer.getPort())));
                log.info("Playwright initialized securely with SafeProxy.");
            } catch (IOException e) {
                log.error("Failed to initialize SafeProxy", e);
                throw new RuntimeException("Failed to initialize secure network proxy", e);
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
        }
        if (proxyServer != null) {
            try { proxyServer.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolContext context) {
        String url = (String) parameters.get("url");
        if (url == null || url.trim().isEmpty()) {
            return new ToolResult(false, "URL is required.");
        }

        if (!url.startsWith("https://")) {
            if (url.startsWith("http://")) {
                return new ToolResult(false, "SECURITY ERROR: Only https:// URLs are allowed to prevent HTTP-based subresource SSRF.");
            }
            return new ToolResult(false, "SECURITY ERROR: Only https:// URLs are allowed.");
        }

        BrowserContext browserContext = null;
        Page page = null;
        try {
            initPlaywright();
            browserContext = browser.newContext(new Browser.NewContextOptions().setAcceptDownloads(false));
            browserContext.setDefaultNavigationTimeout(10000); // 10s
            browserContext.setDefaultTimeout(10000);
            
            // Strictly abort any plaintext HTTP requests to force everything through HTTPS CONNECT in our proxy
            browserContext.route("http://**", route -> route.abort());
            
            page = browserContext.newPage();
            
            log.info("BrowserTool navigating to: {}", url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            
            String title = page.title();
            
            String extractedText = (String) page.evaluate("() => { " +
                    "const body = document.body.cloneNode(true); " +
                    "const scripts = body.querySelectorAll('script, style, nav, footer, iframe, noscript'); " +
                    "scripts.forEach(s => s.remove()); " +
                    "return body.innerText; " +
                    "}");
                    
            if (extractedText == null) {
                extractedText = "";
            }
            
            extractedText = extractedText.trim();
            if (extractedText.length() > MAX_TEXT_LENGTH) {
                extractedText = extractedText.substring(0, MAX_TEXT_LENGTH) + "\n...[CONTENT TRUNCATED]...";
            }
            
            if (extractedText.isEmpty()) {
                return new ToolResult(true, "Page loaded successfully but no readable text could be extracted.");
            }
            
            String markdown = "### " + title + "\n" +
                              "**Source**: " + page.url() + "\n\n" +
                              extractedText;
                              
            return new ToolResult(true, markdown);
            
        } catch (Exception e) {
            log.error("BrowserTool error for url: {}", url, e);
            if (e.getMessage() != null && e.getMessage().contains("ERR_TUNNEL_CONNECTION_FAILED")) {
                return new ToolResult(false, "Browser failed to load the page: Access denied by security proxy (SSRF blocked)");
            }
            return new ToolResult(false, "Browser failed to load the page: " + e.getMessage());
        } finally {
            if (page != null) {
                try { page.close(); } catch (Exception ignored) {}
            }
            if (browserContext != null) {
                try { browserContext.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Embedded proxy that intercepts all Playwright traffic.
     * Prevents DNS Rebinding by resolving the hostname itself, verifying it,
     * and opening the TCP socket exactly to the verified IP.
     */
    public static class SafeProxy implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private volatile boolean running = true;

        public SafeProxy() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            executor.submit(this::acceptLoop);
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(10000);
                    executor.submit(() -> handleConnection(client));
                } catch (Exception e) {
                    if (running) log.trace("Proxy accept error", e);
                }
            }
        }

        private void handleConnection(Socket client) {
            try (client) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String requestLine = reader.readLine();
                if (requestLine == null) return;

                String[] parts = requestLine.split(" ");
                if (parts.length < 3 || !parts[0].equals("CONNECT")) {
                    client.getOutputStream().write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
                    return;
                }

                String hostPort = parts[1];
                String[] hostPortParts = hostPort.split(":");
                String host = hostPortParts[0];
                int port = hostPortParts.length > 1 ? Integer.parseInt(hostPortParts[1]) : 443;

                // Security check
                InetAddress[] addresses = InetAddress.getAllByName(host);
                InetAddress safeAddress = null;
                for (InetAddress addr : addresses) {
                    if (!isSafeIp(addr)) {
                        log.warn("Blocked SSRF attempt to private IP: {}", addr);
                        client.getOutputStream().write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
                        return;
                    }
                    if (safeAddress == null) {
                        safeAddress = addr;
                    }
                }

                if (safeAddress == null) {
                    client.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                    return;
                }

                // Connect to remote
                try (Socket remote = new Socket()) {
                    remote.connect(new InetSocketAddress(safeAddress, port), 5000);
                    remote.setSoTimeout(10000);
                    
                    client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                    client.getOutputStream().flush();

                    // Pipe data
                    Thread t1 = new Thread(() -> {
                        try { pipe(client.getInputStream(), remote.getOutputStream(), client); } catch (Exception ignored) {}
                    });
                    Thread t2 = new Thread(() -> {
                        try { pipe(remote.getInputStream(), client.getOutputStream(), remote); } catch (Exception ignored) {}
                    });
                    t1.start();
                    t2.start();
                    t1.join();
                    t2.join();
                }
            } catch (Exception e) {
                log.trace("Proxy connection error", e);
            }
        }

        private void pipe(InputStream in, OutputStream out, Socket toClose) {
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (Exception ignored) {
            } finally {
                try { toClose.close(); } catch (Exception ignored) {}
            }
        }

        public static boolean isSafeIp(InetAddress addr) {
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isAnyLocalAddress() || 
                addr.isLinkLocalAddress() || addr.isMulticastAddress()) {
                return false;
            }
            byte[] address = addr.getAddress();
            if (address.length == 4) { // IPv4
                int b0 = address[0] & 0xFF;
                int b1 = address[1] & 0xFF;
                if (b0 == 0) return false; // 0.0.0.0/8
                if (b0 == 10) return false; // 10.0.0.0/8 (covered by isSiteLocal, but explicit is better)
                if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return false; // 100.64.0.0/10 CGNAT
                if (b0 == 127) return false; // 127.0.0.0/8
                if (b0 == 169 && b1 == 254) return false; // 169.254.0.0/16 Link local
                if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return false; // 172.16.0.0/12
                if (b0 == 192 && b1 == 168) return false; // 192.168.0.0/16
                if (b0 == 198 && (b1 == 18 || b1 == 19)) return false; // 198.18.0.0/15
                if (b0 == 255) return false; // 255.255.255.255
            }
            return true;
        }

        @Override
        public void close() {
            running = false;
            try { serverSocket.close(); } catch (Exception ignored) {}
            executor.shutdownNow();
        }
    }
}
