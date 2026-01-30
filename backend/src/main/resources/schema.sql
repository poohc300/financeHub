-- KRX 데이터 테이블

-- KOSPI 일별 시세
CREATE TABLE IF NOT EXISTS financeHub.kospi_daily_trading (
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
CREATE TABLE IF NOT EXISTS financeHub.kosdaq_daily_trading (
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
CREATE TABLE IF NOT EXISTS financeHub.gold_market_daily_trading (
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
CREATE TABLE IF NOT EXISTS financeHub.oil_market_daily_trading (
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
CREATE TABLE IF NOT EXISTS financeHub.stock_daily_trading (
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
CREATE TABLE IF NOT EXISTS financeHub.news (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    link VARCHAR(1000),
    published_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 공모주
CREATE TABLE IF NOT EXISTS financeHub.ipo (
    id BIGSERIAL PRIMARY KEY,
    data_hash VARCHAR(64) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    link VARCHAR(1000),
    period VARCHAR(50),
    fixed_offering_price VARCHAR(50),
    expected_offering_price VARCHAR(50),
    subscription_rate VARCHAR(50),
    under_writer VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 스케줄러 실행 이력
CREATE TABLE IF NOT EXISTS financeHub.scheduler_execution_log (
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

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_kospi_bas_dd ON financeHub.kospi_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_kosdaq_bas_dd ON financeHub.kosdaq_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_gold_bas_dd ON financeHub.gold_market_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_oil_bas_dd ON financeHub.oil_market_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_stock_bas_dd ON financeHub.stock_daily_trading(bas_dd);
CREATE INDEX IF NOT EXISTS idx_stock_fluc_rt ON financeHub.stock_daily_trading(bas_dd, fluc_rt);
CREATE INDEX IF NOT EXISTS idx_stock_acc_trdvol ON financeHub.stock_daily_trading(bas_dd, acc_trdvol);
CREATE INDEX IF NOT EXISTS idx_news_published_at ON financeHub.news(published_at);
CREATE INDEX IF NOT EXISTS idx_scheduler_log_job_name ON financeHub.scheduler_execution_log(job_name);
CREATE INDEX IF NOT EXISTS idx_scheduler_log_execution_time ON financeHub.scheduler_execution_log(execution_time);
