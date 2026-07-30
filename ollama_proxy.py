from http.server import HTTPServer, BaseHTTPRequestHandler
import urllib.request
import json
import logging

logging.basicConfig(level=logging.INFO, format='%(message)s')

class Proxy(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        req = urllib.request.Request("http://localhost:11434" + self.path, data=post_data, headers=dict(self.headers), method="POST")
        try:
            with urllib.request.urlopen(req) as response:
                body = response.read()
                
                # Check if it's the chat endpoint to log it
                if "/api/chat" in self.path or "/api/generate" in self.path:
                    logging.info("=== PROXY INTERCEPTED REQUEST TO OLLAMA ===")
                    try:
                        req_json = json.loads(post_data.decode('utf-8'))
                        logging.info("Model: %s", req_json.get("model"))
                        messages = req_json.get("messages", [])
                        logging.info("Messages Array: %s", json.dumps(messages, indent=2))
                        
                        total_chars = sum(len(m.get("content", "")) for m in messages)
                        logging.info("Total Characters in Messages: %d", total_chars)
                        
                    except Exception as e:
                        logging.info("Could not parse request JSON: %s", e)
                        
                    logging.info("=== OLLAMA RAW RESPONSE ===")
                    logging.info(body.decode('utf-8'))
                    logging.info("===========================================")
                
                self.send_response(response.status)
                for k, v in response.headers.items():
                    self.send_header(k, v)
                self.end_headers()
                self.wfile.write(body)
        except Exception as e:
            self.send_response(500)
            self.end_headers()
            self.wfile.write(str(e).encode('utf-8'))

if __name__ == '__main__':
    server = HTTPServer(('localhost', 11435), Proxy)
    print("Proxy started on port 11435")
    server.serve_forever()
