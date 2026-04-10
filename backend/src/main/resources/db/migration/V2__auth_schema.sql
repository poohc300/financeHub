-- ============================================================
-- V2: 인증/인가 스키마 (JWT + RBAC)
-- 생성일: 2026-04-10
-- ============================================================

-- ============================================================
-- 1. users — 사용자 계정
--    password: LOCAL 로그인 시 BCrypt 해시, OAuth2 사용자는 NULL
--    login_type: LOCAL | GOOGLE | KAKAO (향후 확장)
-- ============================================================
CREATE TABLE financehub.users (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password    VARCHAR(255),
    email       VARCHAR(100) UNIQUE,
    login_type  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 2. roles — 역할
--    현재: ROLE_ADMIN, ROLE_USER
-- ============================================================
CREATE TABLE financehub.roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  UNIQUE NOT NULL,
    description VARCHAR(200)
);

-- ============================================================
-- 3. permissions — 권한 (리소스:액션 형식)
-- ============================================================
CREATE TABLE financehub.permissions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(200)
);

-- ============================================================
-- 4. role_permissions — 역할 ↔ 권한 N:M 매핑
-- ============================================================
CREATE TABLE financehub.role_permissions (
    role_id       BIGINT NOT NULL REFERENCES financehub.roles(id)       ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES financehub.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- 5. user_roles — 사용자 ↔ 역할 N:M 매핑
-- ============================================================
CREATE TABLE financehub.user_roles (
    user_id BIGINT NOT NULL REFERENCES financehub.users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES financehub.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- 6. refresh_tokens — JWT Refresh Token 관리
--    token_hash: SHA-256 해시 저장 (원본 저장 X)
--    family:     Rotation Family UUID — 탈취된 토큰 재사용 감지 시
--                같은 family 전체 revoke
-- ============================================================
CREATE TABLE financehub.refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES financehub.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    family      UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 7. oauth_accounts — OAuth2 소셜 계정 연동 (미래 확장용)
--    현재 비어있음. Google / Kakao 연동 시 사용.
--    한 사용자가 여러 OAuth 제공자와 연동 가능 (1:N)
-- ============================================================
CREATE TABLE financehub.oauth_accounts (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES financehub.users(id) ON DELETE CASCADE,
    provider         VARCHAR(20)  NOT NULL,       -- GOOGLE | KAKAO
    provider_user_id VARCHAR(255) NOT NULL,       -- 제공자의 고유 사용자 ID
    email            VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_user_id)
);

-- ============================================================
-- 인덱스
-- ============================================================
CREATE INDEX idx_refresh_tokens_user_id   ON financehub.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family    ON financehub.refresh_tokens(family);
CREATE INDEX idx_refresh_tokens_expires   ON financehub.refresh_tokens(expires_at);
CREATE INDEX idx_oauth_accounts_user_id   ON financehub.oauth_accounts(user_id);

-- ============================================================
-- SEED: 역할
-- ============================================================
INSERT INTO financehub.roles (name, description) VALUES
    ('ROLE_ADMIN', '시스템 관리자 — 모든 기능 접근 가능'),
    ('ROLE_USER',  '일반 사용자 — 조회 + 관심종목 관리');

-- ============================================================
-- SEED: 권한 (리소스:액션)
-- ============================================================
INSERT INTO financehub.permissions (name, description) VALUES
    ('dashboard:read',  '대시보드 조회'),
    ('news:read',       '뉴스 조회'),
    ('stock:read',      '주식 차트 조회'),
    ('ipo:read',        '공모주 조회'),
    ('overseas:read',   '해외주식 조회'),
    ('insight:read',    '인사이트 조회'),
    ('watchlist:read',  '관심종목 조회'),
    ('watchlist:write', '관심종목 등록/수정/삭제'),
    ('admin:execute',   '스케줄러 수동 실행'),
    ('admin:users',     '사용자 관리');

-- ============================================================
-- SEED: 역할별 권한 부여
-- ROLE_ADMIN → 모든 권한
-- ROLE_USER  → 조회 전체 + watchlist write
-- ============================================================
INSERT INTO financehub.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM financehub.roles r, financehub.permissions p
WHERE r.name = 'ROLE_ADMIN';

INSERT INTO financehub.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM financehub.roles r, financehub.permissions p
WHERE r.name = 'ROLE_USER'
  AND p.name IN (
      'dashboard:read', 'news:read', 'stock:read', 'ipo:read',
      'overseas:read',  'insight:read', 'watchlist:read', 'watchlist:write'
  );

-- ============================================================
-- SEED: 초기 ADMIN 계정
--   username : admin
--   password : admin1234!  (BCrypt $2b$10$ 해시)
--   ⚠️  최초 기동 후 반드시 비밀번호 변경 필요
-- ============================================================
INSERT INTO financehub.users (username, password, email, login_type) VALUES
    ('admin',
     '$2b$10$3IH40BCpQRJPaMCyeWIj9O9LuoRiOEG5hmuMMoiWnhRkOIiJaXKYO',
     'admin@financehub.local',
     'LOCAL');

INSERT INTO financehub.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM financehub.users u, financehub.roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
