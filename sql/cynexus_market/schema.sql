CREATE TABLE IF NOT EXISTS market_overview_snapshot (
    id BIGINT PRIMARY KEY,
    trade_date DATE NOT NULL,
    index_change_percent NUMERIC(8, 4),
    turnover_amount NUMERIC(20, 4),
    northbound_net_inflow NUMERIC(20, 4),
    rise_count INTEGER,
    fall_count INTEGER,
    summary TEXT,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_market_overview_trade_date UNIQUE (trade_date)
);

CREATE TABLE IF NOT EXISTS market_sector_rank_snapshot (
    id BIGINT PRIMARY KEY,
    trade_date DATE NOT NULL,
    sector_code VARCHAR(64) NOT NULL,
    sector_name VARCHAR(128) NOT NULL,
    change_percent NUMERIC(8, 4),
    turnover_amount NUMERIC(20, 4),
    main_net_inflow NUMERIC(20, 4),
    rank_no INTEGER,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_market_sector_rank UNIQUE (trade_date, sector_code)
);

CREATE TABLE IF NOT EXISTS stock_daily_feature_snapshot (
    id BIGINT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    close_price NUMERIC(20, 4),
    change_percent NUMERIC(8, 4),
    turnover_rate NUMERIC(8, 4),
    volume_ratio NUMERIC(8, 4),
    main_net_inflow NUMERIC(20, 4),
    trend_label VARCHAR(64),
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_daily_feature UNIQUE (symbol, trade_date)
);

CREATE TABLE IF NOT EXISTS stock_intraday_feature_snapshot (
    id BIGINT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    latest_price NUMERIC(20, 4),
    amplitude NUMERIC(8, 4),
    volume_acceleration NUMERIC(8, 4),
    main_net_inflow NUMERIC(20, 4),
    intraday_label VARCHAR(64),
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_intraday_feature UNIQUE (symbol, trade_date)
);
