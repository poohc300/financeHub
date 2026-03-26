# financeHub 서버 복구 런북

Raspberry Pi(ARM64) 에서 OS를 새로 세팅할 때 처음부터 순서대로 따라하면 된다.

---

## 시스템 구성 요약

```
[브라우저]
    ↓ HTTPS (443) — Cloudflare 경유
[nginx:alpine Docker 컨테이너]  ← 프론트 정적 파일 서빙 + /api/ /ws/ /news/ 프록시
    ↓ http://172.17.0.1:8080
[Spring Boot (app.jar)] — systemd 서비스 (financehub-backend)
    ↓ localhost:5432
[PostgreSQL:16-alpine Docker 컨테이너]
```

- **도메인**: nexacromancer.win (Cloudflare 프록시)
- **SSL**: Cloudflare Origin 인증서 (`ssl/origin.pem`, `ssl/origin.key`)
- **코드**: https://github.com/poohc300/financeHub
- **DB 백업**: `/home/jeremy/backups/financehub/` (평일 20:00 자동)

---

## 1단계 — OS 기본 설정

```bash
# 패키지 업데이트
sudo apt update && sudo apt upgrade -y

# 필수 패키지
sudo apt install -y git curl wget unzip docker.io docker-compose postgresql-client-16
```

### Docker 권한 설정
```bash
sudo usermod -aG docker jeremy
# 로그아웃 후 재로그인해야 적용됨
```

---

## 2단계 — Java 21 설치

> Ubuntu apt 기본 패키지가 아닌 Oracle JDK 21을 `/usr/local`에 직접 설치한다.

```bash
# ARM64용 JDK 21 다운로드 (Oracle 공식 또는 아래 링크)
# https://www.oracle.com/java/technologies/downloads/#java21 → Linux ARM64 tar.gz

wget https://download.oracle.com/java/21/latest/jdk-21_linux-aarch64_bin.tar.gz
sudo tar -xzf jdk-21_linux-aarch64_bin.tar.gz -C /usr/local/
# 압축 해제 후 폴더명 확인 (예: jdk-21.0.1)
sudo ln -s /usr/local/jdk-21.0.1 /usr/local/jdk

# 환경변수 등록
echo 'export JAVA_HOME=/usr/local/jdk-21.0.1' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

java -version  # openjdk 21 확인
```

---

## 3단계 — Node.js 20 설치

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node --version   # v20.x.x 확인
npm --version    # 9.x.x 확인
```

---

## 4단계 — 코드 클론 및 환경변수 설정

```bash
mkdir -p /home/jeremy/dev
cd /home/jeremy/dev
git clone https://github.com/poohc300/financeHub.git
cd financeHub
```

### 루트 .env 생성 (docker-compose용)
```bash
cat > .env << 'EOF'
POSTGRES_USER=financeuser
POSTGRES_PASSWORD=<실제 DB 비밀번호>
POSTGRES_DB=financehub
EOF
```

### 백엔드 .env 생성
```bash
cat > backend/.env << 'EOF'
DB_HOST=jdbc:postgresql://localhost:5432/financehub
DB_USERNAME=financeuser
DB_PASSWORD=<실제 DB 비밀번호>
JWT_SECRET=<JWT 시크릿 키>
JASYPT_ENCRYPTOR_PASSWORD=<Jasypt 패스워드>
KRX_API_KEY=<KRX API 키>
KIS_APP_KEY=<KIS 앱키>
KIS_APP_SECRET=<KIS 앱시크릿>
EOF
```

> ⚠️ 실제 값은 별도 보관 중인 자격증명에서 복사. `.env` 파일은 git에 올라가지 않는다.

---

## 5단계 — SSL 인증서 복원

Cloudflare Origin 인증서 2개 파일이 필요하다.

```bash
mkdir -p /home/jeremy/dev/financeHub/ssl
# 아래 두 파일을 안전한 저장소(USB, 비밀번호 관리자 등)에서 복사
cp <백업본>/origin.pem /home/jeremy/dev/financeHub/ssl/
cp <백업본>/origin.key /home/jeremy/dev/financeHub/ssl/
chmod 600 ssl/origin.key
```

> SSL 인증서를 분실했을 경우: Cloudflare 대시보드 → SSL/TLS → Origin Server → Create Certificate 에서 재발급.

---

## 6단계 — Docker 컨테이너 시작 (nginx + PostgreSQL)

```bash
cd /home/jeremy/dev/financeHub
docker-compose up -d

# 확인
docker ps  # nginx, postgres 두 컨테이너가 Up 상태여야 함
```

---

## 7단계 — DB 스키마 및 데이터 복원

### 스키마 생성
```bash
PGPASSWORD=<DB 비밀번호> psql -h localhost -U financeuser -d financehub -c "CREATE SCHEMA IF NOT EXISTS financehub;"
```

### 백업 데이터 복원
```bash
# 백업 파일을 USB 또는 원격 저장소에서 복사
ls /home/jeremy/backups/financehub/  # 또는 백업 파일 경로

