# financeHub 수정 내역

## 2026-03-18 (9차)

### 스케줄러 당일 데이터 미수집 버그 수정

- `KrxCommonUtil.java` — `getLastTradingDay()` 버그 수정
  - 수정 전: 평일 항상 `minusDays(1)` (어제) 반환 → 16시 실행 시 오늘 데이터를 못 가져옴
  - 수정 후: 월~금은 오늘 반환, 토·일만 직전 금요일 반환
- KOSPI·KOSDAQ·금·유가·종목 전 데이터 스케줄러 정상화

## 2026-03-18 (8차)

### 비교 차트 범례 개선

- `StockView.vue` — 비교 모드 시 차트 제목 "KOSPI vs 코스닥 비교"로 변경
- Chart.js 기본 범례 숨기고 커스텀 범례 배지 추가 (색상선 + 좌축/우축 안내)

## 2026-03-18 (7차)

### 모바일 뉴스/공모주 링크 개선

- `openLink.ts` — `window.open(url, '_blank', 'noopener,noreferrer')` 공용 유틸 추가
- `NewsView.vue` — v-card href 제거 → `@click="openLink()"` 처리
- `DashboardView.vue` — 뉴스 `<a>` href 제거 → `@click="openLink()"` 처리
- `IpoView.vue` — 공모주명 링크 href 제거 → `@click="openLink()"` 처리
- PWA/모바일 브라우저에서 앱을 벗어나는 문제 해결

## 2026-03-18 (6차)

### 모바일 탭바 Vuetify v-bottom-navigation 전환

- `App.vue` — 커스텀 Tailwind 탭바 → `v-bottom-navigation` 교체
  - `useDisplay` 로 모바일(md 미만) 조건부 렌더링
  - emoji 아이콘 → MDI 아이콘 (`mdi-home`, `mdi-newspaper-variant-outline`, `mdi-chart-line`, `mdi-briefcase-outline`)
  - 활성 탭 자동 색상 + elevation 그림자 적용

## 2026-03-18 (5차)

### nginx 라우팅 + 모바일 UI + 빈 데이터 안내

- `nginx.conf` — `/admin/` 직접 프록시 추가 (IP 제한), `/news/` 백엔드 프록시 추가
- `App.vue` — 모바일 탭바 활성 탭 상단 인디케이터 라인 추가
- `StockView.vue` — 마켓 버튼 모바일 가로 스크롤 처리 (`overflow-x-auto`, `flex-shrink-0`)
- `DashboardView.vue` — 금/오일 데이터 없을 때 "데이터 없음 · 평일 16시 수집" 안내 문구 표시

## 2026-03-18 (4차)

### 공모주(IPO) 고도화 완료

- `CrawlerData.xml` — `selectFilteredIpo`에 `period LIKE '%~%'` 노이즈 필터 추가 (크롤링 쓰레기 데이터 제거)
- 백엔드: `IpoController` (`GET /ipo/list`), `selectFilteredIpo` SQL, `CrawlerDataMapper` 이미 구현 확인
- 프론트: `IpoView.vue` (필터/페이지네이션), 라우터 `/ipo`, 네비 이미 구현 확인
- DB: 컬럼 이미 `text` 타입으로 VARCHAR 제한 없음 — ALTER TABLE 불필요

## 2026-03-18 (3차)

### 스케줄러 + 대시보드 UX 개선

- `DataFetchScheduler.java` — KRX 수집 스케줄 18:00 → 16:00 조정 (KRX 데이터 공개 직후 수집)
- `DashboardView.vue` — 마지막 업데이트 시각 표시 + 수동 새로고침 버튼 추가

## 2026-03-18 (2차)

### 인프라: 배포 시 포트 충돌 방지

- `deploy.yml` — `systemctl restart` 전 `fuser -k 8080/tcp` 추가, 포트 점유 프로세스 강제 종료

## 2026-03-18

### 보안: nginx /api/admin/ IP 접근 제한

- `nginx.conf` — `/api/admin/` location block 분리, 내부 IP(127.0.0.1, 192.168.x.x, 10.x.x.x)만 허용, 외부 접근 403 차단
- `deploy.yml` — 배포 시 `docker exec nginx nginx -s reload` 추가 (nginx 설정 변경 자동 반영)

