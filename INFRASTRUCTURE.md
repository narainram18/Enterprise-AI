# Enterprise AI - Local Infrastructure Guide

This guide explains how to manage the local development infrastructure (Qdrant and Ollama) required for the Enterprise AI application.

## Prerequisites
- **Docker Desktop** installed and running on Windows.
- **Ollama** installed locally on Windows (not via Docker, for optimal GPU utilization).

## First-Time Setup
On your first run, you do not need to perform any complex setup. Simply run the startup script, and it will pull the necessary Docker images and create persistent storage directories.

1. Open your terminal in the `Enterprise-AI-Workspace` directory.
2. Run `start-dev.bat`.

## Normal Startup
Whenever you want to start developing or after restarting your computer:
1. Ensure Docker Desktop is running (you can set it to auto-start with Windows).
2. Run the `start-dev.bat` script.
3. Start the Spring Boot backend (`mvn spring-boot:run` or via your IDE).
4. Start the React frontend.

> [!NOTE]
> The `start-dev.bat` script does NOT start the backend or frontend automatically. It only manages the background infrastructure.

## Normal Shutdown
When you are done for the day or want to free up resources:
1. Stop your backend and frontend.
2. Run `stop-dev.bat` to safely spin down the Docker containers.

## Restart After Windows Reboot
Since Qdrant is configured in `docker-compose.yml` with `restart: unless-stopped`, it will automatically attempt to start when Docker Desktop starts.
However, to be perfectly safe and ensure both Qdrant and Ollama are ready, it is a good practice to run `start-dev.bat` after a reboot.

## How to Verify Infrastructure Health

You can verify the health of all required components using the provided `status-dev.bat` script, or run these commands manually:

### 1. Verify Docker & Qdrant
- **Command**: `docker ps --filter "name=enterprise-qdrant"`
- **Command**: `curl http://localhost:6333/collections`
- **Expected**: A JSON response indicating your collections (e.g., `document_chunks`).

### 2. Verify Ollama
- **Command**: `curl http://localhost:11434/api/tags`
- **Expected**: A JSON list of installed models on your local machine.

## Data Persistence
Qdrant vectors and collections are stored in `./.docker/qdrant_data`.
This directory is mapped as a Docker Volume. Do not delete this directory unless you intentionally want to wipe all ingested documents and vectors.

## Configuration Matching
The infrastructure uses the following ports and URLs, which perfectly match the `application.properties`:
- Qdrant REST: `http://localhost:6333`
- Qdrant gRPC: `localhost:6334`
- Qdrant Collection: `document_chunks`
- Ollama Base URL: `http://localhost:11434`
