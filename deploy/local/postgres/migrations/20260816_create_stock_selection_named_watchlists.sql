CREATE TABLE IF NOT EXISTS cynexus_appl_market.stock_selection_watchlist (
    id bigint PRIMARY KEY,
    tenant_id varchar(128) NOT NULL,
    owner_id varchar(128) NOT NULL,
    name varchar(128) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_stock_selection_watchlist_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_selection_watchlist_owner_name_active
    ON cynexus_appl_market.stock_selection_watchlist (tenant_id, owner_id, lower(name))
    WHERE deleted_at IS NULL AND status = 'ACTIVE';

ALTER TABLE cynexus_appl_market.stock_selection_watchlist_item
    ADD COLUMN IF NOT EXISTS watchlist_id bigint,
    ADD COLUMN IF NOT EXISTS market varchar(8),
    ADD COLUMN IF NOT EXISTS entry_trade_date date,
    ADD COLUMN IF NOT EXISTS entry_price numeric(18, 6);

INSERT INTO cynexus_appl_market.stock_selection_watchlist
    (id, tenant_id, owner_id, name, status, version)
SELECT 920000000000000000 + row_number() OVER (ORDER BY tenant_id, owner_id), tenant_id, owner_id, 'My watchlist', 'ACTIVE', 0
  FROM (SELECT DISTINCT tenant_id, owner_id FROM cynexus_appl_market.stock_selection_watchlist_item) legacy
ON CONFLICT DO NOTHING;

UPDATE cynexus_appl_market.stock_selection_watchlist_item item
   SET watchlist_id = list.id
  FROM cynexus_appl_market.stock_selection_watchlist list
 WHERE item.watchlist_id IS NULL
   AND list.tenant_id = item.tenant_id
   AND list.owner_id = item.owner_id
   AND lower(list.name) = lower('My watchlist')
   AND list.deleted_at IS NULL;

ALTER TABLE cynexus_appl_market.stock_selection_watchlist_item
    DROP CONSTRAINT IF EXISTS fk_stock_selection_watchlist_item_parent;
ALTER TABLE cynexus_appl_market.stock_selection_watchlist_item
    ADD CONSTRAINT fk_stock_selection_watchlist_item_parent
    FOREIGN KEY (watchlist_id) REFERENCES cynexus_appl_market.stock_selection_watchlist(id);

DROP INDEX IF EXISTS cynexus_appl_market.uk_stock_selection_watchlist_active_owner_stock;
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_selection_watchlist_active_list_stock
    ON cynexus_appl_market.stock_selection_watchlist_item (watchlist_id, stock_code)
    WHERE deleted_at IS NULL AND status IN ('WATCHING', 'PAUSED');
CREATE INDEX IF NOT EXISTS idx_stock_selection_watchlist_item_list_status
    ON cynexus_appl_market.stock_selection_watchlist_item (watchlist_id, status, updated_at DESC)
    WHERE deleted_at IS NULL;
