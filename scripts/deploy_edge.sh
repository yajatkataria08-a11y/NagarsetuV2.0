#!/usr/bin/env bash
# ============================================================
# NagarSetu V2.0 — Linux / macOS Edge Deployment Script
# Phase 15: Air-gapped / local network startup
# Usage: bash scripts/deploy_edge.sh [--pull] [--clean]
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/../docker-compose.yml"
PROJECT_NAME="nagarsetu"
MAX_WAIT=120

# ANSI colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; NC='\033[0m'; BOLD='\033[1m'

banner() {
  echo -e "\n${CYAN}${BOLD}"
  echo "  ================================================"
  echo "   NagarSetu V2.0 | Edge Deployment"
  echo "   Subnet: 172.30.0.0/24"
  echo "  ================================================"
  echo -e "${NC}"
}

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ---- Parse arguments ----------------------------------------
PULL=false
CLEAN=false
for arg in "$@"; do
  case $arg in
    --pull)  PULL=true ;;
    --clean) CLEAN=true ;;
    *) log_warn "Unknown argument: $arg" ;;
  esac
done

banner

# ---- Check Docker -------------------------------------------
if ! docker info >/dev/null 2>&1; then
  log_error "Docker is not running. Start Docker and retry."
  exit 1
fi
log_info "Docker is running."

# ---- Create certs directory if missing ----------------------
mkdir -p "$SCRIPT_DIR/../certs"
if [[ ! -f "$SCRIPT_DIR/../certs/nagarsetu.crt" ]]; then
  log_warn "No TLS certs found. Generating self-signed certificate for development..."
  openssl req -x509 -newkey rsa:4096 -keyout "$SCRIPT_DIR/../certs/nagarsetu.key" \
    -out "$SCRIPT_DIR/../certs/nagarsetu.crt" -sha256 -days 365 -nodes \
    -subj "/C=IN/ST=MP/L=Bhopal/O=NagarSetu/CN=nagarsetu.local" 2>/dev/null
  log_info "Self-signed cert generated at certs/nagarsetu.crt"
fi

# ---- Optional: clean ----------------------------------------
if $CLEAN; then
  log_info "Removing previous containers and volumes..."
  docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v --remove-orphans || true
  log_info "Clean complete."
fi

# ---- Optional: pull -----------------------------------------
if $PULL; then
  log_info "Pulling base images..."
  docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" pull
fi

# ---- Build --------------------------------------------------
log_info "Building service images (parallel)..."
docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build --parallel

# ---- Start --------------------------------------------------
log_info "Starting all 5 services..."
docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d

# ---- Health check loop --------------------------------------
log_info "Waiting for services to become healthy (max ${MAX_WAIT}s)..."
elapsed=0
while true; do
  sleep 5
  elapsed=$((elapsed + 5))

  # Count unhealthy / starting containers
  unhealthy=$(docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps --format json 2>/dev/null \
    | python3 -c "import sys,json; data=sys.stdin.read();
rows=[json.loads(l) for l in data.splitlines() if l.strip()];
print(sum(1 for r in rows if r.get('Health','') in ('unhealthy','starting','')))" 2>/dev/null || echo "0")

  if [[ "$unhealthy" -eq 0 ]]; then
    log_info "All services healthy after ${elapsed}s."
    break
  fi

  if [[ $elapsed -ge $MAX_WAIT ]]; then
    log_warn "Health check timed out after ${MAX_WAIT}s."
    log_warn "Check logs: docker compose -p $PROJECT_NAME logs --tail=50"
    break
  fi

  log_info "${elapsed}s elapsed — ${unhealthy} service(s) still initializing..."
done

# ---- Done ---------------------------------------------------
echo -e "\n${GREEN}${BOLD}"
echo "  ================================================"
echo "   NagarSetu V2.0 is UP"
echo "   Gateway  : https://localhost:443"
echo "   Frontend : http://localhost:3000  (direct)"
echo "   Backend  : http://localhost:4000  (direct)"
echo "   AI Engine: http://localhost:8000/health"
echo "   Database : localhost:5432 (nagarsetu)"
echo "  ================================================"
echo -e "${NC}"
echo "  Stop:  docker compose -p $PROJECT_NAME down"
echo "  Logs:  docker compose -p $PROJECT_NAME logs -f"
echo ""
