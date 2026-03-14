# financeHub 수정 내역

## 2026-03-14

### NewsView DB 연동
- `NewsView.vue` - 하드코딩 더미 데이터 제거, `/dashboard/data` API 실제 뉴스로 교체
- `DashboardDataDTO.ts` - `CrawledNewsDTO`에 `publishedAt` 필드 추가

### CI/CD 구축 (배포 서버)
- `.github/workflows/deploy.yml` - GitHub Actions self-hosted runner 워크플로우 추가
  - Frontend: `npm ci` → `npm run build` → dist 복사
  - Backend: `./gradlew bootJar` → jar 복사 → `systemctl restart financehub-backend`
  - 배포 후 서비스 상태 확인 로직 포함
- `.gitignore` - `actions-runner/` 추가

### 캔들차트 x축 타입 수정 (배포 서버 fix)
- `StockView.vue` - 캔들차트 x값을 문자열 대신 timestamp(ms)로 변환하여 정상 렌더링

### 캔들 차트 / 복수 지수 비교 / 금·오일 차트
- `ChartDataDTO` - OHLC 필드 추가 (`opens`, `highs`, `lows`)
- `KrxDataMapper` - Gold/Oil 히스토리 + 날짜범위 메서드 4개 추가
- `KrxData.xml` - `selectGoldHistory`, `selectGoldHistoryByDateRange`, `selectOilHistory`, `selectOilHistoryByDateRange` SQL 추가
- `DashboardController` - GOLD/OIL market 타입 처리 + KOSPI/KOSDAQ/STOCK OHLC 데이터 응답 추가
- `package.json` - `chartjs-chart-financial` 의존성 추가
- `StockView.vue` - 라인/캔들 차트 타입 토글, KOSPI+KOSDAQ 비교 차트, 금/오일 버튼 추가

### 날짜 범위 선택 (캘린더 picker)
- `KrxDataMapper` - KOSPI/KOSDAQ/Stock 날짜 범위 조회 메서드 추가
- `KrxData.xml` - `selectKospiHistoryByDateRange`, `selectKosdaqHistoryByDateRange`, `selectStockHistoryByDateRange` SQL 추가
- `DashboardController` - `startDate`, `endDate` 쿼리 파라미터 추가 (없으면 기존 limit 방식 사용)
- `StockView.vue` - 기간 버튼 옆에 날짜 범위 입력 UI 추가 (시작일~종료일 + 조회/초기화 버튼)

### 거래량 Bar 차트 추가
- `ChartDataDTO`에 `volumes` 필드 추가
- `DashboardController` - KOSPI/KOSDAQ/STOCK 차트 응답에 `accTrdvol` 포함
- `StockView.vue` - 지수 Line 차트 아래 거래량 Bar 차트 추가
- `StockView.vue` - 라벨 포맷 로직 `formattedLabels` computed로 공통화

### 환경 설정
- `application.yml` - `spring.config.import` 들여쓰기 수정 및 `optional:` 추가
- `application.yml` - logging level `DEBUG` → `INFO` 권장 (불필요한 AutoConfiguration 로그 제거)
