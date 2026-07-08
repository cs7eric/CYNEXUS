CREATE SCHEMA IF NOT EXISTS cynexus_appl_market;

ALTER TABLE cynexus_appl_market.sec_stock_basic
    ADD COLUMN IF NOT EXISTS total_shares numeric(24, 4),
    ADD COLUMN IF NOT EXISTS float_shares numeric(24, 4),
    ADD COLUMN IF NOT EXISTS list_date date,
    ADD COLUMN IF NOT EXISTS eastmoney_price numeric(18, 4),
    ADD COLUMN IF NOT EXISTS eastmoney_float_market_cap numeric(24, 4),
    ADD COLUMN IF NOT EXISTS eastmoney_updated_at timestamptz;

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_research_report (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    scope text NOT NULL,
    code text NOT NULL DEFAULT '',
    industry_code text NOT NULL DEFAULT '',
    industry_name text,
    report_id text NOT NULL,
    title text NOT NULL DEFAULT '',
    org_name text,
    author text,
    publish_date date,
    rating text,
    rating_change text,
    pdf_url text,
    url text,
    metrics jsonb NOT NULL DEFAULT '{}'::jsonb,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_research_report
    ON cynexus_appl_market.a_stock_research_report (source, scope, code, industry_code, report_id);
CREATE INDEX IF NOT EXISTS idx_a_stock_research_report_code_date
    ON cynexus_appl_market.a_stock_research_report (code, publish_date DESC);
CREATE INDEX IF NOT EXISTS idx_a_stock_research_report_industry_date
    ON cynexus_appl_market.a_stock_research_report (industry_code, publish_date DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_financial_statement (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    report_type text NOT NULL,
    report_date date NOT NULL,
    fields jsonb NOT NULL DEFAULT '{}'::jsonb,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_financial_statement
    ON cynexus_appl_market.a_stock_financial_statement (source, code, report_type, report_date);
CREATE INDEX IF NOT EXISTS idx_a_stock_financial_statement_code_date
    ON cynexus_appl_market.a_stock_financial_statement (code, report_date DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_announcement (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL DEFAULT '',
    announcement_id text NOT NULL,
    title text NOT NULL DEFAULT '',
    category text,
    publish_date date,
    url text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_announcement
    ON cynexus_appl_market.a_stock_announcement (source, code, announcement_id);
CREATE INDEX IF NOT EXISTS idx_a_stock_announcement_code_date
    ON cynexus_appl_market.a_stock_announcement (code, publish_date DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_news (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL DEFAULT '',
    news_id text NOT NULL,
    title text NOT NULL DEFAULT '',
    summary text,
    media_name text,
    published_at timestamptz,
    url text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_news
    ON cynexus_appl_market.a_stock_news (source, code, news_id);
CREATE INDEX IF NOT EXISTS idx_a_stock_news_code_time
    ON cynexus_appl_market.a_stock_news (code, published_at DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_investor_interaction (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    interaction_id text NOT NULL,
    company_name text,
    question text NOT NULL DEFAULT '',
    answer text,
    answerer text,
    asked_at timestamptz,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_investor_interaction
    ON cynexus_appl_market.a_stock_investor_interaction (source, code, interaction_id);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_fund_flow_daily (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    trade_date date NOT NULL,
    main_net numeric(24, 4),
    small_net numeric(24, 4),
    mid_net numeric(24, 4),
    large_net numeric(24, 4),
    super_net numeric(24, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_fund_flow_daily
    ON cynexus_appl_market.a_stock_fund_flow_daily (source, code, trade_date);
CREATE INDEX IF NOT EXISTS idx_a_stock_fund_flow_daily_code_date
    ON cynexus_appl_market.a_stock_fund_flow_daily (code, trade_date DESC);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_industry_rank_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    rank_direction text NOT NULL DEFAULT 'top',
    rank_no integer NOT NULL,
    industry_code text NOT NULL DEFAULT '',
    industry_name text NOT NULL DEFAULT '',
    change_pct numeric(12, 4),
    up_count integer,
    down_count integer,
    leader_name text,
    leader_change_pct numeric(12, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_industry_rank_snapshot
    ON cynexus_appl_market.a_stock_industry_rank_snapshot (source, snapshot_date, rank_direction, rank_no, industry_code);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_dragon_tiger_daily (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    trade_date date NOT NULL,
    code text NOT NULL,
    name text,
    reason text NOT NULL DEFAULT '',
    close_price numeric(18, 4),
    change_pct numeric(12, 4),
    net_buy_wan numeric(24, 4),
    buy_wan numeric(24, 4),
    sell_wan numeric(24, 4),
    turnover_pct numeric(12, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_dragon_tiger_daily
    ON cynexus_appl_market.a_stock_dragon_tiger_daily (source, trade_date, code, reason);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_dragon_tiger_seat (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    trade_date date NOT NULL,
    code text NOT NULL,
    side text NOT NULL,
    seat_rank integer NOT NULL,
    seat_name text NOT NULL DEFAULT '',
    buy_wan numeric(24, 4),
    sell_wan numeric(24, 4),
    net_wan numeric(24, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_dragon_tiger_seat
    ON cynexus_appl_market.a_stock_dragon_tiger_seat (source, trade_date, code, side, seat_rank, seat_name);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_lockup_expiry (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    free_date date NOT NULL,
    lockup_type text NOT NULL DEFAULT '',
    shares numeric(24, 4),
    ratio numeric(12, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_lockup_expiry
    ON cynexus_appl_market.a_stock_lockup_expiry (source, code, free_date, lockup_type);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_block_trade (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    trade_date date NOT NULL,
    price numeric(18, 4),
    close_price numeric(18, 4),
    premium_pct numeric(12, 4),
    volume numeric(24, 4),
    amount numeric(24, 4),
    buyer text NOT NULL DEFAULT '',
    seller text NOT NULL DEFAULT '',
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_block_trade
    ON cynexus_appl_market.a_stock_block_trade (source, code, trade_date, price, buyer, seller);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_holder_num (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    end_date date NOT NULL,
    holder_num integer,
    change_num integer,
    change_ratio numeric(12, 4),
    avg_shares numeric(24, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_holder_num
    ON cynexus_appl_market.a_stock_holder_num (source, code, end_date);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_dividend (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    code text NOT NULL,
    ex_dividend_date date NOT NULL,
    bonus_rmb numeric(18, 6),
    transfer_ratio numeric(18, 6),
    bonus_ratio numeric(18, 6),
    plan text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_dividend
    ON cynexus_appl_market.a_stock_dividend (source, code, ex_dividend_date, plan);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_hot_rank_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    list_type text NOT NULL DEFAULT '',
    rank_no integer NOT NULL,
    code text NOT NULL,
    name text,
    heat numeric(24, 4),
    price numeric(18, 4),
    pct numeric(12, 4),
    rank_change numeric(12, 4),
    concepts jsonb NOT NULL DEFAULT '[]'::jsonb,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_hot_rank_snapshot
    ON cynexus_appl_market.a_stock_hot_rank_snapshot (source, snapshot_date, list_type, rank_no, code);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_hot_concept_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    code text NOT NULL,
    concept_code text NOT NULL DEFAULT '',
    concept_name text NOT NULL DEFAULT '',
    hit_count integer,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_hot_concept_snapshot
    ON cynexus_appl_market.a_stock_hot_concept_snapshot (source, snapshot_date, code, concept_code, concept_name);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_hot_reason_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    code text NOT NULL,
    name text,
    reason_key text NOT NULL DEFAULT '',
    reason text,
    subject text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_hot_reason_snapshot
    ON cynexus_appl_market.a_stock_hot_reason_snapshot (source, snapshot_date, code, reason_key);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_limit_pool_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    pool text NOT NULL,
    code text NOT NULL,
    name text,
    price numeric(18, 4),
    pct numeric(12, 4),
    reason text,
    board_type text,
    limit_days text,
    first_time text,
    last_time text,
    break_times integer,
    seal_amount numeric(24, 4),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_limit_pool_snapshot
    ON cynexus_appl_market.a_stock_limit_pool_snapshot (source, snapshot_date, pool, code);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_option_contract_snapshot (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    underlying text NOT NULL,
    option_month text NOT NULL,
    option_type text NOT NULL,
    option_code text NOT NULL,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_option_contract_snapshot
    ON cynexus_appl_market.a_stock_option_contract_snapshot (source, snapshot_date, underlying, option_month, option_type, option_code);

CREATE TABLE IF NOT EXISTS cynexus_appl_market.a_stock_concept_membership (
    id bigint PRIMARY KEY,
    source text NOT NULL,
    snapshot_date date NOT NULL,
    code text NOT NULL,
    concept_code text NOT NULL DEFAULT '',
    concept_name text NOT NULL DEFAULT '',
    change_pct numeric(12, 4),
    lead_stock text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_a_stock_concept_membership
    ON cynexus_appl_market.a_stock_concept_membership (source, snapshot_date, code, concept_code, concept_name);
