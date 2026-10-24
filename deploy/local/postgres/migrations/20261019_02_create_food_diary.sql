\set ON_ERROR_STOP on

-- Additive Life diary foundation. Apply after the shared nutrition catalog.
CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_goal_history (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    effective_from date NOT NULL,
    calories_kcal numeric,
    carbs_g numeric,
    protein_g numeric,
    fat_g numeric,
    notes text,
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_goal_history_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_goal_history_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT ck_food_goal_history_nutrition CHECK (
        num_nonnulls(calories_kcal, carbs_g, protein_g, fat_g) > 0
        AND (calories_kcal IS NULL OR (calories_kcal >= 0 AND calories_kcal < 'Infinity'::numeric))
        AND (carbs_g IS NULL OR (carbs_g >= 0 AND carbs_g < 'Infinity'::numeric))
        AND (protein_g IS NULL OR (protein_g >= 0 AND protein_g < 'Infinity'::numeric))
        AND (fat_g IS NULL OR (fat_g >= 0 AND fat_g < 'Infinity'::numeric))
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_food_goal_history_effective_alive
    ON cynexus_appl_life.food_goal_history (owner_key, effective_from) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_diary_day (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    diary_date date NOT NULL,
    timezone text NOT NULL CHECK (btrim(timezone) <> ''),
    goal_history_id bigint,
    target_calories_kcal numeric,
    target_carbs_g numeric,
    target_protein_g numeric,
    target_fat_g numeric,
    status text NOT NULL DEFAULT 'open',
    completed_at timestamptz,
    notes text,
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_diary_day_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_diary_day_date UNIQUE (owner_key, diary_date),
    CONSTRAINT uk_food_diary_day_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_food_diary_day_goal FOREIGN KEY (owner_key, goal_history_id)
        REFERENCES cynexus_appl_life.food_goal_history (owner_key, id),
    CONSTRAINT ck_food_diary_day_status CHECK (
        (status = 'open' AND completed_at IS NULL) OR (status = 'complete' AND completed_at IS NOT NULL)
    ),
    CONSTRAINT ck_food_diary_day_targets CHECK (
        (target_calories_kcal IS NULL OR (target_calories_kcal >= 0 AND target_calories_kcal < 'Infinity'::numeric))
        AND (target_carbs_g IS NULL OR (target_carbs_g >= 0 AND target_carbs_g < 'Infinity'::numeric))
        AND (target_protein_g IS NULL OR (target_protein_g >= 0 AND target_protein_g < 'Infinity'::numeric))
        AND (target_fat_g IS NULL OR (target_fat_g >= 0 AND target_fat_g < 'Infinity'::numeric))
    )
);
CREATE INDEX IF NOT EXISTS idx_food_diary_day_goal
    ON cynexus_appl_life.food_diary_day (owner_key, goal_history_id);

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_meal (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    diary_day_id bigint NOT NULL,
    meal_type text NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
    eaten_at timestamptz,
    notes text,
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_meal_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_meal_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_food_meal_day FOREIGN KEY (owner_key, diary_day_id)
        REFERENCES cynexus_appl_life.food_diary_day (owner_key, id)
);
CREATE INDEX IF NOT EXISTS idx_food_meal_day
    ON cynexus_appl_life.food_meal (owner_key, diary_day_id, sort_order, id);

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_custom (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    name text NOT NULL CHECK (btrim(name) <> ''),
    brand text,
    barcode text,
    calories_kcal numeric,
    carbs_g numeric,
    protein_g numeric,
    fat_g numeric,
    provenance jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(provenance) = 'object'),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_custom_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_custom_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT ck_food_custom_nutrition CHECK (
        num_nonnulls(calories_kcal, carbs_g, protein_g, fat_g) > 0
        AND (calories_kcal IS NULL OR (calories_kcal >= 0 AND calories_kcal < 'Infinity'::numeric))
        AND (carbs_g IS NULL OR (carbs_g >= 0 AND carbs_g < 'Infinity'::numeric))
        AND (protein_g IS NULL OR (protein_g >= 0 AND protein_g < 'Infinity'::numeric))
        AND (fat_g IS NULL OR (fat_g >= 0 AND fat_g < 'Infinity'::numeric))
    )
);
CREATE INDEX IF NOT EXISTS idx_food_custom_name_alive
    ON cynexus_appl_life.food_custom (owner_key, name, id) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_portion (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    catalog_food_id bigint REFERENCES cynexus_appl_life.food_nutrition_catalog (id),
    custom_food_id bigint,
    quantity numeric NOT NULL DEFAULT 1,
    unit text NOT NULL CHECK (btrim(unit) <> ''),
    grams numeric NOT NULL,
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_portion_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_portion_catalog_identity UNIQUE (owner_key, id, catalog_food_id),
    CONSTRAINT uk_food_portion_custom_identity UNIQUE (owner_key, id, custom_food_id),
    CONSTRAINT uk_food_portion_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_food_portion_custom FOREIGN KEY (owner_key, custom_food_id)
        REFERENCES cynexus_appl_life.food_custom (owner_key, id),
    CONSTRAINT ck_food_portion_food CHECK (num_nonnulls(catalog_food_id, custom_food_id) = 1),
    CONSTRAINT ck_food_portion_weight CHECK (
        quantity > 0 AND quantity < 'Infinity'::numeric AND grams > 0 AND grams < 'Infinity'::numeric
        AND (unit <> 'g' OR grams = quantity)
        AND (unit <> 'kg' OR grams = quantity * 1000)
        AND (unit <> 'mg' OR grams = quantity / 1000)
    )
);
CREATE INDEX IF NOT EXISTS idx_food_portion_catalog ON cynexus_appl_life.food_portion (catalog_food_id);
CREATE INDEX IF NOT EXISTS idx_food_portion_custom ON cynexus_appl_life.food_portion (owner_key, custom_food_id);

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_favorite (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    catalog_food_id bigint REFERENCES cynexus_appl_life.food_nutrition_catalog (id),
    custom_food_id bigint,
    default_portion_id bigint,
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_favorite_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_favorite_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT ck_food_favorite_food CHECK (num_nonnulls(catalog_food_id, custom_food_id) = 1),
    CONSTRAINT fk_food_favorite_custom FOREIGN KEY (owner_key, custom_food_id)
        REFERENCES cynexus_appl_life.food_custom (owner_key, id),
    CONSTRAINT fk_food_favorite_portion_owner FOREIGN KEY (owner_key, default_portion_id)
        REFERENCES cynexus_appl_life.food_portion (owner_key, id),
    CONSTRAINT fk_food_favorite_portion_catalog FOREIGN KEY (owner_key, default_portion_id, catalog_food_id)
        REFERENCES cynexus_appl_life.food_portion (owner_key, id, catalog_food_id),
    CONSTRAINT fk_food_favorite_portion_custom FOREIGN KEY (owner_key, default_portion_id, custom_food_id)
        REFERENCES cynexus_appl_life.food_portion (owner_key, id, custom_food_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_food_favorite_catalog_alive
    ON cynexus_appl_life.food_favorite (owner_key, catalog_food_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_food_favorite_custom_alive
    ON cynexus_appl_life.food_favorite (owner_key, custom_food_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_food_favorite_portion
    ON cynexus_appl_life.food_favorite (owner_key, default_portion_id);
CREATE INDEX IF NOT EXISTS idx_food_favorite_order_alive
    ON cynexus_appl_life.food_favorite (owner_key, sort_order, id) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_meal_item (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    meal_id bigint NOT NULL,
    entry_kind text NOT NULL CHECK (entry_kind IN ('catalog', 'custom', 'quick')),
    catalog_food_id bigint REFERENCES cynexus_appl_life.food_nutrition_catalog (id),
    custom_food_id bigint,
    portion_id bigint,
    food_name text NOT NULL CHECK (btrim(food_name) <> ''),
    brand text,
    quantity numeric,
    unit text,
    grams_per_unit numeric,
    consumed_grams numeric GENERATED ALWAYS AS (quantity * grams_per_unit) STORED,
    calories_per100g numeric,
    carbs_per100g numeric,
    protein_per100g numeric,
    fat_per100g numeric,
    quick_calories_kcal numeric,
    quick_carbs_g numeric,
    quick_protein_g numeric,
    quick_fat_g numeric,
    calories_kcal numeric GENERATED ALWAYS AS (
        CASE WHEN entry_kind = 'quick' THEN quick_calories_kcal
             ELSE calories_per100g * quantity * grams_per_unit / 100 END
    ) STORED,
    carbs_g numeric GENERATED ALWAYS AS (
        CASE WHEN entry_kind = 'quick' THEN quick_carbs_g
             ELSE carbs_per100g * quantity * grams_per_unit / 100 END
    ) STORED,
    protein_g numeric GENERATED ALWAYS AS (
        CASE WHEN entry_kind = 'quick' THEN quick_protein_g
             ELSE protein_per100g * quantity * grams_per_unit / 100 END
    ) STORED,
    fat_g numeric GENERATED ALWAYS AS (
        CASE WHEN entry_kind = 'quick' THEN quick_fat_g
             ELSE fat_per100g * quantity * grams_per_unit / 100 END
    ) STORED,
    source_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(source_snapshot) = 'object'),
    notes text,
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    idempotency_key text NOT NULL CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metadata) = 'object'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_meal_item_owner_id UNIQUE (owner_key, id),
    CONSTRAINT uk_food_meal_item_request UNIQUE (owner_key, idempotency_key),
    CONSTRAINT fk_food_meal_item_meal FOREIGN KEY (owner_key, meal_id)
        REFERENCES cynexus_appl_life.food_meal (owner_key, id),
    CONSTRAINT fk_food_meal_item_custom FOREIGN KEY (owner_key, custom_food_id)
        REFERENCES cynexus_appl_life.food_custom (owner_key, id),
    CONSTRAINT fk_food_meal_item_portion_owner FOREIGN KEY (owner_key, portion_id)
        REFERENCES cynexus_appl_life.food_portion (owner_key, id),
    CONSTRAINT fk_food_meal_item_portion_catalog FOREIGN KEY (owner_key, portion_id, catalog_food_id)
        REFERENCES cynexus_appl_life.food_portion (owner_key, id, catalog_food_id),
    CONSTRAINT fk_food_meal_item_portion_custom FOREIGN KEY (owner_key, portion_id, custom_food_id)
        REFERENCES cynexus_appl_life.food_portion (owner_key, id, custom_food_id),
    CONSTRAINT ck_food_meal_item_mode CHECK (
        (entry_kind = 'catalog' AND catalog_food_id IS NOT NULL AND custom_food_id IS NULL)
        OR (entry_kind = 'custom' AND custom_food_id IS NOT NULL AND catalog_food_id IS NULL)
        OR (entry_kind = 'quick' AND catalog_food_id IS NULL AND custom_food_id IS NULL AND portion_id IS NULL)
    ),
    CONSTRAINT ck_food_meal_item_quantity CHECK (
        ((quantity IS NULL AND unit IS NULL AND grams_per_unit IS NULL)
         OR (quantity IS NOT NULL AND quantity > 0 AND quantity < 'Infinity'::numeric
             AND unit IS NOT NULL AND btrim(unit) <> ''
             AND (grams_per_unit IS NULL OR (grams_per_unit > 0 AND grams_per_unit < 'Infinity'::numeric))))
        AND (entry_kind = 'quick' OR (quantity IS NOT NULL AND grams_per_unit IS NOT NULL))
        AND (unit IS DISTINCT FROM 'g' OR grams_per_unit IS NOT DISTINCT FROM 1::numeric)
        AND (unit IS DISTINCT FROM 'kg' OR grams_per_unit IS NOT DISTINCT FROM 1000::numeric)
        AND (unit IS DISTINCT FROM 'mg' OR grams_per_unit IS NOT DISTINCT FROM 0.001::numeric)
    ),
    CONSTRAINT ck_food_meal_item_nutrition_mode CHECK (
        (entry_kind = 'quick' AND num_nonnulls(calories_per100g, carbs_per100g, protein_per100g, fat_per100g) = 0
            AND num_nonnulls(quick_calories_kcal, quick_carbs_g, quick_protein_g, quick_fat_g) > 0)
        OR (entry_kind <> 'quick' AND num_nonnulls(quick_calories_kcal, quick_carbs_g, quick_protein_g, quick_fat_g) = 0)
    ),
    CONSTRAINT ck_food_meal_item_nutrition CHECK (
        (calories_per100g IS NULL OR (calories_per100g >= 0 AND calories_per100g < 'Infinity'::numeric))
        AND (carbs_per100g IS NULL OR (carbs_per100g >= 0 AND carbs_per100g < 'Infinity'::numeric))
        AND (protein_per100g IS NULL OR (protein_per100g >= 0 AND protein_per100g < 'Infinity'::numeric))
        AND (fat_per100g IS NULL OR (fat_per100g >= 0 AND fat_per100g < 'Infinity'::numeric))
        AND (quick_calories_kcal IS NULL OR (quick_calories_kcal >= 0 AND quick_calories_kcal < 'Infinity'::numeric))
        AND (quick_carbs_g IS NULL OR (quick_carbs_g >= 0 AND quick_carbs_g < 'Infinity'::numeric))
        AND (quick_protein_g IS NULL OR (quick_protein_g >= 0 AND quick_protein_g < 'Infinity'::numeric))
        AND (quick_fat_g IS NULL OR (quick_fat_g >= 0 AND quick_fat_g < 'Infinity'::numeric))
    )
);
CREATE INDEX IF NOT EXISTS idx_food_meal_item_meal
    ON cynexus_appl_life.food_meal_item (owner_key, meal_id, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_food_meal_item_catalog ON cynexus_appl_life.food_meal_item (catalog_food_id);
CREATE INDEX IF NOT EXISTS idx_food_meal_item_custom ON cynexus_appl_life.food_meal_item (owner_key, custom_food_id);
CREATE INDEX IF NOT EXISTS idx_food_meal_item_portion ON cynexus_appl_life.food_meal_item (owner_key, portion_id);

CREATE OR REPLACE VIEW cynexus_appl_life.food_daily_summary AS
WITH intake AS (
    SELECT d.id AS diary_day_id, d.owner_key, d.diary_date, d.timezone, d.status, d.completed_at,
           d.target_calories_kcal, d.target_carbs_g, d.target_protein_g, d.target_fat_g,
           count(DISTINCT m.id) AS meal_count, count(i.id) AS item_count,
           CASE WHEN count(i.id) = 0 THEN 0 ELSE sum(i.calories_kcal) END AS known_calories_kcal,
           CASE WHEN count(i.id) = 0 THEN 0 ELSE sum(i.carbs_g) END AS known_carbs_g,
           CASE WHEN count(i.id) = 0 THEN 0 ELSE sum(i.protein_g) END AS known_protein_g,
           CASE WHEN count(i.id) = 0 THEN 0 ELSE sum(i.fat_g) END AS known_fat_g,
           count(i.id) FILTER (WHERE i.calories_kcal IS NULL) AS missing_calories_count,
           count(i.id) FILTER (WHERE i.carbs_g IS NULL) AS missing_carbs_count,
           count(i.id) FILTER (WHERE i.protein_g IS NULL) AS missing_protein_count,
           count(i.id) FILTER (WHERE i.fat_g IS NULL) AS missing_fat_count
    FROM cynexus_appl_life.food_diary_day d
    LEFT JOIN cynexus_appl_life.food_meal m
        ON m.owner_key = d.owner_key AND m.diary_day_id = d.id AND m.deleted_at IS NULL
    LEFT JOIN cynexus_appl_life.food_meal_item i
        ON i.owner_key = m.owner_key AND i.meal_id = m.id AND i.deleted_at IS NULL
    WHERE d.deleted_at IS NULL
    GROUP BY d.id
), totals AS (
    SELECT intake.*,
           CASE WHEN missing_calories_count = 0 THEN known_calories_kcal END AS calories_kcal,
           CASE WHEN missing_carbs_count = 0 THEN known_carbs_g END AS carbs_g,
           CASE WHEN missing_protein_count = 0 THEN known_protein_g END AS protein_g,
           CASE WHEN missing_fat_count = 0 THEN known_fat_g END AS fat_g
    FROM intake
)
SELECT totals.*,
       (missing_calories_count + missing_carbs_count + missing_protein_count + missing_fat_count > 0)
           AS has_incomplete_nutrition,
       calories_kcal / nullif(target_calories_kcal, 0) AS calories_progress,
       carbs_g / nullif(target_carbs_g, 0) AS carbs_progress,
       protein_g / nullif(target_protein_g, 0) AS protein_progress,
       fat_g / nullif(target_fat_g, 0) AS fat_progress,
       target_calories_kcal - calories_kcal AS remaining_calories_kcal,
       target_carbs_g - carbs_g AS remaining_carbs_g,
       target_protein_g - protein_g AS remaining_protein_g,
       target_fat_g - fat_g AS remaining_fat_g
FROM totals;

COMMENT ON TABLE cynexus_appl_life.food_goal_history IS 'Owner daily nutrition targets effective from a local diary date; resolve the latest active date <= diary_date when creating a day.';
COMMENT ON TABLE cynexus_appl_life.food_diary_day IS 'One stable diary date per owner, including explicit empty/completed days and historical target snapshots.';
COMMENT ON TABLE cynexus_appl_life.food_meal IS 'Multiple meals per diary day; meal types are deliberately not unique, allowing repeated snacks.';
COMMENT ON TABLE cynexus_appl_life.food_custom IS 'Private owner-defined foods; all nutrition is per 100 g, NULL unknown and zero observed.';
COMMENT ON TABLE cynexus_appl_life.food_portion IS 'Owner food-specific measured conversion: quantity of unit equals grams. No implicit volume/density conversion.';
COMMENT ON TABLE cynexus_appl_life.food_favorite IS 'Owner favorites with an optional same-owner, same-food default portion.';
COMMENT ON TABLE cynexus_appl_life.food_meal_item IS 'Consumed food snapshots; catalog/custom use per-100g calculation, quick records actual totals without requiring a known weight.';
COMMENT ON VIEW cynexus_appl_life.food_daily_summary IS 'Active diary days with intake, snapshots and completeness; consumers MUST filter authenticated owner_key and bound diary_date. No row-level authorization is provided by this view.';

COMMENT ON COLUMN cynexus_appl_life.food_diary_day.timezone IS 'IANA timezone captured at day creation, validated by the application; never recompute diary_date from current timezone or eaten_at.';
COMMENT ON COLUMN cynexus_appl_life.food_diary_day.goal_history_id IS 'Optional same-owner provenance. Target columns are copied at creation and never dynamically joined to mutable goals.';
COMMENT ON COLUMN cynexus_appl_life.food_diary_day.status IS 'open: incomplete logging; complete: owner explicitly finished, including deliberate zero-intake days. Does not imply all nutrients are known.';
COMMENT ON COLUMN cynexus_appl_life.food_meal.eaten_at IS 'Optional occurrence instant; diary_day_id, not this timestamp, determines diary membership.';
COMMENT ON COLUMN cynexus_appl_life.food_custom.provenance IS 'Label/source/method and optional license information for owner-entered nutrition.';
COMMENT ON COLUMN cynexus_appl_life.food_meal_item.food_name IS 'Name snapshot at logging; never resolve historical display names from the live catalog/custom record.';
COMMENT ON COLUMN cynexus_appl_life.food_meal_item.grams_per_unit IS 'Explicit conversion snapshot; copy portion grams/quantity or supply measured density for ml. g=1, kg=1000, mg=0.001.';
COMMENT ON COLUMN cynexus_appl_life.food_meal_item.source_snapshot IS 'Captured source dataset/food identity, license, usage restrictions, estimates and measurement method; later source edits cannot rewrite this JSON.';
COMMENT ON COLUMN cynexus_appl_life.food_meal_item.portion_id IS 'Optional same-owner/same-food provenance; quantity/unit/conversion snapshots remain authoritative after portion changes.';
COMMENT ON COLUMN cynexus_appl_life.food_meal_item.calories_kcal IS 'Generated actual kcal: snapshot per100g * quantity * grams_per_unit / 100, or quick actual total. NULL remains unknown.';
COMMENT ON COLUMN cynexus_appl_life.food_daily_summary.known_calories_kcal IS 'Sum of known kcal only; NULL if every item is unknown, zero for no items. Check missing_calories_count before presenting as a complete total.';
COMMENT ON COLUMN cynexus_appl_life.food_daily_summary.calories_kcal IS 'Complete kcal total; NULL if any active item has unknown kcal. An empty day totals zero but its status still distinguishes unfinished logging.';
COMMENT ON COLUMN cynexus_appl_life.food_daily_summary.calories_progress IS 'Complete intake / target; NULL if intake unknown or target absent/zero. May exceed 1; remaining values may be negative.';

-- Common column contracts for all owner-scoped tables; no runtime triggers needed.
DO $comments$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY['food_goal_history', 'food_diary_day', 'food_meal',
        'food_custom', 'food_portion', 'food_favorite', 'food_meal_item']
    LOOP
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.id IS %L', table_name,
            'Positive application-assigned bigint identity; no database sequence.');
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.owner_key IS %L', table_name,
            'Authenticated stable owner identity, assigned server-side. Composite foreign keys prevent cross-owner links; queries must enforce owner scope.');
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.idempotency_key IS %L', table_name,
            'Client mutation key, unique per owner within this resource even after soft deletion. Retry must return the existing record; conflicting payloads must be rejected.');
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.updated_at IS %L', table_name,
            'Application-maintained update timestamp; inserts default to now().');
        EXECUTE format('COMMENT ON COLUMN cynexus_appl_life.%I.deleted_at IS %L', table_name,
            'Soft deletion. Historical references remain; application must reject new links to deleted records.');
    END LOOP;
END
$comments$;
