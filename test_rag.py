import requests
import time
import os

BASE_URL = 'http://localhost:8080/api'

def main():
    email = "ragtest@example.com"
    pwd = "Password123!"
    requests.post(f"{BASE_URL}/auth/register", json={"email": email, "password": pwd, "name": "RAG Tester"})
    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": pwd})
    token = res.json()["data"]["token"]
    headers = {"Authorization": f"Bearer {token}"}
    
    with open("handbook.txt", "w") as f:
        f.write("CEO: John Smith\nAI Team Manager: Rahul Krishnan\nTechnologies: Spring Boot, PostgreSQL, React, Ollama, Qdrant, LangChain")
    
    with open("handbook.txt", "rb") as f:
        files = {"file": ("handbook.txt", f, "text/plain")}
        res = requests.post(f"{BASE_URL}/documents", headers=headers, files=files)
    doc_id = res.json()["data"]["id"]
    
    for i in range(15):
        time.sleep(2)
        status = requests.get(f"{BASE_URL}/documents/{doc_id}", headers=headers).json()["data"]["processingStatus"]
        if status in ("READY", "FAILED"): break
    
    print(f"Document status: {status}")
    
    res = requests.post(f"{BASE_URL}/conversations", headers=headers, json={"title": "RAG Test"})
    conversation_id = res.json()["data"]["id"]
    
    chat_url = f"{BASE_URL}/conversations/{conversation_id}/messages"
    payload = {"content": "Who is the CEO?"}
    res = requests.post(chat_url, headers=headers, json=payload)
    print("Chat response:")
    print(res.json()["data"]["assistantMessage"]["content"])

if __name__ == "__main__":
    main()
