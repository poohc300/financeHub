# KRX 데이터 수집 스케줄러 가이드

## 데이터 수집 스케줄

- **실행 시간**: 평일 18:00 (Asia/Seoul)
- **수집 대상**: KOSPI, KOSDAQ, 금시장, 석유시장, 개별주식, 뉴스, 공모주

## 수집 데이터

| 데이터 | 테이블 | 설명 |
|--------|--------|------|
| KOSPI 지수 | `kospi_daily_trading` | KOSPI 시리즈 일별 시세 |
| KOSDAQ 지수 | `kosdaq_daily_trading` | KOSDAQ 시리즈 일별 시세 |
| 금시장 | `gold_market_daily_trading` | 금 일별 매매 정보 |
| 석유시장 | `oil_market_daily_trading` | 석유 일별 매매 정보 |
| 개별주식 | `stock_daily_trading` | 개별 종목 일별 시세 (등락율/거래량 TOP용) |
| 뉴스 | `news` | 네이버 금융 뉴스 |
| 공모주 | `ipo` | 38.co.kr 공모주 일정 |

## 대시보드 데이터 흐름

```
스케줄러 (18:00) → 외부 API/크롤링 → DB 저장
                                        ↓
프론트엔드 → /dashboard/data API → DB 조회 → 응답
```

## 테스트 방법

### 1. 스케줄러 수동 실행

스케줄러 메서드를 직접 호출하는 테스트 코드 작성:

```java
@Autowired
private DataFetchScheduler scheduler;

// 전체 수집
scheduler.fetchAllData();

// 개별 수집
scheduler.fetchKospiData();
scheduler.fetchStockData();
```

### 2. 실행 이력 확인

```sql
SELECT * FROM financeHub.scheduler_execution_log
ORDER BY execution_time DESC
LIMIT 10;
```

### 3. 수집된 데이터 확인

```sql
-- 등락율 TOP 5
SELECT isu_nm, tdd_clsprc, fluc_rt
FROM financeHub.stock_daily_trading
WHERE bas_dd = (SELECT MAX(bas_dd) FROM financeHub.stock_daily_trading)
ORDER BY CAST(REPLACE(fluc_rt, ',', '') AS DECIMAL) DESC
LIMIT 5;

-- 거래량 TOP 5
SELECT isu_nm, tdd_clsprc, acc_trdvol
FROM financeHub.stock_daily_trading
WHERE bas_dd = (SELECT MAX(bas_dd) FROM financeHub.stock_daily_trading)
ORDER BY CAST(REPLACE(acc_trdvol, ',', '') AS BIGINT) DESC
LIMIT 5;
```

## 주의사항

- 대시보드는 DB에 저장된 데이터를 조회합니다
- 스케줄러가 실행되기 전에는 데이터가 비어있습니다
- 테스트 시에는 스케줄러를 수동 실행하거나 테이블에 데이터를 직접 입력하세요
