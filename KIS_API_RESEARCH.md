# 실시간 주식 검색을 위한 조사

> 조사일: 2026-03-18

---

## 한국투자증권 KIS Developers API

### 공식 링크
- 개발자 포털: https://apiportal.koreainvestment.com/intro
- API 문서: https://apiportal.koreainvestment.com/apiservice
- 공식 GitHub (예제): https://github.com/koreainvestment/open-trading-api
- Java 라이브러리: https://github.com/youhogeon/finance.kis_api
- Kotlin/Java 라이브러리: https://github.com/devngho/kt_kisopenapi

---

## 선행 조건

- 한국투자증권 계좌 필수 (모바일 비대면 개설 가능, 신분증만 필요)
- **모의투자 계좌로도 API 신청 가능** → 실제 돈 없이 개발/테스트 OK
- 신청 후 App Key / App Secret 발급 (포털에서 즉시 확인)

---

## API 방식

| 방식 | 용도 | 제한 |
|------|------|------|
| REST API | 현재가 단건 조회, 호가, 체결 정보 | 초당 20회 |
| WebSocket | 실시간 체결가/호가 스트림 구독 | 1세션 최대 41종목 |

- **비용**: 완전 무료 (매매 수수료는 HTS와 동일하나 조회만 할 경우 무료)
- **토큰**: POST `/oauth2/tokenP` 로 24시간짜리 access_token 발급

---

## 주요 엔드포인트

| 환경 | Base URL |
|------|----------|
| 실전투자 | `https://openapi.koreainvestment.com:9443` |
| 모의투자 | `https://openapivts.koreainvestment.com:29443` |

| 기능 | 엔드포인트 | TR ID |
|------|-----------|-------|
| 토큰 발급 | `POST /oauth2/tokenP` | - |
| 현재가 조회 | `GET /uapi/domestic-stock/v1/quotations/inquire-price` | `FHKST01010100` |
| 실시간 체결가 (WS) | WebSocket 구독 | `H0STCNT0` |
| 실시간 호가 (WS) | WebSocket 구독 | `H0STASP0` |

---

## financeHub 적용 방안

### 방안 A — REST 폴링 (간단)
- 장 중(09:00~15:30) 매 1~5분마다 상위 종목 현재가 REST 조회
- 기존 스케줄러에 장 중 전용 job 추가

### 방안 B — WebSocket 실시간 (정확)
- 백엔드에서 WebSocket 연결 유지
- 상위 거래량 종목 최대 20개 구독 → DB 또는 메모리 캐시 갱신
- 프론트는 SSE 또는 polling으로 수신

### 권장
모의투자 계좌로 방안 A 먼저 검증 → 안정화 후 방안 B 적용

---

## Spring Boot 연동 기본 구조

```java
// application.yml
kis:
  appkey: YOUR_APP_KEY
  appsecret: YOUR_APP_SECRET
  base-url: https://openapivts.koreainvestment.com:29443  # 모의투자

// 토큰 발급
POST /oauth2/tokenP
{
  "grant_type": "client_credentials",
  "appkey": "...",
  "appsecret": "..."
}
// → access_token (24시간 유효)

// 현재가 조회
GET /uapi/domestic-stock/v1/quotations/inquire-price?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=005930
Headers:
  authorization: Bearer {access_token}
  appkey: ...
  appsecret: ...
  tr_id: FHKST01010100
```
