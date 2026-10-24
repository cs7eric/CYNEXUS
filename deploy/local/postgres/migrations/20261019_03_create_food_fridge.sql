\set ON_ERROR_STOP on

-- Fridge inventory is deliberately lot based. A single food may have several
-- best-before dates and must remain traceable when it is consumed by the diary.
CREATE SCHEMA IF NOT EXISTS cynexus_appl_life;

-- A confirmed name/brand alias is reused on the next add, while the original
-- display name remains stored on each fridge item for auditability.
CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_fridge_mapping (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    normalized_name text NOT NULL CHECK (btrim(normalized_name) <> ''),
    brand text,
    catalog_food_id bigint REFERENCES cynexus_appl_life.food_nutrition_catalog(id),
    custom_food_id bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_fridge_mapping_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_fridge_mapping_identity UNIQUE (owner_key, normalized_name, brand),
    CONSTRAINT fk_food_fridge_mapping_custom FOREIGN KEY (owner_key, custom_food_id)
        REFERENCES cynexus_appl_life.food_custom (owner_key, id),
    CONSTRAINT ck_food_fridge_mapping_target CHECK (num_nonnulls(catalog_food_id, custom_food_id) = 1)
);
CREATE INDEX IF NOT EXISTS idx_food_fridge_mapping_name
    ON cynexus_appl_life.food_fridge_mapping (owner_key, normalized_name)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_fridge_item (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    catalog_food_id bigint REFERENCES cynexus_appl_life.food_nutrition_catalog(id),
    custom_food_id bigint,
    name text NOT NULL CHECK (btrim(name) <> ''),
    brand text,
    normalized_name text NOT NULL CHECK (btrim(normalized_name) <> ''),
    category text NOT NULL DEFAULT 'other'
        CHECK (category IN ('vegetable', 'fruit', 'meat', 'seafood', 'dairy', 'grain', 'beverage', 'other')),
    mapping_status text NOT NULL DEFAULT 'pending'
        CHECK (mapping_status IN ('pending', 'matched', 'user_confirmed', 'unmatched')),
    mapping_confidence numeric,
    mapping_candidates jsonb NOT NULL DEFAULT '[]'::jsonb
        CHECK (jsonb_typeof(mapping_candidates) = 'array'),
    unit_policy jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(unit_policy) = 'object'),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(metadata) = 'object'),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_fridge_item_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_fridge_item_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_fridge_item_custom_food FOREIGN KEY (owner_key, custom_food_id)
        REFERENCES cynexus_appl_life.food_custom (owner_key, id),
    CONSTRAINT ck_fridge_item_mapping_target CHECK (
        (mapping_status IN ('matched', 'user_confirmed') AND num_nonnulls(catalog_food_id, custom_food_id) = 1)
        OR (mapping_status IN ('pending', 'unmatched') AND num_nonnulls(catalog_food_id, custom_food_id) = 0)
    ),
    CONSTRAINT ck_fridge_item_mapping_confidence CHECK (
        mapping_confidence IS NULL OR (mapping_confidence >= 0 AND mapping_confidence <= 1)
    )
);
ALTER TABLE cynexus_appl_life.food_fridge_item
    ADD COLUMN IF NOT EXISTS brand text;

