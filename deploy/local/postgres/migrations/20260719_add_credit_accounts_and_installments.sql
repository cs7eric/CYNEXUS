\set ON_ERROR_STOP on

BEGIN;

ALTER TABLE cynexus_appl_core.finance_accounts
    DROP CONSTRAINT IF EXISTS ck_finance_account_type,
    DROP CONSTRAINT IF EXISTS ck_finance_account_nature;

UPDATE cynexus_appl_core.finance_accounts
   SET account_type = 'credit_card', updated_at = now()
 WHERE account_type = 'credit';

UPDATE cynexus_appl_core.finance_accounts
   SET nature = 'credit', updated_at = now()
 WHERE nature = 'liability';

ALTER TABLE cynexus_appl_core.finance_accounts
    ADD CONSTRAINT ck_finance_account_type CHECK (account_type IN ('cash', 'bank', 'e_wallet', 'huabei', 'baitiao', 'douyin_monthly', 'credit_card', 'investment', 'other')),
    ADD CONSTRAINT ck_finance_account_nature CHECK (nature IN ('asset', 'credit'));

ALTER TABLE cynexus_appl_core.finance_ai_capture_items
    ADD COLUMN IF NOT EXISTS installment_count smallint,
    ADD COLUMN IF NOT EXISTS installment_fee numeric(20, 2);

ALTER TABLE cynexus_appl_core.finance_ai_capture_items
    DROP CONSTRAINT IF EXISTS ck_finance_ai_item_installment_count,
    DROP CONSTRAINT IF EXISTS ck_finance_ai_item_installment_fee;

ALTER TABLE cynexus_appl_core.finance_ai_capture_items
    ADD CONSTRAINT ck_finance_ai_item_installment_count CHECK (installment_count IS NULL OR installment_count BETWEEN 2 AND 60),
    ADD CONSTRAINT ck_finance_ai_item_installment_fee CHECK (installment_fee IS NULL OR installment_fee >= 0);

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_credit_installment_plans (
    id bigint PRIMARY KEY,
    owner_key varchar(128) NOT NULL DEFAULT 'default',
    account_id bigint NOT NULL REFERENCES cynexus_appl_core.finance_accounts(id),
    purchase_transaction_id bigint REFERENCES cynexus_appl_core.finance_transactions(id),
    merchant varchar(256),
    total_amount numeric(20, 2) NOT NULL,
    fee_total numeric(20, 2) NOT NULL DEFAULT 0,
    installment_count smallint NOT NULL,
    first_due_date date NOT NULL,
    interval_months smallint NOT NULL DEFAULT 1,
    status varchar(16) NOT NULL DEFAULT 'active',
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_installment_plan_total CHECK (total_amount > 0),
    CONSTRAINT ck_finance_installment_plan_fee CHECK (fee_total >= 0),
    CONSTRAINT ck_finance_installment_plan_count CHECK (installment_count BETWEEN 2 AND 60),
    CONSTRAINT ck_finance_installment_plan_interval CHECK (interval_months BETWEEN 1 AND 12),
    CONSTRAINT ck_finance_installment_plan_status CHECK (status IN ('active', 'completed', 'cancelled'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_installment_plan_purchase
    ON cynexus_appl_core.finance_credit_installment_plans (owner_key, purchase_transaction_id)
    WHERE deleted_at IS NULL AND purchase_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_finance_installment_plans_owner_due
    ON cynexus_appl_core.finance_credit_installment_plans (owner_key, status, first_due_date)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_core.finance_credit_installments (
    id bigint PRIMARY KEY,
    plan_id bigint NOT NULL REFERENCES cynexus_appl_core.finance_credit_installment_plans(id) ON DELETE CASCADE,
    installment_no smallint NOT NULL,
    due_date date NOT NULL,
    principal_amount numeric(20, 2) NOT NULL,
    fee_amount numeric(20, 2) NOT NULL DEFAULT 0,
    amount numeric(20, 2) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'pending',
    paid_at timestamptz,
    payment_transaction_id bigint REFERENCES cynexus_appl_core.finance_transactions(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_finance_installment_principal CHECK (principal_amount > 0),
    CONSTRAINT ck_finance_installment_fee CHECK (fee_amount >= 0),
    CONSTRAINT ck_finance_installment_amount CHECK (amount > 0),
    CONSTRAINT ck_finance_installment_status CHECK (status IN ('pending', 'paid', 'cancelled'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_installment_plan_no
    ON cynexus_appl_core.finance_credit_installments (plan_id, installment_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_finance_installments_due
    ON cynexus_appl_core.finance_credit_installments (due_date, status)
    WHERE deleted_at IS NULL;

COMMIT;
