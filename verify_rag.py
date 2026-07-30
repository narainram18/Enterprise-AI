import psycopg2
import json
import requests
import traceback

print("--- STEP 1: Verify Database ---")
try:
    conn = psycopg2.connect("dbname=enterprise_ai user=postgres password=2005 host=localhost")
    cur = conn.cursor()
    cur.execute("SELECT id, owner_id, original_filename, processing_status FROM document LIMIT 1;")
    doc = cur.fetchone()
    if doc:
        print(f"Document ID: {doc[0]}")
        print(f"Owner ID: {doc[1]}")
        print(f"Filename: {doc[2]}")
        print(f"Status: {doc[3]}")
        
        cur.execute("SELECT id, content FROM document_chunk WHERE document_id = %s;", (doc[0],))
        chunks = cur.fetchall()
        print(f"Chunk count: {len(chunks)}")
        print("Chunk IDs: ", [c[0] for c in chunks])
        for i, c in enumerate(chunks[:3]):
            print(f"Chunk {i+1} text: {c[1]}")
    else:
        print("No documents found in DB.")
except Exception as e:
    print(f"DB Error: {e}")

print("\n--- STEP 2: Verify Embeddings (via Qdrant) ---")
try:
    res = requests.get("http://localhost:6333/collections/document_chunks")
    print(f"Qdrant collection stats: {res.json()}")
    
    # Get points
    res = requests.post("http://localhost:6333/collections/document_chunks/points/scroll", json={"limit": 5, "with_vector": True, "with_payload": True})
    points = res.json().get("result", {}).get("points", [])
    for p in points:
        print(f"Chunk ID: {p.get('id')}")
        vector = p.get('vector')
        print(f"Embedding exists? {vector is not None}")
        if vector:
            print(f"Embedding dimension: {len(vector)}")
except Exception as e:
    print(f"Qdrant Error: {e}")

print("\n--- STEP 3: Verify Qdrant (Manual Search) ---")
try:
    # First get embedding for 'Who is the CEO?'
    emb_res = requests.post("http://localhost:11434/api/embeddings", json={
        "model": "qwen3-embedding:0.6b",
        "prompt": "Who is the CEO?"
    })
    vector = emb_res.json().get("embedding")
    if vector:
        print(f"Query vector generated, len={len(vector)}")
        search_res = requests.post("http://localhost:6333/collections/document_chunks/points/search", json={
            "vector": vector,
            "limit": 3,
            "with_payload": True
        })
        print("Raw Qdrant response:")
        print(json.dumps(search_res.json(), indent=2))
    else:
        print("Failed to get embedding from Ollama")
except Exception as e:
    print(f"Qdrant Search Error: {e}")
