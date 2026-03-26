-- KRX 데이터 테이블

-- KOSPI 일별 시세
CREATE TABLE IF NOT EXISTS financehub.kospi_daily_trading (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    bas_dd VARCHAR(8) NOT NULL,
    idx_clss VARCHAR(50),
    idx_nm VARCHAR(100) NOT NULL,
    clsprc_idx VARCHAR(20),
    cmpprevdd_idx VARCHAR(20),
    fluc_rt VARCHAR(20),
    opnprc_idx VARCHAR(20),
    hgprc_idx VARCHAR(20),
    lwprc_idx VARCHAR(20),
    acc_trdvol VARCHAR(30),
    acc_trdval VARCHAR(30),
    mktcap VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- KOSDAQ 일별 시세
CREATE TABLE IF NOT EXISTS financehub.kosdaq_daily_trading (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    bas_dd VARCHAR(8) NOT NULL,
    idx_clss VARCHAR(50),
    idx_nm VARCHAR(100) NOT NULL,
    clsprc_idx VARCHAR(20),
    cmpprevdd_idx VARCHAR(20),
    fluc_rt VARCHAR(20),
    opnprc_idx VARCHAR(20),
    hgprc_idx VARCHAR(20),
    lwprc_idx VARCHAR(20),
    acc_trdvol VARCHAR(30),
    acc_trdval VARCHAR(30),
    mktcap VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 금시장 일별 시세
CREATE TABLE IF NOT EXISTS financehub.gold_market_daily_trading (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    bas_dd VARCHAR(8) NOT NULL,
    isu_cd VARCHAR(20),
    isu_nm VARCHAR(100) NOT NULL,
    tdd_clsprc VARCHAR(20),
    cmpprevdd_prc VARCHAR(20),
    fluc_rt VARCHAR(20),
    tdd_opnprc VARCHAR(20),
    tdd_hgprc VARCHAR(20),
    tdd_lwprc VARCHAR(20),
    acc_trdvol VARCHAR(30),
    acc_trdval VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 석유시장 일별 시세
CREATE TABLE IF NOT EXISTS financehub.oil_market_daily_trading (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    bas_dd VARCHAR(8) NOT NULL,
    oil_nm VARCHAR(100) NOT NULL,
    wt_avg_prc VARCHAR(20),
    wt_dis_avg_prc VARCHAR(20),
    acc_trdvol VARCHAR(30),
    acc_trd_val VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 개별 주식 종목 일별 시세
CREATE TABLE IF NOT EXISTS financehub.stock_daily_trading (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    bas_dd VARCHAR(8) NOT NULL,
    isu_cd VARCHAR(20) NOT NULL,
    isu_srt_cd VARCHAR(20),
    isu_nm VARCHAR(100) NOT NULL,
    mkt_nm VARCHAR(20),
    sect_tp_nm VARCHAR(50),
    tdd_clsprc VARCHAR(20),
    cmpprevdd_prc VARCHAR(20),
    fluc_rt VARCHAR(20),
    tdd_opnprc VARCHAR(20),
    tdd_hgprc VARCHAR(20),
    tdd_lwprc VARCHAR(20),
    acc_trdvol VARCHAR(30),
    acc_trdval VARCHAR(30),
    mktcap VARCHAR(30),
    list_shrs VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 크롤러 데이터 테이블

-- 뉴스
CREATE TABLE IF NOT EXISTS financehub.news (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    link VARCHAR(1000),
    published_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 공모주
CREATE TABLE IF NOT EXISTS financehub.ipo (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    company_name TEXT NOT NULL,
    link TEXT,
    period TEXT,
    fixed_offering_price TEXT,
    expected_offering_price TEXT,
    subscription_rate TEXT,
    under_writer TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 스케줄러 실행 이력
CREATE TABLE IF NOT EXISTS financehub.scheduler_execution_log (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    execution_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    records_processed INT DEFAULT 0,
    records_inserted INT DEFAULT 0,
    records_skipped INT DEFAULT 0,
    error_message TEXT,
    execution_duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 해외주식 일별 시세
CREATE TABLE IF NOT EXISTS financehub.overseas_stock_daily_trading (
    id          BIGSERIAL PRIMARY KEY,
    data_hash   VARCHAR(64) UNIQUE,
    bass_dt     VARCHAR(8),
    isu_cd      VARCHAR(20),
    isu_nm      VARCHAR(100),
    excd        VARCHAR(10),
    curr        VARCHAR(10),
    cls_prc     NUMERIC(18,4),
    opn_prc     NUMERIC(18,4),
    hgst_prc    NUMERIC(18,4),
    lwst_prc    NUMERIC(18,4),
    acc_trd_vol BIGINT,
    fluc_rt     NUMERIC(8,2),
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_kospi_bas_dd ON financehub.kospi_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_kosdaq_bas_dd ON financehub.kosdaq_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_gold_bas_dd ON financehub.gold_market_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_oil_bas_dd ON financehub.oil_market_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_stock_bas_dd ON financehub.stock_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_stock_fluc_rt ON financehub.stock_daily_trading(bas_dd, fluc_rt);
CREATE INDEX IF NOT EXISTS idx_stock_acc_trdvol ON financehub.stock_daily_trading(bas_dd, acc_trdvol);
CREATE INDEX IF NOT EXISTS idx_news_published_at ON financehub.news(published_at);
CREATE INDEX IF NOT EXISTS idx_scheduler_log_job_name ON financehub.scheduler_execution_log(job_name);
CREATE INDEX IF NOT EXISTS idx_scheduler_log_execution_time ON financehub.scheduler_execution_log(execution_time);
CREATE INDEX IF NOT EXISTS idx_overseas_bass_dt ON financehub.overseas_stock_daily_trading(bass_dt);
CREATE INDEX IF NOT EXISTS idx_overseas_isu_cd ON financehub.overseas_stock_daily_trading(isu_cd);
CREATE UNIQUE INDEX IF NOT EXISTS idx_overseas_unique ON financehub.overseas_stock_daily_trading(bass_dt, isu_cd, excd);