## 2026-03-17 (5차)

### 모바일 네비게이션 + 뉴스 링크 수정

- `App.vue` — 모바일 하단 탭바 추가 (데스크탑 상단 nav 유지, 모바일 fixed bottom tab)
- `NewsCrawler.java` — 뉴스 링크 URL 정규화 추가
  - `finance.naver.com/news/news_read.naver?article_id=XXX&office_id=YYY` → `n.news.naver.com/article/YYY/XXX`
  - 기존 URL은 모바일에서 Pay증권 투자정보 페이지로 리다이렉트되는 문제 해결

---

## 2026-03-17 (4차)

### 모바일 네비게이션 바 수정 (하단 탭바)
- 위 5차에 통합

---

## 2026-03-17 (3차)

### 주요 경제 지표 UI 개편 — 핵심 4개 + 전체 접기/펼치기

- `DashboardDataDTO.ts` — `KosdaqDailyTradingDTO` 인터페이스 추가, `DashboardDataDTO`에 `kosdaqDailyTradingList` 추가
- `DashboardView.vue` — 경제 지표 섹션 전면 재작성
  - 디폴트: 코스피·코스닥·금 1kg·휘발유 4개만 큼직한 카드로 표시
  - "전체 지표 보기 ▼" 버튼으로 KOSPI 업종별·KOSDAQ 업종별·원자재·에너지 전체 펼치기
  - 코스닥 데이터 표시 추가 (기존에 API에서 받아도 미표시 상태였음)
  - `mapKospi` 시가(opnprcIdx) → 종가(clsprcIdx) 표시로 수정

---

## 2026-03-17 (2차)

### TOP 5 미표시 버그 수정 — run-krx-date 종목 수집 누락

- `StockFetcher.java` — `fetch(String formattedDate)` 오버로드 추가
- `KrxDataService.java` — `getStockDailyTradingInfo(String formattedDate)` 추가
- `DataFetchScheduler.java` — `fetchStockData(String date)` 추가
- `SchedulerController.java` — `run-krx-date` 응답에 `stock` 필드 추가
- 2026-02-28 ~ 2026-03-17 개별 종목 데이터 백필 완료 (날짜당 ~950개)

---

## 2026-03-17

### KRX 날짜별 수동 수집 기능 추가 + 이란전쟁 이후 데이터 백필

#### 백엔드 — Fetcher 날짜 파라미터 오버로드
- `GoldMarketFetcher.java` — `fetch(String formattedDate)` 추가 (기존 `fetch()`는 위임으로 단순화)
- `OilMarketFetcher.java` — 동일
- `KospiFetcher.java` — 동일
- `KosdaqFetcher.java` — 동일

#### 백엔드 — KrxDataService 날짜별 메서드 추가
- `getGoldMarketDailyTradingInfo(String formattedDate)`
- `getOilMarketDailyTradingInfo(String formattedDate)`
- `getKospiDailyTradingInfo(String formattedDate)`
- `getKosdaqDailyTradingInfo(String formattedDate)`

#### 백엔드 — DataFetchScheduler 날짜별 메서드 추가
- `fetchKospiData(String date)`, `fetchKosdaqData(String date)`, `fetchGoldMarketData(String date)`, `fetchOilMarketData(String date)` 추가
- 기존 void 메서드는 날짜 파라미터 버전으로 위임
- `FetchResult` record 추가 (processed / inserted / skipped / status / errorMessage)

#### 백엔드 — SchedulerController 엔드포인트 추가
- `POST /admin/run-krx-date?date=YYYYMMDD` — KOSPI + KOSDAQ + 금 + 유가 일괄 수집
- `POST /admin/run-gold?date=YYYYMMDD` — 금 시세 단독 수집
- `POST /admin/run-oil?date=YYYYMMDD` — 유가 단독 수집
- `POST /admin/run-commodity?date=YYYYMMDD` — 금 + 유가 동시 수집

#### 데이터 백필
- 이란전쟁 시작일(2026-02-28)부터 오늘(2026-03-17)까지 KRX 영업일 데이터 전량 수집
- KOSPI 51개 × 13일, KOSDAQ 40개 × 13일, 금 2개 × 13일, 유가 3개 × 13일 삽입

