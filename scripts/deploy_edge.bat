@echo off
REM ============================================================
REM NagarSetu V2.0 — Windows Edge Deployment Script
REM Phase 15: Air-gapped / local network startup
REM Usage: scripts\deploy_edge.bat [--pull] [--clean]
REM ============================================================
setlocal enabledelayedexpansion

set COMPOSE_FILE=%~dp0..\docker-compose.yml
set PROJECT_NAME=nagarsetu

echo.
echo  ================================================
echo   NagarSetu V2.0 ^| Edge Deployment
echo   Subnet: 172.30.0.0/24
echo  ================================================
echo.

REM ---- Parse arguments -------------------------------------
set PULL=0
set CLEAN=0
for %%A in (%*) do (
    if "%%A"=="--pull"  set PULL=1
    if "%%A"=="--clean" set CLEAN=1
)

REM ---- Check Docker is running -----------------------------
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Desktop is not running. Please start Docker and retry.
    exit /b 1
)

REM ---- Optional: clean old containers and volumes ----------
if "%CLEAN%"=="1" (
    echo [CLEAN] Removing previous containers and volumes...
    docker compose -f "%COMPOSE_FILE%" -p %PROJECT_NAME% down -v --remove-orphans
    echo [CLEAN] Done.
)

REM ---- Optional: pull latest images -----------------------
if "%PULL%"=="1" (
    echo [PULL] Pulling base images...
    docker compose -f "%COMPOSE_FILE%" -p %PROJECT_NAME% pull
)

REM ---- Build and start all services -----------------------
echo [BUILD] Building service images...
docker compose -f "%COMPOSE_FILE%" -p %PROJECT_NAME% build --parallel

echo [START] Starting all 5 services...
docker compose -f "%COMPOSE_FILE%" -p %PROJECT_NAME% up -d

REM ---- Health check loop ----------------------------------
echo [HEALTH] Waiting for services to become healthy...
set MAX_WAIT=120
set ELAPSED=0

:healthloop
timeout /t 5 /nobreak >nul
set /a ELAPSED+=5

docker compose -f "%COMPOSE_FILE%" -p %PROJECT_NAME% ps --format "table {{.Name}}\t{{.Status}}" 2>nul | findstr /i "unhealthy\|starting" >nul
if errorlevel 1 (
    echo [OK] All services are healthy after %ELAPSED%s.
    goto done
)

if %ELAPSED% geq %MAX_WAIT% (
    echo [WARN] Health check timed out after %MAX_WAIT%s. Check logs:
    echo        docker compose -p %PROJECT_NAME% logs --tail=50
    goto done
)

echo [WAIT] %ELAPSED%s elapsed — services still initializing...
goto healthloop

:done
echo.
echo  ================================================
echo   NagarSetu V2.0 is UP
echo   Gateway  : http://localhost:80
echo   Frontend : http://localhost:3000  (direct)
echo   Backend  : http://localhost:4000  (direct)
echo   AI Engine: http://localhost:8000/health
echo   Database : localhost:5432 (nagarsetu)
echo  ================================================
echo.
echo  To stop: docker compose -p %PROJECT_NAME% down
echo  Logs:    docker compose -p %PROJECT_NAME% logs -f
echo.
endlocal
