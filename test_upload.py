import requests
import json
import time
import os

BASE_URL = 'http://localhost:8080/api'

def register_and_login():
    email = "embedtest@example.com"
    pwd = "Password123!"
    requests.post(f"{BASE_URL}/auth/register", json={"email": email, "password": pwd, "name": "Embed Tester"})
    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": pwd})
    data = res.json()
    if not data.get("success"):
        print(f"Login failed: {data}")
        return None, None
    token = data["data"]["token"]
    return token, {"Authorization": f"Bearer {token}"}

def upload_and_check(headers, filepath, content_type, label):
    print(f"\n--- Uploading {label}: {filepath} ---")
    with open(filepath, "rb") as f:
        files = {"file": (os.path.basename(filepath), f, content_type)}
        res = requests.post(f"{BASE_URL}/documents", headers=headers, files=files)
    
    data = res.json()
    if not data.get("success"):
        print(f"  Upload failed: {data}")
        return
    
    doc_id = data["data"]["id"]
    print(f"  Document ID: {doc_id}")
    
    # Poll for status
    for i in range(15):
        time.sleep(2)
        res = requests.get(f"{BASE_URL}/documents/{doc_id}", headers=headers)
        doc = res.json()["data"]
        status = doc["processingStatus"]
        print(f"  Poll {i+1}: status={status}")
        if status in ("READY", "FAILED"):
            break
    
    if status == "READY":
        print(f"  [SUCCESS] {label} SUCCESS - Document is READY")
    else:
        error = doc.get("extractionError", "unknown")
        print(f"  [FAILED] {label} FAILED - Error: {error}")
    
    return status

def main():
    print("=== Document Embedding Verification ===\n")
    
    # Check Qdrant
    try:
        r = requests.get("http://localhost:6333", timeout=3)
        print(f"Qdrant REST: {r.status_code} [OK]")
    except Exception as e:
        print(f"Qdrant REST: UNREACHABLE [ERROR] - {e}")
        return
    
    # Check Ollama embedding
    try:
        r = requests.post("http://localhost:11434/api/embed", json={"model": "qwen3-embedding:0.6b", "input": ["test"]}, timeout=10)
        print(f"Ollama Embed: {r.status_code} [OK]")
    except Exception as e:
        print(f"Ollama Embed: UNREACHABLE [ERROR] - {e}")
        return
    
    token, headers = register_and_login()
    if not headers:
        return
    
    # Create test files
    with open("test.txt", "w") as f:
        f.write("Artificial intelligence is transforming how businesses operate. "
                "Machine learning models can analyze vast datasets to find patterns. "
                "Natural language processing enables computers to understand human language. "
                "This document tests the complete embedding pipeline from upload to vector storage.")
    
    results = {}
    
    # Test TXT
    results["TXT"] = upload_and_check(headers, "test.txt", "text/plain", "TXT")
    
    # Cleanup
    os.remove("test.txt")
    
    print("\n=== Summary ===")
    for fmt, status in results.items():
        icon = "[OK]" if status == "READY" else "[ERROR]"
        print(f"  {fmt}: {status} {icon}")

if __name__ == "__main__":
    main()
