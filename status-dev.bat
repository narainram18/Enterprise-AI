@echo off
echo === Docker Containers ===
docker ps --filter "name=enterprise-qdrant"

echo.
echo === Qdrant Status ===
curl -s http://localhost:6333/collections >nul
if %errorlevel% neq 0 (
    echo [ERROR] Qdrant is unreachable.
) else (
    echo [OK] Qdrant is running.
)

echo.
echo === Ollama Status ===
curl -s http://localhost:11434/api/tags >nul
if %errorlevel% neq 0 (
    echo [ERROR] Ollama is unreachable.
) else (
    echo [OK] Ollama is running.
)
echo.
