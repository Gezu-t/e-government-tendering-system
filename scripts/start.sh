#!/usr/bin/env bash
# ============================================================
# E-Government Tendering System - Startup Script
# ============================================================
# Usage:
#   ./scripts/start.sh              # Start everything (infra + services)
#   ./scripts/start.sh infra        # Start only infrastructure (MySQL, Kafka, Redis)
#   ./scripts/start.sh services     # Start only Spring Boot services (assumes infra running)
#   ./scripts/start.sh frontend     # Start only the React frontend
#   ./scripts/start.sh build        # Build all services then start
#   ./scripts/start.sh stop         # Stop everything
#   ./scripts/start.sh status       # Show status of all components
#   ./scripts/start.sh db-setup     # Create databases and run migrations only
# ============================================================

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# ============================================================
# Configuration
# ============================================================
MYSQL_HOST="${DB_HOST:-localhost}"
MYSQL_PORT="${DB_PORT:-3306}"
MYSQL_USER="${DB_USERNAME:-root}"
MYSQL_PASS="${DB_PASSWORD:-password}"
KAFKA_HOST="${KAFKA_HOST:-localhost}"
KAFKA_PORT="${KAFKA_PORT:-9092}"
JWT_ISSUER_URI="${JWT_ISSUER_URI:-http://localhost:9000/auth/realms/egov-tendering}"
JWT_JWK_SET_URI="${JWT_JWK_SET_URI:-${JWT_ISSUER_URI%/}/protocol/openid-connect/certs}"

# Service definitions: name, port, db_name
declare -a SERVICES=(
  "discovery-service:8761:"
  "config-service:8888:"
  "gateway-service:8080:"
  "user-service:8081:user_service"
  "tender-service:8082:tender_service"
  "bidding-service:8083:bidding_service"
  "contract-service:8084:contract_service"
  "document-service:8085:document_service"
  "notification-service:8086:notification_service"
  "evaluation-service:8087:evaluation_service"
  "audit-service:8088:audit_service"
)

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log_info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "\n${CYAN}========================================${NC}"; echo -e "${CYAN}  $1${NC}"; echo -e "${CYAN}========================================${NC}"; }

port_pid() {
  local port=$1
  lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -1
}

# ============================================================
# Prerequisites Check
# ============================================================
check_prerequisites() {
  log_step "Checking Prerequisites"

  local missing=0

  if ! command -v java &>/dev/null; then
    log_error "Java not found. Install JDK 17+"
    missing=1
  else
    local java_ver
    java_ver=$(java -version 2>&1 | head -1 | awk -F'"' '{print $2}' | cut -d. -f1)
    if [ "$java_ver" -lt 17 ] 2>/dev/null; then
      log_error "Java 17+ required. Found: $java_ver"
      missing=1
    else
      log_ok "Java $(java -version 2>&1 | head -1 | awk -F'"' '{print $2}')"
    fi
  fi

  if ! command -v mvn &>/dev/null; then
    if [ -x "$PROJECT_ROOT/mvnw" ]; then
      log_ok "Maven Wrapper found"
      MVN="$PROJECT_ROOT/mvnw"
    else
      log_error "Maven not found"
      missing=1
    fi
  else
    log_ok "Maven $(mvn --version 2>&1 | head -1 | awk '{print $3}')"
    MVN="mvn"
  fi

  if ! command -v mysql &>/dev/null; then
    log_warn "MySQL client not found (will use alternative DB setup)"
  else
    log_ok "MySQL client available"
  fi

  if ! command -v node &>/dev/null; then
    log_warn "Node.js not found (frontend won't start)"
  else
    log_ok "Node.js $(node --version)"
  fi

  if [ $missing -eq 1 ]; then
    log_error "Missing prerequisites. Please install them and retry."
    exit 1
  fi
}

# ============================================================
# Database Setup
# ============================================================
wait_for_mysql() {
  log_info "Waiting for MySQL at $MYSQL_HOST:$MYSQL_PORT..."
  local retries=30
  while [ $retries -gt 0 ]; do
    if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "SELECT 1" &>/dev/null; then
      log_ok "MySQL is ready"
      return 0
    fi
    retries=$((retries - 1))
    sleep 2
  done
  log_error "MySQL not available after 60 seconds"
  return 1
}

