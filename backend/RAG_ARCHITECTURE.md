# Enterprise AI - RAG Architecture

## Overview
This document outlines the architecture for the Retrieval-Augmented Generation (RAG) implementation in the Enterprise AI application.

## Core Components
1. **AiChatProperties & RetrievalProperties**: Contains the token budgets and retrieval hyperparameters.
2. **TokenBudgetManager**: Responsible for truncating conversation history based on priority (System Prompt > Current Question > Retrieved Knowledge > History).
3. **RetrievalContextBuilder**: Groups retrieved chunks by document, merges contiguous chunks, removes duplicated chunks, and formats the result with precise citations (`--- Section X ---`).
4. **RagPromptBuilder**: Injects behavior guidelines and retrieved knowledge directly into the `USER` message to leverage the LLM's recency bias, resolving issues where history would override distant system prompts.
5. **SemanticSearchService**: Uses `QueryEmbeddingService` to encode the query and fetches documents from Qdrant. Results are filtered by owner and strictly sorted by similarity score.
6. **ChatRetrievalService**: Orchestrates the vector search and builds the final context string. Results are cached (`@Cacheable`) for identical queries.

## Prompt Structure
- **SYSTEM**: Provider-neutral instructions on grounding and strict adherence to context.
- **USER**:
  - Conversation History (truncated)
  - Retrieved Knowledge (if available)
  - Current Question

## Caching & Optimization
- Application caching is enabled (`@EnableCaching`).
- User embeddings (`QueryEmbeddingService`) and retrieval results (`ChatRetrievalService`) are cached to prevent redundant vector DB calls on identical back-to-back queries.
