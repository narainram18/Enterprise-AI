import requests
import time

BASE_URL = 'http://localhost:8080/api'

def main():
    email = "cachetest@example.com"
    pwd = "Password123!"
    requests.post(f"{BASE_URL}/auth/register", json={"email": email, "password": pwd, "name": "Cache Tester"})
    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": pwd})
    token = res.json()["data"]["token"]
    headers = {"Authorization": f"Bearer {token}"}
    
    print("--- 1. Creating conversation ---")
    res = requests.post(f"{BASE_URL}/conversations", headers=headers, json={"title": "Cache Test"})
    conversation_id = res.json()["data"]["id"]
    chat_url = f"{BASE_URL}/conversations/{conversation_id}/messages"
    
    print("--- 2. Asking question BEFORE upload ---")
    payload = {"content": "Who is the CEO?"}
    res = requests.post(chat_url, headers=headers, json=payload)
    print("Response 1:", res.json()["data"]["assistantMessage"]["content"])
    
    print("--- 3. Uploading document (should EVICT cache) ---")
    with open("handbook2.txt", "w") as f:
        f.write("CEO: John Smith\nAI Team Manager: Rahul Krishnan\nTechnologies: Spring Boot, PostgreSQL, React, Ollama, Qdrant, LangChain")
    
    with open("handbook2.txt", "rb") as f:
        files = {"file": ("handbook2.txt", f, "text/plain")}
        res = requests.post(f"{BASE_URL}/documents", headers=headers, files=files)
    doc_id = res.json()["data"]["id"]
    
    for i in range(15):
        time.sleep(2)
        status = requests.get(f"{BASE_URL}/documents/{doc_id}", headers=headers).json()["data"]["processingStatus"]
        if status in ("READY", "FAILED"): break
    print(f"Document status: {status}")
    
    print("--- 4. Asking question AFTER upload (should MISS cache and return context) ---")
    res = requests.post(chat_url, headers=headers, json=payload)
    print("Response 2:", res.json()["data"]["assistantMessage"]["content"])
    
    print("--- 5. Asking question AGAIN (should HIT cache and return context) ---")
    res = requests.post(chat_url, headers=headers, json=payload)
    print("Response 3:", res.json()["data"]["assistantMessage"]["content"])

if __name__ == "__main__":
    main()