setup_databases() {
  log_step "Setting Up Databases"

  if ! command -v mysql &>/dev/null; then
    log_warn "MySQL client not installed. Databases will be auto-created by Spring Boot (createDatabaseIfNotExist=true)"
    return 0
  fi

  if ! wait_for_mysql; then
    log_warn "MySQL not running. Start it first or services will create DBs on connect."
    return 0
  fi

  local databases=(
    "user_service"
    "tender_service"
    "bidding_service"
    "contract_service"
    "document_service"
    "notification_service"
    "audit_service"
    "evaluation_service"
  )

  for db in "${databases[@]}"; do
    if mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "CREATE DATABASE IF NOT EXISTS \`$db\`;" 2>/dev/null; then
      log_ok "Database '$db' ready"
    else
      log_warn "Could not create database '$db' (may already exist)"
    fi
  done

  # Grant privileges
  mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "
    GRANT ALL PRIVILEGES ON user_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON tender_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON bidding_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON contract_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON document_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON notification_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON audit_service.* TO '$MYSQL_USER'@'%';
    GRANT ALL PRIVILEGES ON evaluation_service.* TO '$MYSQL_USER'@'%';
    FLUSH PRIVILEGES;
  " 2>/dev/null && log_ok "Privileges granted" || log_warn "Could not grant privileges"
}

# ============================================================
# Build
# ============================================================
build_project() {
  log_step "Building All Services"
  log_info "This may take a few minutes on first run..."

  ${MVN:-mvn} clean package -DskipTests -q --batch-mode \
    -pl common-util,app-config-data,discovery-service,config-service,gateway-service,user-service,tender-service,bidding-service,contract-service,document-service,notification-service,evaluation-service,audit-service \
    -am 2>&1 | tail -5

  if [ $? -eq 0 ]; then
    log_ok "Build successful"
  else
    log_error "Build failed. Run 'mvn clean package -DskipTests' for details."
    exit 1
  fi
}

# ============================================================
# Start Infrastructure (MySQL, Kafka, Redis)
# ============================================================
start_infra() {
  log_step "Starting Infrastructure"

  if command -v docker &>/dev/null; then
    log_info "Starting MySQL, Kafka, Zookeeper, Redis via Docker..."
    docker compose up -d mysql redis zookeeper kafka kafka-ui 2>&1 | grep -v "^$" || true
    log_ok "Infrastructure containers started"
    log_info "Waiting for services to be healthy..."
    sleep 10
  else
    log_warn "Docker not available. Please ensure MySQL, Kafka, and Redis are running manually:"
    log_info "  MySQL:     $MYSQL_HOST:$MYSQL_PORT"
    log_info "  Kafka:     $KAFKA_HOST:$KAFKA_PORT"
    log_info "  Redis:     localhost:6379"
  fi
}

# ============================================================
# Start Spring Boot Services
# ============================================================
start_service() {
  local name=$1
  local port=$2
  local db=$3
  local pid_file="$PROJECT_ROOT/.pids/${name}.pid"
  local log_file="$PROJECT_ROOT/logs/${name}.log"

  # Check if already running
  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    log_warn "$name already running (PID: $(cat "$pid_file"))"
    return 0
  fi

  local existing_port_pid
  existing_port_pid=$(port_pid "$port" || true)
  if [ -n "$existing_port_pid" ]; then
    log_warn "$name port $port already in use (PID: $existing_port_pid); skipping duplicate start"
    echo "$existing_port_pid" > "$pid_file"
    return 0
  fi

  local jar_file
  jar_file=$(find "$PROJECT_ROOT/$name/target" -name "*.jar" -not -name "*-sources*" -not -name "*-javadoc*" 2>/dev/null | head -1)

  if [ -z "$jar_file" ]; then
    log_error "$name JAR not found. Run './scripts/start.sh build' first."
    return 1
  fi

  log_info "Starting $name on port $port..."

  local java_opts=(
    "-jar" "$jar_file"
    "--server.port=$port"
  )

  if [ "$name" = "config-service" ]; then
    java_opts+=(
      "--spring.profiles.active=native"
      "--spring.cloud.config.server.native.search-locations=file:$PROJECT_ROOT/config-repo"
    )
  else
    java_opts+=("--spring.profiles.active=dev")
  fi

  # Add DB config for services with databases
  if [ -n "$db" ]; then
    java_opts+=(
      "--spring.datasource.url=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/$db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
      "--spring.datasource.username=$MYSQL_USER"
      "--spring.datasource.password=$MYSQL_PASS"
      "--spring.flyway.baseline-on-migrate=true"
      "--spring.jpa.hibernate.ddl-auto=validate"
    )
  fi

  # Add Eureka and Kafka config
  if [ "$name" != "discovery-service" ]; then
    java_opts+=("--eureka.client.service-url.defaultZone=http://localhost:8761/eureka/")
  fi

  if [ "$name" != "discovery-service" ] && [ "$name" != "config-service" ]; then
    java_opts+=(
      "--spring.security.oauth2.resourceserver.jwt.issuer-uri=$JWT_ISSUER_URI"
      "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=$JWT_JWK_SET_URI"
    )
  fi

  if [ "$name" != "discovery-service" ] && [ "$name" != "config-service" ] && [ "$name" != "gateway-service" ] && [ "$name" != "document-service" ]; then
    java_opts+=("--spring.kafka.bootstrap-servers=$KAFKA_HOST:$KAFKA_PORT")
  fi

  # Start in background
  nohup java "${java_opts[@]}" > "$log_file" 2>&1 &
  local pid=$!
  echo "$pid" > "$pid_file"

  # Wait for startup
  local retries=60
  while [ $retries -gt 0 ]; do
    # Check if process died (bad JAR, missing class, etc.)
    if ! kill -0 "$pid" 2>/dev/null; then
      log_error "$name failed to start. Check logs: $log_file"
      tail -3 "$log_file" 2>/dev/null
      rm -f "$pid_file"
      return 1
    fi
    if curl -s "http://localhost:$port/actuator/health" &>/dev/null; then
      log_ok "$name started (PID: $pid, Port: $port)"
      return 0
    fi
    retries=$((retries - 1))
    sleep 2
  done

  log_warn "$name may still be starting (PID: $pid). Check logs: $log_file"
}

