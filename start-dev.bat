@echo off
echo Starting local infrastructure...
docker-compose up -d

echo.
echo Waiting for Qdrant to be ready...
:QdrantLoop
curl -s http://localhost:6333/collections >nul
if %errorlevel% neq 0 (
    ping 127.0.0.1 -n 3 >nul
    goto QdrantLoop
)
echo Qdrant is READY!

echo.
echo Checking Ollama...
curl -s http://localhost:11434/api/tags >nul
if %errorlevel% neq 0 (
    echo [WARNING] Ollama is not running or unreachable at http://localhost:11434
    echo Please start Ollama manually on your Windows machine.
) else (
    echo Ollama is READY!
)

echo.
echo Infrastructure startup complete! You may now start the backend and frontend.