---

## 2026-03-14 (5차)

### 뉴스 1시간 크롤링 + Vuetify 카드 리스트 + 필터
- `DataFetchScheduler.java` - `fetchNewsHourly()` 추가 (`0 0 * * * *` 매 정시)
- `NewsDTO.java` - `createdAt` 필드 추가 (크롤링 시각)
- `CrawlerDataMapper.java` - `selectFilteredNews()` 추가 (기간/키워드 필터)
- `CrawlerData.xml` - `selectFilteredNews` SQL 추가 (MyBatis 동적 WHERE)
- `NewsController.java` (신규) - `GET /news/list` 필터 조회 API
- `package.json` - vuetify, @mdi/font, vite-plugin-vuetify 추가
- `vite.config.ts` - vite-plugin-vuetify 적용
- `tailwind.config.js` - preflight 비활성화 (Vuetify CSS reset과 충돌 방지)
- `main.ts` - Vuetify 전역 등록
- `NewsView.vue` - Vuetify 카드 리스트로 전면 재작성
  - 기간 버튼 (오늘/3일/1주일/1개월/전체)
  - 주제 칩 (증시/코스피/환율/금리/부동산/암호화폐)
  - 키워드 검색 (debounce 400ms)
  - 더 보기 (offset 기반 페이지네이션)

## 2026-03-14 (4차)

### ARM64 크롤링 Selenium → Jsoup 전환
- `build.gradle` - `selenium-java` 제거, `jsoup:1.18.3` 추가
- `NewsCrawler.java` - Selenium/ChromeDriver 제거, Jsoup으로 재작성 (네이버 금융 뉴스)
- `IpoCrawler.java` - Selenium/ChromeDriver 제거, Jsoup으로 재작성 (38.co.kr 공모주)
- `WebDriverConfig.java` - Selenium Bean 제거 (빈 클래스로 교체)
- 효과: 브라우저/Chromium 없이 ARM64 서버에서 크롤링 정상 동작

## 2026-03-14 (3차)

### 스케줄러 수동 트리거 및 로그 조회 API
- `SchedulerController.java` (신규) - 수동 실행 및 로그 조회 엔드포인트
  - `POST /admin/run-krx` - KRX 데이터만 수동 수집 (Selenium 불필요)
  - `POST /admin/run-all` - 전체 수집 (뉴스/IPO 포함, Chromium 필요)
  - `GET /admin/scheduler-logs?limit=50` - 최근 실행 로그 조회
- `CrawlerDataMapper.java` - `selectRecentLogs` 메서드 추가
- `CrawlerData.xml` - `selectRecentLogs` SQL 추가

## 2026-03-14 (2차)

### StockView 종목 검색 UI
- `StockView.vue` - 종목명 검색 입력창 + 자동완성 드롭다운 추가 (debounce 300ms)
- `StockView.vue` - `selectStock` 함수: 종목 선택 시 isuCd 저장 후 차트 전환
- `StockView.vue` - `buildUrl` 수정: STOCK 마켓일 때 isuCd 파라미터로 URL 구성

### 오일 전일 대비 등락률 계산
- `OilMarketDailyTradingDTO.java` - `flucRt` 필드 추가 (DB 미저장, 컨트롤러 계산값)
- `KrxData.xml` - `selectPrevOilMarket` SQL 추가 (직전 영업일 오일 데이터 조회)
- `KrxDataMapper.java` - `selectPrevOilMarket()` 인터페이스 추가
- `DashboardController.java` - 전일 오일 가격 조회 후 등락률 계산하여 `flucRt` 세팅
- `DashboardDataDTO.ts` - `OilMarketDailtyTradingDTO`에 `flucRt` 필드 추가
- `DashboardView.vue` - `mapOil` 함수에서 `flucRt` 사용하여 등락률 표시

## 2026-03-14

### DashboardView 뉴스 표시 버그 수정
- `DashboardView.vue` - `{{ event }}` 객체 출력 버그 수정 → `news.title` + 링크 클릭 가능하도록 변경

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