CREATE INDEX IF NOT EXISTS idx_fridge_item_owner_name_alive
    ON cynexus_appl_life.food_fridge_item (owner_key, normalized_name, id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_fridge_item_catalog_alive
    ON cynexus_appl_life.food_fridge_item (catalog_food_id)
    WHERE deleted_at IS NULL AND catalog_food_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fridge_item_mapping_status_alive
    ON cynexus_appl_life.food_fridge_item (owner_key, mapping_status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_fridge_stock_lot (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    fridge_item_id bigint NOT NULL,
    quantity_remaining numeric NOT NULL CHECK (quantity_remaining >= 0 AND quantity_remaining < 'Infinity'::numeric),
    unit text NOT NULL CHECK (unit IN ('mg', 'g', 'kg', 'ml', 'L', 'pcs', 'pack', 'head', 'bunch', 'bottle', 'carton')),
    grams_per_unit numeric,
    density_g_per_ml numeric,
    quantity_grams numeric GENERATED ALWAYS AS (
        CASE unit
            WHEN 'mg' THEN quantity_remaining / 1000
            WHEN 'g' THEN quantity_remaining
            WHEN 'kg' THEN quantity_remaining * 1000
            WHEN 'ml' THEN CASE WHEN density_g_per_ml IS NULL THEN NULL ELSE quantity_remaining * density_g_per_ml END
            WHEN 'L' THEN CASE WHEN density_g_per_ml IS NULL THEN NULL ELSE quantity_remaining * 1000 * density_g_per_ml END
            ELSE CASE WHEN grams_per_unit IS NULL THEN NULL ELSE quantity_remaining * grams_per_unit END
        END
    ) STORED,
    best_before date NOT NULL,
    opened_at timestamptz,
    storage_zone text NOT NULL DEFAULT 'middle'
        CHECK (storage_zone IN ('top', 'middle', 'lower', 'bottom', 'drawer', 'door', 'freezer', 'other')),
    status text NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'depleted', 'discarded')),
    nutrition_status text NOT NULL DEFAULT 'pending'
        CHECK (nutrition_status IN ('known', 'partial', 'unknown', 'pending')),
    calories_per100g numeric,
    carbs_per100g numeric,
    protein_per100g numeric,
    fat_per100g numeric,
    nutrition_source_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(nutrition_source_snapshot) = 'object'),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(metadata) = 'object'),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_fridge_stock_lot_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_fridge_stock_lot_owner_item UNIQUE (owner_key, id, fridge_item_id),
    CONSTRAINT uk_fridge_stock_lot_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_fridge_stock_lot_item FOREIGN KEY (owner_key, fridge_item_id)
        REFERENCES cynexus_appl_life.food_fridge_item (owner_key, id),
    CONSTRAINT ck_fridge_stock_lot_unit_conversion CHECK (
        ((unit IN ('mg', 'g', 'kg') AND density_g_per_ml IS NULL)
            OR (unit IN ('ml', 'L') AND density_g_per_ml > 0 AND density_g_per_ml < 'Infinity'::numeric
                AND grams_per_unit IS NULL)
            OR (unit IN ('pcs', 'pack', 'head', 'bunch', 'bottle', 'carton')
                AND density_g_per_ml IS NULL
                AND (grams_per_unit IS NULL OR (grams_per_unit > 0 AND grams_per_unit < 'Infinity'::numeric))))
    ),
    CONSTRAINT ck_fridge_stock_lot_quantity_grams CHECK (
        quantity_grams IS NULL OR (quantity_grams >= 0 AND quantity_grams < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_fridge_stock_lot_status_quantity CHECK (
        (status = 'active' AND quantity_remaining > 0)
        OR (status IN ('depleted', 'discarded') AND quantity_remaining = 0)
    ),
    CONSTRAINT ck_fridge_stock_lot_nutrition_status CHECK (
        (nutrition_status = 'known'
            AND calories_per100g IS NOT NULL AND carbs_per100g IS NOT NULL
            AND protein_per100g IS NOT NULL AND fat_per100g IS NOT NULL)
        OR (nutrition_status = 'partial'
            AND num_nonnulls(calories_per100g, carbs_per100g, protein_per100g, fat_per100g) BETWEEN 1 AND 3)
        OR (nutrition_status IN ('unknown', 'pending')
            AND num_nonnulls(calories_per100g, carbs_per100g, protein_per100g, fat_per100g) = 0)
    ),
    CONSTRAINT ck_fridge_stock_lot_calories CHECK (
        calories_per100g IS NULL OR (calories_per100g >= 0 AND calories_per100g < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_fridge_stock_lot_carbs CHECK (
        carbs_per100g IS NULL OR (carbs_per100g >= 0 AND carbs_per100g < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_fridge_stock_lot_protein CHECK (
        protein_per100g IS NULL OR (protein_per100g >= 0 AND protein_per100g < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_fridge_stock_lot_fat CHECK (
        fat_per100g IS NULL OR (fat_per100g >= 0 AND fat_per100g < 'Infinity'::numeric)
    )
);

CREATE INDEX IF NOT EXISTS idx_fridge_stock_lot_item_active
    ON cynexus_appl_life.food_fridge_stock_lot (owner_key, fridge_item_id, best_before, id)
    WHERE deleted_at IS NULL AND status = 'active' AND quantity_remaining > 0;
CREATE INDEX IF NOT EXISTS idx_fridge_stock_lot_expiry_active
    ON cynexus_appl_life.food_fridge_stock_lot (owner_key, best_before, id)
    WHERE deleted_at IS NULL AND status = 'active' AND quantity_remaining > 0;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_fridge_stock_event (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    fridge_item_id bigint NOT NULL,
    lot_id bigint NOT NULL,
    event_kind text NOT NULL
        CHECK (event_kind IN ('add', 'remove', 'adjust', 'consume', 'discard')),
    quantity_delta numeric NOT NULL CHECK (quantity_delta <> 0 AND quantity_delta > '-Infinity'::numeric AND quantity_delta < 'Infinity'::numeric),
    unit text NOT NULL CHECK (unit IN ('mg', 'g', 'kg', 'ml', 'L', 'pcs', 'pack', 'head', 'bunch', 'bottle', 'carton')),
    grams_delta numeric,
    quantity_before numeric NOT NULL CHECK (quantity_before >= 0 AND quantity_before < 'Infinity'::numeric),
    quantity_after numeric NOT NULL CHECK (quantity_after >= 0 AND quantity_after < 'Infinity'::numeric),
    source text NOT NULL DEFAULT 'user' CHECK (btrim(source) <> ''),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_fridge_stock_event_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_fridge_stock_event_owner_lot UNIQUE (owner_key, id, lot_id),
    CONSTRAINT uk_fridge_stock_event_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_fridge_stock_event_item FOREIGN KEY (owner_key, fridge_item_id)
        REFERENCES cynexus_appl_life.food_fridge_item (owner_key, id),
    CONSTRAINT fk_fridge_stock_event_lot FOREIGN KEY (owner_key, lot_id)
        REFERENCES cynexus_appl_life.food_fridge_stock_lot (owner_key, id),
    CONSTRAINT fk_fridge_stock_event_lot_item FOREIGN KEY (owner_key, lot_id, fridge_item_id)
        REFERENCES cynexus_appl_life.food_fridge_stock_lot (owner_key, id, fridge_item_id),
    CONSTRAINT ck_fridge_stock_event_kind_delta CHECK (
        (event_kind = 'add' AND quantity_delta > 0)
        OR (event_kind IN ('remove', 'consume', 'discard') AND quantity_delta < 0)
        OR (event_kind = 'adjust')
    ),
    CONSTRAINT ck_fridge_stock_event_grams CHECK (
        grams_delta IS NULL OR (grams_delta <> 0 AND grams_delta > '-Infinity'::numeric AND grams_delta < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_fridge_stock_event_quantity_balance CHECK (
        quantity_after = quantity_before + quantity_delta
    )
);

CREATE INDEX IF NOT EXISTS idx_fridge_stock_event_lot_time
    ON cynexus_appl_life.food_fridge_stock_event (owner_key, lot_id, occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_fridge_stock_event_item_time
    ON cynexus_appl_life.food_fridge_stock_event (owner_key, fridge_item_id, occurred_at DESC, id DESC);

-- A meal may consume portions from more than one expiry lot. This allocation
-- table keeps that relationship explicit while food_meal_item retains its
-- immutable nutrition snapshot.
CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_meal_item_fridge_lot (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    meal_item_id bigint NOT NULL,
    lot_id bigint NOT NULL,
    event_id bigint NOT NULL,
    quantity numeric NOT NULL CHECK (quantity > 0 AND quantity < 'Infinity'::numeric),
    unit text NOT NULL CHECK (unit IN ('mg', 'g', 'kg', 'ml', 'L', 'pcs', 'pack', 'head', 'bunch', 'bottle', 'carton')),
    consumed_grams numeric,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_meal_item_fridge_lot_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_meal_item_fridge_lot_event_lot UNIQUE (owner_key, id, lot_id),
    CONSTRAINT uk_food_meal_item_fridge_lot_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_food_meal_item_fridge_lot_meal FOREIGN KEY (owner_key, meal_item_id)
        REFERENCES cynexus_appl_life.food_meal_item (owner_key, id),
    CONSTRAINT fk_food_meal_item_fridge_lot_lot FOREIGN KEY (owner_key, lot_id)
        REFERENCES cynexus_appl_life.food_fridge_stock_lot (owner_key, id),
    CONSTRAINT fk_food_meal_item_fridge_lot_event FOREIGN KEY (owner_key, event_id)
        REFERENCES cynexus_appl_life.food_fridge_stock_event (owner_key, id),
    CONSTRAINT fk_food_meal_item_fridge_lot_event_lot FOREIGN KEY (owner_key, event_id, lot_id)
        REFERENCES cynexus_appl_life.food_fridge_stock_event (owner_key, id, lot_id),
    CONSTRAINT ck_food_meal_item_fridge_lot_grams CHECK (
        consumed_grams IS NULL OR (consumed_grams > 0 AND consumed_grams < 'Infinity'::numeric)
    )
);

CREATE INDEX IF NOT EXISTS idx_food_meal_item_fridge_lot_meal
    ON cynexus_appl_life.food_meal_item_fridge_lot (owner_key, meal_item_id, id);
CREATE INDEX IF NOT EXISTS idx_food_meal_item_fridge_lot_lot
    ON cynexus_appl_life.food_meal_item_fridge_lot (owner_key, lot_id, id);

CREATE OR REPLACE VIEW cynexus_appl_life.food_fridge_inventory_current AS
WITH active_lots AS (
    SELECT l.*, i.name, i.normalized_name, i.category, i.mapping_status,
           i.catalog_food_id, i.custom_food_id
    FROM cynexus_appl_life.food_fridge_stock_lot l
    JOIN cynexus_appl_life.food_fridge_item i
      ON i.owner_key = l.owner_key AND i.id = l.fridge_item_id
    WHERE l.deleted_at IS NULL
      AND i.deleted_at IS NULL
      AND l.status = 'active'
      AND l.quantity_remaining > 0
), grouped AS (
    SELECT owner_key, fridge_item_id, name, normalized_name, category, mapping_status,
           catalog_food_id, custom_food_id,
           count(*)::integer AS active_lot_count,
           min(best_before) AS earliest_best_before,
           count(DISTINCT unit)::integer AS unit_count,
           min(unit) AS only_unit,
           sum(quantity_remaining) AS same_unit_quantity,
           bool_and(quantity_grams IS NOT NULL) AS all_lots_have_grams,
           sum(quantity_grams) AS total_quantity_grams,
           count(*) FILTER (WHERE quantity_grams IS NULL) AS missing_weight_count,
           count(*) FILTER (WHERE calories_per100g IS NULL) AS missing_calories_count,
           count(*) FILTER (WHERE carbs_per100g IS NULL) AS missing_carbs_count,
           count(*) FILTER (WHERE protein_per100g IS NULL) AS missing_protein_count,
           count(*) FILTER (WHERE fat_per100g IS NULL) AS missing_fat_count,
           sum(quantity_grams * calories_per100g / 100) FILTER (WHERE quantity_grams IS NOT NULL AND calories_per100g IS NOT NULL) AS known_calories_kcal,
           sum(quantity_grams * carbs_per100g / 100) FILTER (WHERE quantity_grams IS NOT NULL AND carbs_per100g IS NOT NULL) AS known_carbs_g,
           sum(quantity_grams * protein_per100g / 100) FILTER (WHERE quantity_grams IS NOT NULL AND protein_per100g IS NOT NULL) AS known_protein_g,
           sum(quantity_grams * fat_per100g / 100) FILTER (WHERE quantity_grams IS NOT NULL AND fat_per100g IS NOT NULL) AS known_fat_g,
           sum(CASE WHEN quantity_grams IS NOT NULL THEN quantity_grams ELSE quantity_remaining END) AS stack_basis
    FROM active_lots
    GROUP BY owner_key, fridge_item_id, name, normalized_name, category, mapping_status,
             catalog_food_id, custom_food_id
)
SELECT grouped.*,
       CASE WHEN unit_count = 1 THEN same_unit_quantity
            WHEN all_lots_have_grams THEN total_quantity_grams
            ELSE NULL END AS display_quantity,
       CASE WHEN unit_count = 1 THEN only_unit
            WHEN all_lots_have_grams THEN 'g'
            ELSE NULL END AS display_unit,
       (missing_weight_count = 0) AS weight_complete,
       (missing_calories_count = 0 AND missing_weight_count = 0) AS calories_complete,
       (missing_carbs_count = 0 AND missing_weight_count = 0) AS carbs_complete,
       (missing_protein_count = 0 AND missing_weight_count = 0) AS protein_complete,
       (missing_fat_count = 0 AND missing_weight_count = 0) AS fat_complete,
       LEAST(5, GREATEST(1, CEIL(stack_basis / 3.0)::integer)) AS visual_stack_count,
       (earliest_best_before < CURRENT_DATE) AS has_expired,
       (earliest_best_before BETWEEN CURRENT_DATE AND CURRENT_DATE + 3) AS use_soon
FROM grouped;

COMMENT ON TABLE cynexus_appl_life.food_fridge_item IS 'Owner food identity used by fridge inventory; mapping may remain pending until the user confirms a nutrition match.';
COMMENT ON COLUMN cynexus_appl_life.food_fridge_item.catalog_food_id IS 'Global shared nutrition catalog identity; null while mapping is pending or unmatched.';
COMMENT ON COLUMN cynexus_appl_life.food_fridge_item.custom_food_id IS 'Same-owner custom food identity; mutually exclusive with catalog_food_id.';
COMMENT ON COLUMN cynexus_appl_life.food_fridge_item.mapping_candidates IS 'Ordered candidate matches returned by automatic name/category mapping, retained for user confirmation.';
COMMENT ON COLUMN cynexus_appl_life.food_fridge_item.unit_policy IS 'Food-specific allowed units, default increment, precision, and measured conversions; no implicit density assumptions.';
COMMENT ON TABLE cynexus_appl_life.food_fridge_stock_lot IS 'A quantity batch with its own best-before date and nutrition conversion snapshot.';
COMMENT ON COLUMN cynexus_appl_life.food_fridge_stock_lot.quantity_grams IS 'Derived weight used for nutrition; null when a count or volume conversion is not known.';
COMMENT ON COLUMN cynexus_appl_life.food_fridge_stock_lot.nutrition_source_snapshot IS 'Catalog/custom mapping and per-100g values copied when the lot is added; historical calculations must use this snapshot.';
COMMENT ON TABLE cynexus_appl_life.food_fridge_stock_event IS 'Append-only inventory change ledger; application writes a matching quantity_before/after row in the same transaction as the lot update.';
COMMENT ON TABLE cynexus_appl_life.food_meal_item_fridge_lot IS 'Allocation from consumed diary items to fridge lots, allowing one meal item to consume several expiry batches.';
COMMENT ON VIEW cynexus_appl_life.food_fridge_inventory_current IS 'Current owner-scoped fridge aggregate with earliest expiry, bounded visual stack count, display quantity, and per-nutrient completeness.';
