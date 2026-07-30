import requests
import json

def get_embedding(text):
    # Get embedding from Ollama
    url = "http://localhost:11434/api/embeddings"
    payload = {
        "model": "qwen3-embedding:0.6b",
        "prompt": text
    }
    response = requests.post(url, json=payload)
    if response.status_code == 200:
        return response.json().get("embedding")
    else:
        print(f"Error getting embedding: {response.text}")
        return None

def search_qdrant(embedding, top_k=10):
    # Search Qdrant
    url = "http://localhost:6333/collections/document_chunks/points/search"
    payload = {
        "vector": embedding,
        "limit": top_k,
        "with_payload": True
    }
    response = requests.post(url, json=payload)
    if response.status_code == 200:
        return response.json().get("result", [])
    else:
        print(f"Error searching Qdrant: {response.text}")
        return []

def main():
    query = "Who is the CEO?"
    print(f"Query: {query}")
    print("-" * 50)
    
    embedding = get_embedding(query)
    if not embedding:
        return
        
    print(f"Successfully generated embedding (length: {len(embedding)})")
    
    results = search_qdrant(embedding)
    
    print(f"\nRaw Qdrant search results BEFORE filtering (count: {len(results)}):")
    for idx, res in enumerate(results):
        score = res.get("score")
        payload = res.get("payload", {})
        chunk_id = payload.get("chunkId", "UNKNOWN")
        owner_id = payload.get("ownerId", "UNKNOWN")
        doc_id = payload.get("documentId", "UNKNOWN")
        chunk_text = payload.get("chunkText", "UNKNOWN")
        
        print(f"\nResult {idx+1}:")
        print(f"Chunk ID: {chunk_id}")
        print(f"Score: {score}")
        print(f"OwnerId: {owner_id}")
        print(f"DocumentId: {doc_id}")
        # print first 50 chars of text to identify
        print(f"Text Preview: {chunk_text[:100].replace(chr(10), ' ')}")

    # Also print retrieval properties configured in Java
    print("\nRetrievalProperties (from application.properties):")
    print("minimumSimilarityScore: 0.5")
    print("topK: 10")

if __name__ == "__main__":
    main()
