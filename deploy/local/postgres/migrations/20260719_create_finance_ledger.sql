\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS cynexus_appl_core;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_accounts (
    id bigint PRIMARY KEY,
    owner_key varchar(128) NOT NULL DEFAULT 'default',
    name varchar(128) NOT NULL,
    account_type varchar(32) NOT NULL,
    nature varchar(16) NOT NULL,
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    opening_balance numeric(20, 2) NOT NULL DEFAULT 0,
    credit_limit numeric(20, 2),
    billing_day smallint,
    due_day smallint,
    color varchar(32),
    notes text,
    is_archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_account_type CHECK (account_type IN ('cash', 'bank', 'e_wallet', 'credit', 'investment', 'other')),
    CONSTRAINT ck_finance_account_nature CHECK (nature IN ('asset', 'liability')),
    CONSTRAINT ck_finance_account_opening_balance CHECK (opening_balance >= 0),
    CONSTRAINT ck_finance_account_credit_limit CHECK (credit_limit IS NULL OR credit_limit >= 0),
    CONSTRAINT ck_finance_account_billing_day CHECK (billing_day IS NULL OR billing_day BETWEEN 1 AND 31),
    CONSTRAINT ck_finance_account_due_day CHECK (due_day IS NULL OR due_day BETWEEN 1 AND 31)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_account_owner_name_active
    ON cynexus_appl_core.finance_accounts (owner_key, lower(name))
    WHERE deleted_at IS NULL AND is_archived = false;

CREATE INDEX IF NOT EXISTS ix_finance_accounts_owner_active
    ON cynexus_appl_core.finance_accounts (owner_key, nature, is_archived, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_categories (
    id bigint PRIMARY KEY,
    owner_key varchar(128) NOT NULL DEFAULT 'default',
    parent_id bigint,
    name varchar(96) NOT NULL,
    direction varchar(16) NOT NULL,
    icon varchar(64),
    color varchar(32),
    sort_order integer NOT NULL DEFAULT 0,
    is_system boolean NOT NULL DEFAULT false,
    is_archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_category_direction CHECK (direction IN ('expense', 'income', 'both'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_category_owner_parent_name_active
    ON cynexus_appl_core.finance_categories (owner_key, coalesce(parent_id, 0), lower(name), direction)
    WHERE deleted_at IS NULL AND is_archived = false;

CREATE INDEX IF NOT EXISTS ix_finance_categories_owner_active
    ON cynexus_appl_core.finance_categories (owner_key, direction, sort_order, name)
    WHERE deleted_at IS NULL AND is_archived = false;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_transactions (
    id bigint PRIMARY KEY,
    owner_key varchar(128) NOT NULL DEFAULT 'default',
    transaction_type varchar(16) NOT NULL,
    account_id bigint NOT NULL REFERENCES cynexus_appl_core.finance_accounts(id),
    to_account_id bigint REFERENCES cynexus_appl_core.finance_accounts(id),
    category_id bigint REFERENCES cynexus_appl_core.finance_categories(id),
    amount numeric(20, 2) NOT NULL,
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    occurred_at timestamptz NOT NULL,
    merchant varchar(256),
    note text,
    source varchar(32) NOT NULL DEFAULT 'manual',
    source_ref varchar(128),
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    is_archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_transaction_type CHECK (transaction_type IN ('expense', 'income', 'transfer', 'adjustment')),
    CONSTRAINT ck_finance_transaction_amount CHECK (amount > 0),
    CONSTRAINT ck_finance_transaction_source CHECK (source IN ('manual', 'ai_capture', 'import', 'system')),
    CONSTRAINT ck_finance_transaction_transfer CHECK (
        (transaction_type = 'transfer' AND to_account_id IS NOT NULL AND to_account_id <> account_id AND category_id IS NULL)
        OR (transaction_type <> 'transfer')
    )
);

CREATE INDEX IF NOT EXISTS ix_finance_transactions_owner_occurred
    ON cynexus_appl_core.finance_transactions (owner_key, occurred_at DESC, id DESC)
    WHERE deleted_at IS NULL AND is_archived = false;

CREATE INDEX IF NOT EXISTS ix_finance_transactions_owner_category
    ON cynexus_appl_core.finance_transactions (owner_key, category_id, occurred_at DESC)
    WHERE deleted_at IS NULL AND is_archived = false;

CREATE INDEX IF NOT EXISTS ix_finance_transactions_owner_account
    ON cynexus_appl_core.finance_transactions (owner_key, account_id, occurred_at DESC)
    WHERE deleted_at IS NULL AND is_archived = false;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_ai_captures (
    id bigint PRIMARY KEY,
    owner_key varchar(128) NOT NULL DEFAULT 'default',
    status varchar(24) NOT NULL DEFAULT 'uploaded',
    source_count integer NOT NULL DEFAULT 0,
    model_code varchar(128),
    raw_response jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_ai_capture_status CHECK (status IN ('uploaded', 'processing', 'review', 'confirmed', 'failed', 'archived'))
);

CREATE INDEX IF NOT EXISTS ix_finance_ai_captures_owner_created
    ON cynexus_appl_core.finance_ai_captures (owner_key, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_ai_capture_images (
    id bigint PRIMARY KEY,
    capture_id bigint NOT NULL REFERENCES cynexus_appl_core.finance_ai_captures(id) ON DELETE CASCADE,
    image_index integer NOT NULL,
    file_name varchar(512),
    content_type varchar(128) NOT NULL,
    image_size bigint NOT NULL,
    content_sha256 varchar(64) NOT NULL,
    image_data bytea NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_ai_capture_image_size CHECK (image_size > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_ai_capture_image_index
    ON cynexus_appl_core.finance_ai_capture_images (capture_id, image_index)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_ai_capture_items (
    id bigint PRIMARY KEY,
    capture_id bigint NOT NULL REFERENCES cynexus_appl_core.finance_ai_captures(id) ON DELETE CASCADE,
    image_id bigint REFERENCES cynexus_appl_core.finance_ai_capture_images(id),
    item_index integer NOT NULL DEFAULT 0,
    transaction_type varchar(16) NOT NULL DEFAULT 'expense',
    occurred_at timestamptz,
    amount numeric(20, 2),
    currency varchar(8) NOT NULL DEFAULT 'CNY',
    merchant varchar(256),
    category_hint varchar(96),
    category_id bigint REFERENCES cynexus_appl_core.finance_categories(id),
    account_id bigint REFERENCES cynexus_appl_core.finance_accounts(id),
    note text,
    confidence numeric(5, 4),
    raw_text text,
    raw_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(16) NOT NULL DEFAULT 'draft',
    transaction_id bigint REFERENCES cynexus_appl_core.finance_transactions(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_ai_item_type CHECK (transaction_type IN ('expense', 'income')),
    CONSTRAINT ck_finance_ai_item_amount CHECK (amount IS NULL OR amount > 0),
    CONSTRAINT ck_finance_ai_item_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    CONSTRAINT ck_finance_ai_item_status CHECK (status IN ('draft', 'confirmed', 'rejected'))
);

CREATE INDEX IF NOT EXISTS ix_finance_ai_capture_items_capture
    ON cynexus_appl_core.finance_ai_capture_items (capture_id, item_index)
    WHERE deleted_at IS NULL;

INSERT INTO cynexus_appl_core.finance_categories
    (id, owner_key, parent_id, name, direction, icon, color, sort_order, is_system)
VALUES
    (920010000000000001, 'default', NULL, '餐饮', 'expense', 'utensils', 'orange', 10, true),
    (920010000000000002, 'default', NULL, '交通', 'expense', 'car', 'blue', 20, true),
    (920010000000000003, 'default', NULL, '购物', 'expense', 'shopping-bag', 'pink', 30, true),
    (920010000000000004, 'default', NULL, '居住', 'expense', 'home', 'violet', 40, true),
    (920010000000000005, 'default', NULL, '娱乐', 'expense', 'gamepad-2', 'cyan', 50, true),
    (920010000000000006, 'default', NULL, '医疗', 'expense', 'heart-pulse', 'rose', 60, true),
    (920010000000000007, 'default', NULL, '教育', 'expense', 'book-open', 'indigo', 70, true),
    (920010000000000008, 'default', NULL, '通讯', 'expense', 'smartphone', 'slate', 80, true),
    (920010000000000009, 'default', NULL, '其他支出', 'expense', 'ellipsis', 'slate', 90, true),
    (920010000000000011, 'default', NULL, '工资', 'income', 'briefcase-business', 'emerald', 10, true),
    (920010000000000012, 'default', NULL, '奖金', 'income', 'sparkles', 'amber', 20, true),
    (920010000000000013, 'default', NULL, '投资收益', 'income', 'trending-up', 'teal', 30, true),
    (920010000000000014, 'default', NULL, '其他收入', 'income', 'plus-circle', 'emerald', 90, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    direction = EXCLUDED.direction,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    updated_at = now(),
    deleted_at = NULL,
    is_archived = false;

COMMIT;
