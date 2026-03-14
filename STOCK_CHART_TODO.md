# financeHub 개선 계획

## 완료된 항목

### 차트 화면 (StockView)
- [x] KOSPI/KOSDAQ 히스토리 DB 연동
- [x] 백엔드 API (`/dashboard/chart-data`)
- [x] KOSPI/KOSDAQ/금/오일 전환 버튼
- [x] 개별 종목 검색 백엔드 API (`/stocks/search`)
- [x] 1주/1개월/3개월/1년 기간 선택 버튼
- [x] 날짜 범위 선택 UI (시작일~종료일 picker)
- [x] 거래량 Bar 차트
- [x] 캔들 차트 (chartjs-chart-financial)
- [x] KOSPI+KOSDAQ 복수 지수 비교 차트

### 뉴스 화면 (NewsView)
- [x] 더미 데이터 제거, DB 실제 뉴스 연동

### 인프라
- [x] GitHub Actions CI/CD (self-hosted runner)
- [x] systemd 백엔드 자동 재시작
- [x] nginx 프론트 자동 배포

---

## 미완료 항목

### 높은 우선순위
- [x] DashboardView 뉴스 표시 버그 수정 (`{{ event }}` 객체 그대로 출력)
- [x] StockView 개별 종목 검색 UI (검색창 + 자동완성)
- [x] 오일 전일 대비 등락률 계산 (현재 빈값)

### 보통 우선순위
- [x] 스케줄러 수동 트리거 엔드포인트 추가 (`/admin/run-krx`, `/admin/run-all`)
- [x] 스케줄러 실행 로그 조회 API 추가 (`/admin/scheduler-logs`)
- [ ] 스케줄러 KRX API 실제 동작 확인 (배포 후 `/admin/run-krx` 호출로 검증)
- [x] 스케줄러 ARM 환경 크롤링 → Selenium 제거, Jsoup으로 교체 (브라우저 불필요)

### 낮은 우선순위
- [ ] 금/오일 데이터 없을 때 안내 문구
- [ ] 비교 차트 범례 개선

### 뉴스 고도화 (완료)
- [x] 뉴스 1시간 단위 자동 크롤링
- [x] 뉴스 필터 API (기간/키워드)
- [x] Vuetify 카드 리스트 UI (기간/주제/검색 필터)

### 공모주 (IPO) 고도화
- [ ] **DB 선행 작업** — 서버에서 ALTER TABLE 실행 (VARCHAR 50→200)
      ```sql
      ALTER TABLE financehub.ipo ALTER COLUMN period TYPE VARCHAR(200);
      ALTER TABLE financehub.ipo ALTER COLUMN fixed_offering_price TYPE VARCHAR(200);
      ALTER TABLE financehub.ipo ALTER COLUMN expected_offering_price TYPE VARCHAR(200);
      ALTER TABLE financehub.ipo ALTER COLUMN subscription_rate TYPE VARCHAR(200);
      ```
- [ ] **DB 추가 컬럼** — `created_at` 조회 지원 (현재 INSERT에만 사용, SELECT에서 누락)
      → `IpoDTO`에 `createdAt` 필드 추가
      → `selectLatestIpo` SQL에 `TO_CHAR(created_at, 'YYYY-MM-DD') AS createdAt` 추가
- [ ] **백엔드** — `selectFilteredIpo` SQL 추가 (CrawlerData.xml)
      → 조건: 종목명 키워드(`company_name ILIKE`), 기간(`created_at` DATE 범위)
      → 페이지네이션: `LIMIT #{limit} OFFSET #{offset}`, `ORDER BY created_at DESC`
      → `period` 컬럼은 "26.03.14~26.03.15" 텍스트라 날짜 파싱 불가 → `created_at` 기준 필터
- [ ] **백엔드** — `CrawlerDataMapper`에 `selectFilteredIpo` 메서드 추가
- [ ] **백엔드** — `IpoController` 신규 생성 (`GET /ipo/list?keyword=&startDate=&endDate=&limit=20&offset=0`)
- [ ] **프론트** — DashboardView 캘린더 섹션: compact 유지, 표시 건수 제한(월 기준 필터)
- [ ] **프론트** — IpoView 신규 or DashboardView 내 리스트 섹션
      → 최신순 내림차순, 페이지네이션(20건씩)
      → 필터: 키워드 검색 + 기간 버튼(이번달/3개월/전체)
      → Vuetify 카드 or 테이블 형태

### 인프라 개선
- [ ] deploy.yml — 배포 시 포트 8080 기존 프로세스 강제 종료 로직 추가 (nohup 잔존 프로세스 충돌 방지)
- [ ] nginx 라우팅 추가 — `/admin/`, `/news/` 경로 백엔드 프록시 설정
- [ ] KRX API 실제 수집 검증 — 평일 18:00 자동 수집 후 DB 데이터 확인