# 가장 최근 백업 복원
LATEST=$(ls -1t /home/jeremy/backups/financehub/daily_trading_*.sql.gz | head -1)
gunzip -c "$LATEST" | PGPASSWORD=<DB 비밀번호> psql -h localhost -U financeuser -d financehub

# 복원 확인
PGPASSWORD=<DB 비밀번호> psql -h localhost -U financeuser -d financehub \
  -c "SELECT relname, n_live_tup FROM pg_stat_user_tables WHERE schemaname='financehub' ORDER BY relname;"
```

---

## 8단계 — 프론트엔드 빌드

```bash
cd /home/jeremy/dev/financeHub/app
npm install
npm run build
# dist/ 폴더 생성됨 → nginx 컨테이너가 자동으로 서빙
```

---

## 9단계 — 백엔드 빌드 및 systemd 서비스 등록

### 빌드
```bash
cd /home/jeremy/dev/financeHub/backend
./gradlew bootJar
# build/libs/financeHub-*.jar → app.jar 로 복사됨 (build.gradle에 설정됨)
```

### systemd 서비스 파일 등록
```bash
sudo tee /etc/systemd/system/financehub-backend.service << 'EOF'
[Unit]
Description=FinanceHub Spring Boot Backend
After=network.target

[Service]
User=jeremy
WorkingDirectory=/home/jeremy/dev/financeHub/backend
ExecStart=/usr/local/jdk-21.0.1/bin/java -jar /home/jeremy/dev/financeHub/backend/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable financehub-backend
sudo systemctl start financehub-backend
sudo systemctl status financehub-backend
```

---

## 10단계 — GitHub Actions self-hosted runner 등록

```bash
cd /home/jeremy/dev/financeHub
mkdir -p actions-runner && cd actions-runner

# ARM64용 runner 다운로드 (버전은 GitHub에서 최신 확인)
# https://github.com/poohc300/financeHub/settings/actions/runners/new
curl -o actions-runner-linux-arm64.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.322.0/actions-runner-linux-arm64-2.322.0.tar.gz
tar xzf actions-runner-linux-arm64.tar.gz

# GitHub에서 토큰 발급 후 등록
# Settings → Actions → Runners → New self-hosted runner → 토큰 복사
./config.sh --url https://github.com/poohc300/financeHub --token <GITHUB_RUNNER_TOKEN>

# systemd 서비스 등록
sudo ./svc.sh install jeremy
sudo ./svc.sh start
```

---

## 11단계 — cron 등록 (DB 자동 백업)

```bash
mkdir -p /home/jeremy/backups/financehub

(crontab -l 2>/dev/null; echo "0 20 * * 1-5 /home/jeremy/dev/financeHub/scripts/backup_db.sh >> /home/jeremy/backups/financehub/backup.log 2>&1") | crontab -

crontab -l  # 확인
```

---

## 12단계 — 동작 확인

```bash
# 1. 컨테이너 상태
docker ps

# 2. 백엔드 상태
sudo systemctl status financehub-backend
curl http://localhost:8080/dashboard/data | head -c 200

# 3. nginx 응답 (HTTPS)
curl -k https://localhost/

# 4. DB 데이터 확인
PGPASSWORD=<DB 비밀번호> psql -h localhost -U financeuser -d financehub \
  -c "SELECT COUNT(*) FROM financehub.kospi_daily_trading;"

# 5. GitHub Actions runner
sudo systemctl status "actions.runner.*"
```

---

## 보관해야 할 것들 (분실 시 복구 불가)

| 항목 | 위치/방법 |
|------|----------|
| DB 비밀번호, KIS API 키 등 | 비밀번호 관리자 (1Password 등) |
| Cloudflare SSL 인증서 | `ssl/origin.pem`, `ssl/origin.key` → USB 또는 암호화 저장소 |
| DB 백업 파일 | `/home/jeremy/backups/financehub/` → USB 복사 권장 |
| GitHub PAT (push용) | GitHub Settings → Developer settings에서 재발급 가능 |

---

## 자주 쓰는 운영 명령어

```bash
# 백엔드 재시작
sudo systemctl restart financehub-backend

# 백엔드 로그 실시간
journalctl -u financehub-backend -f

# 포트 8080 강제 종료
fuser -k 8080/tcp

# nginx 설정 반영 (컨테이너 재시작)
docker restart nginx

# DB 접속
PGPASSWORD=<비밀번호> psql -h localhost -U financeuser -d financehub

# 수동 KRX 수집
curl -X POST http://localhost:8080/admin/run-krx

# 수동 백업
/home/jeremy/dev/financeHub/scripts/backup_db.sh
```
