#!/bin/bash
# financehub DB 일별 거래 데이터 자동 백업
# 대상: kospi/kosdaq/gold/oil/stock daily_trading (financehub 스키마)
# 보관: 최근 30개 (약 1개월치)

set -euo pipefail

BACKUP_DIR="/home/jeremy/backups/financehub"
DB_NAME="financehub"
DB_USER="financeuser"
DB_HOST="localhost"
PGPASSWORD_VAL="test1234"
KEEP_DAYS=30

TABLES=(
  "financehub.kospi_daily_trading"
  "financehub.kosdaq_daily_trading"
  "financehub.gold_market_daily_trading"
  "financehub.oil_market_daily_trading"
  "financehub.stock_daily_trading"
)

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/daily_trading_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

# 테이블별 --table 옵션 조합
TABLE_ARGS=""
for t in "${TABLES[@]}"; do
  TABLE_ARGS="$TABLE_ARGS --table=$t"
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 백업 시작: $BACKUP_FILE"

PGPASSWORD="$PGPASSWORD_VAL" pg_dump \
  -h "$DB_HOST" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --no-owner \
  --no-acl \
  $TABLE_ARGS \
  | gzip > "$BACKUP_FILE"

SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 백업 완료: $SIZE"

# 오래된 백업 삭제 (30개 초과분)
COUNT=$(ls -1 "$BACKUP_DIR"/daily_trading_*.sql.gz 2>/dev/null | wc -l)
if [ "$COUNT" -gt "$KEEP_DAYS" ]; then
  DELETE_COUNT=$((COUNT - KEEP_DAYS))
  ls -1t "$BACKUP_DIR"/daily_trading_*.sql.gz | tail -"$DELETE_COUNT" | xargs rm -f
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] 오래된 백업 ${DELETE_COUNT}개 삭제"
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 현재 백업 파일 수: $(ls -1 "$BACKUP_DIR"/daily_trading_*.sql.gz 2>/dev/null | wc -l)"
