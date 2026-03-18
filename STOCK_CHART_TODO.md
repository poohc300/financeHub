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
- [x] 스케줄러 KRX API 실제 동작 확인 (2026-03-17 `/admin/run-krx-date` 호출로 검증 완료)
- [x] 스케줄러 ARM 환경 크롤링 → Selenium 제거, Jsoup으로 교체 (브라우저 불필요)

### 낮은 우선순위
- [x] 금/오일 데이터 없을 때 안내 문구 — 2026-03-18 완료
- [ ] 비교 차트 범례 개선
- [x] 모바일 UI 고도화 — 하단 탭바 활성 인디케이터, StockView 마켓 버튼 가로 스크롤 — 2026-03-18 완료
- [x] **모바일 탭바 Vuetify 전환** — 현재 Tailwind 커스텀 구현을 `v-bottom-navigation`으로 교체해 Material Design 스타일 적용 — 2026-03-18 완료
  - 현재: `App.vue` 50-67라인, Tailwind `grid grid-cols-4` + 커스텀 인디케이터
  - 전환 방향: `v-bottom-navigation` + `v-btn` (value= 라우터 path) + `useRouter().push()` 연동
  - 주요 props: `grow`, `mandatory`, `elevation="4"`, `color="primary"`, `mode="shift"` (활성 탭만 라벨 표시) 검토
  - 아이콘: emoji → `mdi-home`, `mdi-newspaper`, `mdi-chart-line`, `mdi-briefcase` 교체
  - 📖 참고자료:
    - 공식 문서: https://vuetifyjs.com/en/components/bottom-navigation/
    - API 레퍼런스: https://vuetifyjs.com/en/api/v-bottom-navigation
    - 레이아웃 예시: https://snips.vuetifyjs.com/application-ui/core-layouts/bottom-navigation-layouts
    - 로컬 타입 정의: `app/node_modules/vuetify/lib/components/VBottomNavigation/VBottomNavigation.d.ts`
- [x] 모바일 뉴스 링크 개선 — `window.open()` 강제 처리로 PWA 이탈 문제 해결 — 2026-03-18 완료

### 뉴스 고도화 (완료)
- [x] 뉴스 1시간 단위 자동 크롤링
- [x] 뉴스 필터 API (기간/키워드)
- [x] Vuetify 카드 리스트 UI (기간/주제/검색 필터)

### 공모주 (IPO) 고도화
- [x] **DB 선행 작업** — 컬럼이 이미 `text` 타입 (VARCHAR 제한 없음) — ALTER TABLE 불필요
- [x] **DB 추가 컬럼** — `created_at` 이미 존재, `IpoDTO.createdAt` 이미 구현
- [x] **백엔드** — `selectFilteredIpo` SQL (CrawlerData.xml) — `period LIKE '%~%'` 노이즈 필터 추가로 완료 (2026-03-18)
- [x] **백엔드** — `CrawlerDataMapper.selectFilteredIpo` 이미 구현
- [x] **백엔드** — `IpoController` (`GET /ipo/list`) 이미 구현
- [x] **프론트** — `IpoView.vue` (필터/페이지네이션/카드 UI) 이미 구현
- [x] **프론트** — 라우터 `/ipo`, 네비게이션 이미 연결 완료

### 주요 경제 지표 UI (완료)
- [x] 핵심 4개(코스피·코스닥·금·휘발유) 큼직한 카드 디폴트 표시
- [x] "전체 지표 보기" 접기/펼치기 — KOSPI·KOSDAQ 업종별 + 원자재·에너지 그룹
- [x] 코스닥 데이터 표시 추가
- [x] 종가 기준 표시로 수정 (시가→종가)

### 오늘의 TOP 5 고도화 (미완료)
- [x] 스케줄러 시간 18:00 → 16:00으로 당기기 (KRX 데이터 공개 직후 수집) — 2026-03-18 완료
- [x] 대시보드에 "마지막 업데이트 시각" 표시 + 수동 새로고침 버튼 — 2026-03-18 완료
- [ ] 장중 실시간 TOP 5 — 증권사 Open API 연동 검토 (한국투자증권, 키움 등)

### 오늘의 TOP 5 (완료)
- [x] 상승률 TOP 5 — 백엔드 SQL/Mapper/Controller (기존 구현)
- [x] 거래량 TOP 5 — 백엔드 SQL/Mapper/Controller (기존 구현)
- [x] DashboardView UI 개선 — 순위 뱃지, 마켓 뱃지, 등락률 화살표, 거래량 단위 변환

### KRX 수동 수집 / 백필 (완료)
- [x] 날짜별 KRX 수집 API 추가 (`/admin/run-krx-date?date=YYYYMMDD`)
- [x] 금·유가 단독 수집 API 추가 (`/admin/run-gold`, `/admin/run-oil`, `/admin/run-commodity`)
- [x] 이란전쟁 시작(2026-02-28) ~ 오늘(2026-03-17) KOSPI·KOSDAQ·금·유가 백필 완료
- [x] run-krx-date에 개별 종목(stock) 수집 누락 수정 + 종목 백필 완료

### 인프라 개선
- [x] deploy.yml — 배포 시 `fuser -k 8080/tcp` 로 포트 기준 프로세스 종료 추가 (2026-03-17 장애 재발 방지) — 2026-03-18 완료
- [x] nginx `/api/admin/` location block 분리 + IP 제한 또는 Basic Auth 적용 (이슈 3) — 2026-03-18 완료
- [x] nginx 라우팅 추가 — `/admin/`, `/news/` 경로 백엔드 프록시 설정 — 2026-03-18 완료
- [ ] KRX API 평일 18:00 자동 수집 후 DB 데이터 자동 검증 (알림 포함)
