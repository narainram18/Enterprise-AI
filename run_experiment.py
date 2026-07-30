import requests
import time
import sys

BASE_URL = 'http://localhost:8080/api'
QDRANT_URL = 'http://localhost:6333/collections/document_chunks/points/search'

def get_embedding(text):
    url = "http://localhost:11434/api/embeddings"
    res = requests.post(url, json={"model": "qwen3-embedding:0.6b", "prompt": text})
    if res.status_code == 200:
        return res.json().get("embedding")
    return None

def search_qdrant(embedding, owner_id):
    payload = {
        "vector": embedding,
        "limit": 50,
        "with_payload": True
    }
    res = requests.post(QDRANT_URL, json=payload)
    if res.status_code == 200:
        results = res.json().get("result", [])
        return [r for r in results if r.get("payload", {}).get("ownerId") == owner_id]
    return []

def main():
    email = "narainram123456789@gmail.com"
    pwd = "2005Narain@"
    
    print("--- Logging in ---")
    res = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": pwd})
    token = res.json()["data"]["token"]
    owner_id = 5
    headers = {"Authorization": f"Bearer {token}"}
    
    print("--- Uploading Handbook ---")
    with open("C:/Users/narai/Downloads/ABC_Technologies_Employee_Handbook_2026.docx", "rb") as f:
        files = {"file": ("ABC_Technologies_Employee_Handbook_2026.docx", f, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")}
        res = requests.post(f"{BASE_URL}/documents", headers=headers, files=files)
    
    doc_id = res.json()["data"]["id"]
    
    print(f"Waiting for Document {doc_id} to be READY...")
    while True:
        status = requests.get(f"{BASE_URL}/documents/{doc_id}", headers=headers).json()["data"]["processingStatus"]
        if status in ("READY", "FAILED"): break
        time.sleep(2)
        
    print(f"Document status: {status}")
    if status != "READY":
        return
        
    print("--- Analyzing Chunks in Qdrant ---")
    # To get all chunks, we can just search with a dummy query or query Qdrant scroll API
    scroll_url = "http://localhost:6333/collections/document_chunks/points/scroll"
    scroll_payload = {
        "filter": {
            "must": [
                { "key": "documentId", "match": { "value": doc_id } }
            ]
        },
        "limit": 1000,
        "with_payload": True
    }
    res = requests.post(scroll_url, json=scroll_payload)
    chunks = res.json().get("result", {}).get("points", [])
    num_chunks = len(chunks)
    
    total_len = sum(len(c.get("payload", {}).get("chunkText", "")) for c in chunks)
    avg_len = total_len / num_chunks if num_chunks > 0 else 0
    
    print(f"Number of chunks: {num_chunks}")
    print(f"Average chunk length: {avg_len:.1f}")
    
    print("\n--- Searching for 'Who is the CEO?' ---")
    query = "Who is the CEO?"
    emb = get_embedding(query)
    
    results = search_qdrant(emb, owner_id)
    # Filter only for this document
    doc_results = [r for r in results if r.get("payload", {}).get("documentId") == doc_id]
    
    print(f"Top 5 similarity scores for Document {doc_id}:")
    for i, r in enumerate(doc_results[:5]):
        score = r.get("score")
        text = r.get("payload", {}).get("chunkText", "")[:60].replace('\n', ' ')
        print(f"  {i+1}. Score: {score:.5f} | Preview: {text}...")

if __name__ == "__main__":
    main()
