import requests
import json

base_url = "http://localhost:8080/api/conversations"
auth = ("user@example.com", "password")

response = requests.post(base_url, auth=auth)
conversation_id = response.json()["data"]["id"]

print("Testing chat API (Non-Streaming)...")
chat_url = f"{base_url}/{conversation_id}/messages"
payload = {"content": "Who is the CEO?"}

response = requests.post(chat_url, auth=auth, json=payload)
print(response.json())
