#!/bin/bash
# FinanceHub 재부팅 후 기동 점검 및 자동 복구 스크립트
# 실행: sudo /home/jeremy/dev/financeHub/scripts/startup-check.sh
# systemd: financehub-startup-check.service

PROJ="/home/jeremy/dev/financeHub"
LOG_DIR="$PROJ/logs"
LOG_FILE="$LOG_DIR/startup-$(date +%Y%m%d_%H%M%S).log"
BACKEND_URL="http://localhost:8080/dashboard/data"
BACKEND_MAX_WAIT=180   # 백엔드 HTTP 응답 대기 최대 3분
BACKEND_MAX_RESTARTS=2 # 최대 재시작 횟수

mkdir -p "$LOG_DIR"

log()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"; }
ok()   { log "[OK]   $1"; }
fail() { log "[FAIL] $1"; }
info() { log "[INFO] $1"; }

ERRORS=0

# ──────────────────────────────────────────
# 1. Docker 데몬
# ──────────────────────────────────────────
check_docker() {
    info "Docker 데몬 확인..."
    if ! docker info &>/dev/null; then
        fail "Docker 미실행 → 시작 중"
        systemctl start docker
        sleep 10
    fi
    if docker info &>/dev/null; then
        ok "Docker 데몬 실행 중"
    else
        fail "Docker 시작 실패"
        ERRORS=$((ERRORS + 1))
    fi
}

# ──────────────────────────────────────────
# 2. Docker 컨테이너 (nginx, postgres)
# ──────────────────────────────────────────
check_container() {
    local name="$1"
    local running
    running=$(docker inspect --format='{{.State.Running}}' "$name" 2>/dev/null || echo "false")

    if [ "$running" = "true" ]; then
        ok "컨테이너 '$name' 실행 중"
        return
    fi

    fail "컨테이너 '$name' 중단됨 → 재시작"
    cd "$PROJ" && docker-compose up -d "$name" 2>>"$LOG_FILE"
    sleep 8

    running=$(docker inspect --format='{{.State.Running}}' "$name" 2>/dev/null || echo "false")
    if [ "$running" = "true" ]; then
        ok "컨테이너 '$name' 복구 완료"
    else
        fail "컨테이너 '$name' 복구 실패"
        ERRORS=$((ERRORS + 1))
    fi
}

# ──────────────────────────────────────────
# 3. PostgreSQL 연결
# ──────────────────────────────────────────
check_postgres() {
    info "PostgreSQL 연결 확인..."
    local i
    for i in $(seq 1 12); do
        if docker exec postgres pg_isready -U financeuser -d financehub -q 2>/dev/null; then
            ok "PostgreSQL 응답 정상"
            return
        fi
        info "PostgreSQL 대기 중... ($i/12)"
        sleep 5
    done
    fail "PostgreSQL 연결 실패 (60초 초과)"
    ERRORS=$((ERRORS + 1))
}

# ──────────────────────────────────────────
# 4. 백엔드 HTTP
# ──────────────────────────────────────────
_wait_backend_http() {
    local waited=0
    while [ "$waited" -lt "$BACKEND_MAX_WAIT" ]; do
        if curl -sf --max-time 5 "$BACKEND_URL" &>/dev/null; then
            ok "백엔드 HTTP 응답 정상 (${waited}초 후)"
            return 0
        fi
        sleep 10
        waited=$((waited + 10))
        info "백엔드 대기 중... (${waited}/${BACKEND_MAX_WAIT}초)"
    done
    return 1
}

check_backend() {
    info "백엔드 서비스 확인..."

    if ! systemctl is-active --quiet financehub-backend; then
        info "백엔드 서비스 비활성 → 시작 중"
        systemctl start financehub-backend
        sleep 5
    else
        ok "백엔드 서비스 active (systemd)"
    fi

    local restarts=0
    while [ "$restarts" -le "$BACKEND_MAX_RESTARTS" ]; do
        if _wait_backend_http; then
            return
        fi

        restarts=$((restarts + 1))
        if [ "$restarts" -le "$BACKEND_MAX_RESTARTS" ]; then
            fail "백엔드 HTTP 무응답 (${BACKEND_MAX_WAIT}초) → 재시작 ($restarts/$BACKEND_MAX_RESTARTS)"
            systemctl restart financehub-backend
            sleep 10
        fi
    done

    fail "백엔드 ${BACKEND_MAX_RESTARTS}회 재시작 후에도 응답 없음"
    ERRORS=$((ERRORS + 1))
}

# ──────────────────────────────────────────
# 5. nginx HTTPS
# ──────────────────────────────────────────
check_nginx() {
    info "nginx 확인..."
    if curl -skf --max-time 5 https://localhost/ &>/dev/null; then
        ok "nginx HTTPS 응답 정상"
        return
    fi

    fail "nginx 무응답 → 재시작"
    docker restart nginx 2>>"$LOG_FILE"
    sleep 8

    if curl -skf --max-time 5 https://localhost/ &>/dev/null; then
        ok "nginx 재시작 완료"
    else
        fail "nginx 재시작 후에도 응답 없음"
        ERRORS=$((ERRORS + 1))
    fi
}

# ──────────────────────────────────────────
# 메인
# ──────────────────────────────────────────
log "========================================"
log "  FinanceHub 기동 점검 시작"
log "========================================"

check_docker
check_container "postgres"
check_container "nginx"
check_postgres
check_backend
check_nginx

log "========================================"
if [ "$ERRORS" -eq 0 ]; then
    log "  결과: 모든 서비스 정상"
else
    log "  결과: 오류 ${ERRORS}건 발생 — $LOG_FILE 확인"
fi
log "========================================"

exit "$ERRORS"