start_services() {
  log_step "Starting Spring Boot Services"

  mkdir -p "$PROJECT_ROOT/.pids" "$PROJECT_ROOT/logs"
  local failed_services=()

  # Start in dependency order with waits between tiers
  log_info "--- Tier 1: Discovery Service ---"
  start_service "discovery-service" "8761" "" || failed_services+=("discovery-service")

  log_info "--- Tier 2: Config Service ---"
  start_service "config-service" "8888" "" || failed_services+=("config-service")

  log_info "--- Tier 3: Gateway + User Service ---"
  start_service "gateway-service" "8080" "" || failed_services+=("gateway-service")
  start_service "user-service" "8081" "user_service" || failed_services+=("user-service")

  log_info "--- Tier 4: Business Services ---"
  start_service "tender-service" "8082" "tender_service" || failed_services+=("tender-service")
  start_service "bidding-service" "8083" "bidding_service" || failed_services+=("bidding-service")
  start_service "contract-service" "8084" "contract_service" || failed_services+=("contract-service")
  start_service "document-service" "8085" "document_service" || failed_services+=("document-service")
  start_service "notification-service" "8086" "notification_service" || failed_services+=("notification-service")
  start_service "evaluation-service" "8087" "evaluation_service" || failed_services+=("evaluation-service")
  start_service "audit-service" "8088" "audit_service" || failed_services+=("audit-service")

  if [ ${#failed_services[@]} -gt 0 ]; then
    log_warn "Services with startup failures: ${failed_services[*]}"
    return 1
  fi
}

# ============================================================
# Start Frontend
# ============================================================
start_frontend() {
  log_step "Starting Frontend"

  if ! command -v node &>/dev/null; then
    log_error "Node.js not installed. Cannot start frontend."
    return 1
  fi

  local pid_file="$PROJECT_ROOT/.pids/frontend.pid"
  local log_file="$PROJECT_ROOT/logs/frontend.log"

  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    log_warn "Frontend already running (PID: $(cat "$pid_file"))"
    return 0
  fi

  cd "$PROJECT_ROOT/frontend"

  if [ ! -d "node_modules" ]; then
    log_info "Installing frontend dependencies..."
    npm install --silent 2>&1 | tail -2
  fi

  log_info "Starting React dev server on port 3000..."
  nohup npm run dev > "$log_file" 2>&1 &
  echo $! > "$pid_file"
  log_ok "Frontend starting at http://localhost:3000"

  cd "$PROJECT_ROOT"
}

# ============================================================
# Stop Everything
# ============================================================
stop_all() {
  log_step "Stopping All Services"

  if [ -d "$PROJECT_ROOT/.pids" ]; then
    for pid_file in "$PROJECT_ROOT/.pids"/*.pid; do
      if [ -f "$pid_file" ]; then
        local name
        name=$(basename "$pid_file" .pid)
        local pid
        pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
          kill "$pid" 2>/dev/null
          log_ok "Stopped $name (PID: $pid)"
        else
          log_warn "$name was not running"
        fi
        rm -f "$pid_file"
      fi
    done
  fi

  # Stop Docker infra if running
  if command -v docker &>/dev/null; then
    log_info "Stopping Docker containers..."
    docker compose down 2>/dev/null || true
  fi

  log_ok "All services stopped"
}

# ============================================================
# Status Check
# ============================================================
show_status() {
  log_step "System Status"

  echo ""
  printf "%-25s %-8s %-10s %-6s\n" "SERVICE" "PORT" "STATUS" "PID"
  printf "%-25s %-8s %-10s %-6s\n" "-------------------------" "--------" "----------" "------"

  for entry in "${SERVICES[@]}"; do
    IFS=':' read -r name port db <<< "$entry"
    local pid_file="$PROJECT_ROOT/.pids/${name}.pid"
    local status="${RED}STOPPED${NC}"
    local pid="-"
    local port_listener_pid
    port_listener_pid=$(port_pid "$port" || true)

    if [ -f "$pid_file" ]; then
      pid=$(cat "$pid_file")
      if kill -0 "$pid" 2>/dev/null; then
        if curl -s "http://localhost:$port/actuator/health" &>/dev/null; then
          status="${GREEN}RUNNING${NC}"
        else
          status="${YELLOW}STARTING${NC}"
        fi
      else
        if [ -n "$port_listener_pid" ]; then
          pid="$port_listener_pid"
          if curl -s "http://localhost:$port/actuator/health" &>/dev/null; then
            status="${GREEN}RUNNING${NC}"
          else
            status="${YELLOW}LISTENING${NC}"
          fi
        else
          status="${RED}DEAD${NC}"
        fi
      fi
    elif [ -n "$port_listener_pid" ]; then
      pid="$port_listener_pid"
      if curl -s "http://localhost:$port/actuator/health" &>/dev/null; then
        status="${GREEN}RUNNING${NC}"
      else
        status="${YELLOW}LISTENING${NC}"
      fi
    fi

    printf "%-25s %-8s $(echo -e "$status")     %-6s\n" "$name" "$port" "$pid"
  done

  # Frontend
  local fe_status="${RED}STOPPED${NC}"
  local fe_pid="-"
  if [ -f "$PROJECT_ROOT/.pids/frontend.pid" ]; then
    fe_pid=$(cat "$PROJECT_ROOT/.pids/frontend.pid")
    if kill -0 "$fe_pid" 2>/dev/null; then
      fe_status="${GREEN}RUNNING${NC}"
    fi
  fi
  printf "%-25s %-8s $(echo -e "$fe_status")     %-6s\n" "frontend" "3000" "$fe_pid"

  # Docker infra
  echo ""
  if command -v docker &>/dev/null; then
    log_info "Docker containers:"
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "  (docker compose not running)"
  fi

  echo ""
  log_info "Logs directory: $PROJECT_ROOT/logs/"
  log_info "PID directory:  $PROJECT_ROOT/.pids/"
}

# ============================================================
# Print Startup Summary
# ============================================================
print_summary() {
  log_step "System Ready"

  echo ""
  echo -e "  ${GREEN}Gateway API:${NC}        http://localhost:8080"
  echo -e "  ${GREEN}Frontend:${NC}           http://localhost:3000"
  echo -e "  ${GREEN}Eureka Dashboard:${NC}   http://localhost:8761"
  echo -e "  ${GREEN}Kafka UI:${NC}           http://localhost:8090"
  echo -e "  ${GREEN}Config Server:${NC}      http://localhost:8888"
  echo ""
  echo -e "  ${BLUE}Logs:${NC}   tail -f logs/<service-name>.log"
  echo -e "  ${BLUE}Status:${NC} ./scripts/start.sh status"
  echo -e "  ${BLUE}Stop:${NC}   ./scripts/start.sh stop"
  echo ""
}

# ============================================================
# Main
# ============================================================
MVN="${MVN:-mvn}"

case "${1:-all}" in
  infra)
    check_prerequisites
    start_infra
    setup_databases
    ;;
  services)
    check_prerequisites
    setup_databases
    start_services
    print_summary
    ;;
  frontend)
    start_frontend
    ;;
  build)
    check_prerequisites
    build_project
    start_infra
    setup_databases
    start_services
    start_frontend
    print_summary
    ;;
  stop)
    stop_all
    ;;
  status)
    show_status
    ;;
  db-setup)
    setup_databases
    ;;
  all)
    check_prerequisites
    start_infra
    setup_databases
    # Check if JARs exist, build if not
    if ! find "$PROJECT_ROOT/discovery-service/target" -name "*.jar" 2>/dev/null | grep -q jar; then
      build_project
    fi
    start_services
    start_frontend
    print_summary
    ;;
  *)
    echo "Usage: $0 {all|infra|services|frontend|build|stop|status|db-setup}"
    exit 1
    ;;
esac
