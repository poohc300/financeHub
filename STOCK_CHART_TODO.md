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
- [ ] 스케줄러 KRX API 연동 실제 동작 확인
- [ ] 스케줄러 ARM 환경 크로미움 연동 확인

### 낮은 우선순위
- [ ] 금/오일 데이터 없을 때 안내 문구
- [ ] 비교 차트 범례 개선
