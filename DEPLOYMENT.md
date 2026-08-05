# Enterprise AI Workspace - Deployment Guide

This document outlines how to deploy the Enterprise AI Workspace in a production-like environment using Docker Compose.

## Architecture

The system consists of the following components:
1. **Frontend**: Vite + React SPA, served via Nginx.
2. **Backend**: Spring Boot 3 Java application.
3. **Database**: PostgreSQL (relational data).
4. **Vector Store**: Qdrant (embeddings and semantic search).
5. **LLM Engine**: Ollama (local model inference).

## Prerequisites
- Docker
- Docker Compose
- (Optional) GPU support configured in Docker for Ollama acceleration.

## Quick Start

1. **Clone the repository** (or navigate to the project root).
2. **Set Environment Variables**: 
   Create a `.env` file in the root directory (or let Docker Compose use the defaults).
   ```properties
   POSTGRES_USER=admin
   POSTGRES_PASSWORD=secret
   POSTGRES_DB=enterprise_ai
   JWT_SECRET=super_secret_jwt_key_that_is_long_enough
   ```

3. **Start the Stack**:
   ```bash
   docker-compose up -d --build
   ```

4. **Access the Application**:
   - Frontend: `http://localhost:8080`
   - Backend API: `http://localhost:8081/api`
   - Actuator Health: `http://localhost:8081/actuator/health`
   - Prometheus Metrics: `http://localhost:8081/actuator/prometheus`

## Monitoring and Observability

The backend is fully instrumented with Micrometer.
- **Health Checks**: `GET /actuator/health` provides detailed status of Qdrant, Ollama, Database, and Document Storage.
- **Metrics**: `GET /actuator/prometheus` exports metrics for Prometheus scraping.
  - Key custom metrics include:
    - `document.upload.latency`
    - `document.embedding.latency`
    - `chat.llm.latency`
    - `chat.tool.latency`
    - `chat.sse.duration`
    - `search.embedding.latency`
    - `search.vector.latency`

## Logging
When running with the `prod` Spring profile (which is the default in the Dockerfile), the backend outputs structured JSON logs via Logstash encoder to `logs/backend.log.%d{yyyy-MM-dd}.%i.json.gz`. These logs include MDC contexts such as `traceId` and `userEmail` for request tracing.
