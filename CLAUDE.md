# financeHub — Claude 작업 지침

이 파일은 세션이 새로 시작될 때마다 Claude가 읽어야 할 프로젝트 규칙이다.

---

## 1. 코드 변경 후 필수 워크플로우

코드를 수정했다면 아래 순서를 **반드시** 따른다.

### 1-1. 빌드

| 변경 위치 | 빌드 명령어 | 작업 디렉토리 |
|-----------|------------|--------------|
| 프론트엔드 (`app/`) | `npm run build` | `/home/jeremy/dev/financeHub/app` |
| 백엔드 (`backend/`) | `./gradlew bootJar` | `/home/jeremy/dev/financeHub/backend` |
| 둘 다 | 프론트 먼저, 이후 백엔드 순서 | 각각 |

빌드 실패 시 에러를 확인하고 수정한 뒤 다시 빌드한다. 빌드가 성공해야 다음 단계로 넘어간다.

### 1-2. Git 커밋 & 푸시

```bash
# 관련 파일만 스테이징 (git add -A 금지 — .env, 빌드 아티팩트 포함 위험)
git add <변경된 파일들>

# 커밋 메시지 형식
git commit -m "feat|fix|docs|refactor: 한 줄 요약

- 세부 변경사항 1
- 세부 변경사항 2

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"

git push origin main
```

### 1-3. 완료 확인

푸시 후 다음을 확인하고 사용자에게 알린다:
- 커밋 해시 (`git log --oneline -1`)
- 변경된 파일 수
- 빌드 결과 요약

---

## 2. 문서 관리 규칙

프로젝트 문서는 **Notion**으로 통합 관리한다. MD 파일로 관리하지 않는다.

| 문서 종류 | Notion 위치 |
|----------|------------|
| TODO / 할일 | FinanceHub 프로젝트 관리 → 할일 관리 |
| 변경 이력 | FinanceHub 프로젝트 관리 → 5. 주요 변경 이력 |
| 이슈/버그 | FinanceHub 프로젝트 관리 → 이슈 트래킹 |
| 공통 인프라 | 사이드 프로젝트 → 공통 인프라 관리 |

### 규칙 요약
```
코드 변경 → Notion 주요 변경 이력 업데이트
이슈 발생 → Notion 이슈 트래킹에 추가
이슈 해결 → Notion 이슈 트래킹에 해결 방법 추가
작업 완료 → Notion 할일 관리에서 [x] 처리
새 작업   → Notion 할일 관리에 [ ] 추가
```

> ⚠️ 예외: `RUNBOOK.md`는 서버 장애 시 오프라인에서 참조해야 하므로 코드베이스에 유지한다.

---

## 3. 프로젝트 구조 요약

```
financeHub/
├── app/                  # Vue 3 + TypeScript 프론트엔드
│   └── src/views/        # DashboardView, StockView, NewsView, IpoView
├── backend/              # Spring Boot 3 백엔드
│   └── src/main/java/com/example/financeHub/
│       ├── krx/          # KRX API 연동 (fetcher, service, mapper)
│       ├── scheduler/    # 스케줄러 + 수동 트리거 컨트롤러
│       ├── crawler/      # 뉴스/IPO Jsoup 크롤러
│       └── dashboard/    # 대시보드 API
└── RUNBOOK.md            # 서버 복구 런북 (오프라인 참조용)
```

## 4. 주요 API 엔드포인트

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /dashboard/data` | 대시보드 전체 데이터 |
| `GET /dashboard/chart-data?market=KOSPI&...` | 차트 데이터 |
| `POST /admin/run-krx-date?date=YYYYMMDD` | 날짜별 KRX 전체 수집 |
| `POST /admin/run-gold?date=YYYYMMDD` | 날짜별 금 수집 |
| `POST /admin/run-oil?date=YYYYMMDD` | 날짜별 유가 수집 |
| `POST /admin/run-krx` | 최근 영업일 KRX 수동 수집 |

## 5. 환경 정보

- 서버: Raspberry Pi (ARM64), Linux
- 백엔드 포트: 8080 (`fuser -k 8080/tcp` 으로 종료)
- DB: PostgreSQL, 스키마 `financehub`
- 프론트: nginx 서빙, `/api/` → 백엔드 프록시
