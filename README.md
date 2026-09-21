# Enterprise AI Workspace

A robust, portfolio-ready full-stack Enterprise AI application that seamlessly integrates local Large Language Models (LLMs) with advanced Retrieval-Augmented Generation (RAG) pipelines, secure document intelligence, and autonomous agents.

## 🌟 Key Features

### Advanced RAG & Vector Intelligence
- **Hybrid Retrieval System:** Combines Qdrant semantic vector search with PostgreSQL Full-Text Search (FTS) using Reciprocal Rank Fusion (RRF) for optimal context accuracy.
- **Local AI Execution:** Powered by Ollama for fully local, privacy-preserving LLM generation and high-dimensional embeddings.
- **Real-time Streaming:** Leverages Server-Sent Events (SSE) to stream AI responses instantly to the client.

### Enterprise Document Management
- **RBAC & Workspace Isolation:** Strict Role-Based Access Control enforcing `PUBLIC` and `PRIVATE` document boundaries down to the vector level.
- **Version Control & Time-Travel:** Maintains historical document states, allowing seamless restoration with automatic vector and chunk garbage collection.
- **Automated Ingestion Pipeline:** Processes, chunks, and embeds documents automatically in the background upon upload.

### Autonomous Agents & Tools
- **Enterprise Research Engine:** An intelligent tool that synthesizes context simultaneously from internal company documents and external live web searches.
- **SSRF-Hardened Web Scraping:** Secured web search agents with explicitly disabled redirect tracing and strict Jsoup safety protocols to prevent Server-Side Request Forgery.

### Observability & Evaluation
- **RAG Latency Analytics:** Intercepts and logs granular metrics for every retrieval stage (Vector vs. Keyword speed, chunk counts, reranking latencies).
- **Automated Evaluation Pipeline:** `/api/evaluation/run` endpoint to programmatically assess index health and retrieval recall over predefined testing matrices.

## 🛠️ Technology Stack

**Frontend:**
- React 18, TypeScript, Vite
- Tailwind CSS (or Vanilla modern styling)
- State management and SSE streaming clients

**Backend:**
- Java 21, Spring Boot 3
- Spring Data JPA, Spring Security
- Jsoup (Web Scraping)
- Mockito & JUnit 5 (Testing)

**Infrastructure / Databases:**
- PostgreSQL (Relational & FTS)
- Qdrant (Vector Database)
- Docker & Docker Compose
- Ollama (Local AI Engine)

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- Docker and Docker Compose
- Ollama installed locally

### Quick Start
1. **Start the Infrastructure**
   ```bash
   docker-compose up -d
   ```
   *This starts PostgreSQL and Qdrant.*

2. **Run Ollama Local Models**
   Ensure your local Ollama instance has the necessary models pulled:
   ```bash
   ollama pull llama3 # or whichever model is configured
   ollama pull mxbai-embed-large
   ```

3. **Start the Backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

4. **Start the Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## 🛡️ Architecture & Security
This application is built with zero-trust architectural principles. Context pipelines require explicit `userId` tokens, and semantic vector stores dynamically compile `ConditionFactory` filters to ensure users only retrieve chunks they are authorized to see. 

## 🧪 Testing
The repository contains 160+ comprehensive JUnit 5 and Mockito tests spanning integrations, security audits, and RAG regressions.
To run the backend test suite:
```bash
cd backend
./mvnw clean test
```

## 📄 License
This project is licensed under the MIT License.
