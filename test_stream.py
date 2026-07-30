import requests
import json

BASE_URL = 'http://localhost:8080/api'

def main():
    email = "ragtest@example.com"
    pwd = "Password123!"
    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": pwd})
    token = res.json()["data"]["token"]
    headers = {"Authorization": f"Bearer {token}"}

    res = requests.post(f"{BASE_URL}/conversations", headers=headers, json={"title": "RAG Test Stream"})
    conversation_id = res.json()["data"]["id"]
    
    chat_url = f"{BASE_URL}/conversations/{conversation_id}/messages/stream"
    payload = {"content": "Who is the CEO?"}
    
    with requests.post(chat_url, headers=headers, json=payload, stream=True) as r:
        for line in r.iter_lines():
            if line:
                print(line.decode('utf-8'))

if __name__ == "__main__":
    main()
