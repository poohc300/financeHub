# 주식 차트 화면 개선 계획

## 현재 상태
- 차트 라이브러리: vue-chartjs (Chart.js) 사용 중
- 레이아웃: 좌측 거래량 TOP + 우측 차트 (3열 그리드)
- 데이터: DB에서 조회 (스케줄러 수집 데이터)

## 개선 필요 사항

### 1. DB 데이터 연동 ✅ 완료
- [x] 하드코딩 → 실제 KOSPI/KOSDAQ 히스토리 조회
- [x] 백엔드 API 추가 (`/dashboard/chart-data`)
- [x] 프론트엔드 데이터 연동
- [x] KOSPI/KOSDAQ 전환 버튼 추가

### 2. 기간 선택 ✅ 완료
- [x] 1주/1개월/3개월/1년 선택 버튼 추가
- [x] 날짜 범위 선택 UI (시작일~종료일 picker)

### 3. 지수/종목 선택 ✅ 완료
- [x] KOSPI, KOSDAQ 선택 버튼
- [x] 금, 오일 선택 버튼
- [x] 개별 종목 검색 및 선택 (백엔드 API 구현됨)

### 4. 추가 차트 ✅ 완료
- [x] 거래량 차트 (Bar Chart)
- [x] 캔들 차트 (Candlestick) - chartjs-chart-financial
- [x] 복수 지수 비교 차트 (KOSPI + KOSDAQ 동시 표시)

### 5. 스케줄러
- [ ] ARM 환경에서는 크로미움 추가
- [ ] KRX API 제대로 연동되는지 확인 필요