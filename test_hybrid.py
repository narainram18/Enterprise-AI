import requests
import time

BASE_URL = 'http://localhost:8080/api'

def main():
    email = "narainram123456789@gmail.com"
    pwd = "2005Narain@"
    
    # Login
    print("Logging in...")
    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": pwd})
    if res.status_code != 200:
        print(f"Login failed: {res.text}")
        return
    token = res.json()["data"]["token"]
    headers = {"Authorization": f"Bearer {token}"}
    
    # Create conversation
    print("Creating conversation...")
    res = requests.post(f"{BASE_URL}/conversations", headers=headers, json={"title": "Hybrid Search Test"})
    conversation_id = res.json()["data"]["id"]
    
    queries = [
        "Who is the CEO?",
        "Rahul Krishnan",
        "Spring Boot",
        "Leave Policy",
        "Working Hours"
    ]
    
    for q in queries:
        print(f"\n--- Querying: {q} ---")
        chat_url = f"{BASE_URL}/conversations/{conversation_id}/messages"
        payload = {"content": q}
        t0 = time.time()
        res = requests.post(chat_url, headers=headers, json=payload)
        t1 = time.time()
        
        if res.status_code == 200:
            data = res.json()["data"]
            stats = data.get("retrievalStatistics", {})
            print(f"Response (Lat: {t1-t0:.2f}s): {data['assistantMessage']['content'][:100]}...")
            print(f"Stats: {stats}")
        else:
            print(f"Failed: {res.text}")

if __name__ == "__main__":
    main()
