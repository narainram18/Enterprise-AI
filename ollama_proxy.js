const http = require('http');

const PORT = 11435;
const TARGET_HOST = 'localhost';
const TARGET_PORT = 11434;

const server = http.createServer((clientReq, clientRes) => {
    let body = '';
    
    // Determine if it's the chat endpoint
    const isChat = clientReq.url.includes('/api/chat') || clientReq.url.includes('/api/generate');

    if (isChat && clientReq.method === 'POST') {
        clientReq.on('data', chunk => {
            body += chunk.toString();
        });
        clientReq.on('end', () => {
            console.log("\n=== PROXY INTERCEPTED REQUEST TO OLLAMA ===");
            try {
                const reqJson = JSON.parse(body);
                console.log("Model: " + reqJson.model);
                console.log("Messages Array: " + JSON.stringify(reqJson.messages, null, 2));
                let totalChars = 0;
                if (reqJson.messages) {
                    reqJson.messages.forEach(m => {
                        totalChars += (m.content || "").length;
                    });
                }
                console.log("Total Characters in Messages: " + totalChars);
            } catch (e) {
                console.log("Could not parse request JSON: " + e.message);
            }
            
            // Forward the collected body to Ollama
            const options = {
                hostname: TARGET_HOST,
                port: TARGET_PORT,
                path: clientReq.url,
                method: clientReq.method,
                headers: clientReq.headers
            };
            
            // Adjust headers for our collected body
            options.headers['content-length'] = Buffer.byteLength(body);
            delete options.headers['transfer-encoding'];

            const proxyReq = http.request(options, proxyRes => {
                let responseBody = '';
                
                // Write headers to client
                clientRes.writeHead(proxyRes.statusCode, proxyRes.headers);
                
                proxyRes.on('data', chunk => {
                    responseBody += chunk.toString();
                    clientRes.write(chunk); // stream to client
                });
                
                proxyRes.on('end', () => {
                    console.log("\n=== OLLAMA RAW RESPONSE ===");
                    console.log(responseBody);
                    console.log("===========================================\n");
                    clientRes.end();
                });
            });
            
            proxyReq.on('error', err => {
                console.error("Proxy request error: ", err);
                clientRes.writeHead(500);
                clientRes.end();
            });
            
            proxyReq.write(body);
            proxyReq.end();
        });
    } else {
        // Just forward everything else directly without body parsing
        const options = {
            hostname: TARGET_HOST,
            port: TARGET_PORT,
            path: clientReq.url,
            method: clientReq.method,
            headers: clientReq.headers
        };
        const proxyReq = http.request(options, proxyRes => {
            clientRes.writeHead(proxyRes.statusCode, proxyRes.headers);
            proxyRes.pipe(clientRes, { end: true });
        });
        clientReq.pipe(proxyReq, { end: true });
        
        proxyReq.on('error', err => {
            console.error("Proxy direct request error: ", err);
            clientRes.writeHead(500);
            clientRes.end();
        });
    }
});

server.listen(PORT, () => {
    console.log(`Proxy listening on port ${PORT}`);
});
