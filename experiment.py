import requests
import json
import time
import sys

OLLAMA_URL = "http://localhost:11434/api/chat"
MODEL = "qwen3.5:4b-q4_K_M"

variant_a_system = """You are an enterprise assistant. Use retrieved information when relevant. If it is insufficient, answer using general knowledge. Never fabricate document contents. Prefer retrieved information over assumptions and mention uncertainty when appropriate.

====================
Retrieved Knowledge
====================

[Document: handbook.txt]

Section: 0

Content:
CEO: John Smith
AI Team Manager: Rahul Krishnan
Technologies: Spring Boot, PostgreSQL, React, Ollama, Qdrant, LangChain

====================
Conversation History
====================

The conversation messages that follow are the conversation history.

====================
User Question
====================

The latest user message is the question to answer."""

variant_b_user = """Use ONLY the following retrieved knowledge to answer.

If the answer is not present, explicitly say it is not found.

Retrieved Knowledge:

[Document: handbook.txt]

Section: 0

Content:
CEO: John Smith
AI Team Manager: Rahul Krishnan
Technologies: Spring Boot, PostgreSQL, React, Ollama, Qdrant, LangChain

Question:

Who is the CEO?"""

def run_experiment(variant, messages):
    responses = []
    print(f"--- Running Variant {variant} 5 times ---", flush=True)
    for i in range(5):
        payload = {
            "model": MODEL,
            "messages": messages,
            "stream": False
        }
        try:
            res = requests.post(OLLAMA_URL, json=payload, timeout=120)
            if res.status_code == 200:
                content = res.json().get("message", {}).get("content", "").strip()
                print(f"Run {i+1}: {content}", flush=True)
                responses.append(content)
            else:
                print(f"Run {i+1}: ERROR {res.status_code} {res.text}", flush=True)
                responses.append(f"ERROR: {res.status_code}")
        except Exception as e:
            print(f"Run {i+1}: EXCEPTION {e}", flush=True)
            responses.append(f"EXCEPTION: {e}")
        time.sleep(1)
    return responses

def main():
    messages_a = [
        {"role": "system", "content": variant_a_system},
        {"role": "user", "content": "Who is the CEO?"}
    ]
    
    messages_b = [
        {"role": "system", "content": "You are an enterprise assistant."},
        {"role": "user", "content": variant_b_user}
    ]
    
    res_a = run_experiment("A (System Prompt)", messages_a)
    print("\n", flush=True)
    res_b = run_experiment("B (User Prompt)", messages_b)
    
    print("\n--- Summary ---", flush=True)
    print("Variant A:")
    for idx, r in enumerate(res_a):
        print(f"  Run {idx+1}: {r}")
        
    print("\nVariant B:")
    for idx, r in enumerate(res_b):
        print(f"  Run {idx+1}: {r}")

if __name__ == "__main__":
    main()
