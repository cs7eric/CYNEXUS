\set ON_ERROR_STOP on

-- Owner-scoped nutrition rules.  The stored values are inputs to the
-- calculation, not daily targets: macro values are grams per kilogram and
-- calorie targets are derived from the latest body measurement at read time.
CREATE SCHEMA IF NOT EXISTS cynexus_appl_life;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.nutrition_configuration (
    id bigint PRIMARY KEY CHECK (id > 0),
    owner_key text NOT NULL CHECK (btrim(owner_key) <> ''),
    sex text,
    body_fat_pct numeric(5, 2),
    age_years smallint,
    activity_factor numeric(6, 3),
    calorie_deficit_kcal numeric(10, 2) NOT NULL DEFAULT 500,
    carbs_g_per_kg numeric(8, 3) NOT NULL DEFAULT 3,
    protein_g_per_kg numeric(8, 3) NOT NULL DEFAULT 1.7,
    fat_g_per_kg numeric(8, 3) NOT NULL DEFAULT 0.9,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_nutrition_configuration_owner_id UNIQUE (owner_key, id),
    CONSTRAINT ck_nutrition_configuration_sex CHECK (sex IS NULL OR sex IN ('male', 'female')),
    CONSTRAINT ck_nutrition_configuration_body_fat CHECK (
        body_fat_pct IS NULL OR (body_fat_pct >= 0 AND body_fat_pct <= 100)
    ),
    CONSTRAINT ck_nutrition_configuration_age CHECK (
        age_years IS NULL OR age_years BETWEEN 1 AND 120
    ),
    CONSTRAINT ck_nutrition_configuration_activity CHECK (
        activity_factor IS NULL OR (activity_factor >= 1.1 AND activity_factor <= 2.5)
    ),
    CONSTRAINT ck_nutrition_configuration_deficit CHECK (
        calorie_deficit_kcal >= 0 AND calorie_deficit_kcal < 'Infinity'::numeric
    ),
    CONSTRAINT ck_nutrition_configuration_carbs CHECK (
        carbs_g_per_kg >= 0 AND carbs_g_per_kg < 'Infinity'::numeric
    ),
    CONSTRAINT ck_nutrition_configuration_protein CHECK (
        protein_g_per_kg >= 0 AND protein_g_per_kg < 'Infinity'::numeric
    ),
    CONSTRAINT ck_nutrition_configuration_fat CHECK (
        fat_g_per_kg >= 0 AND fat_g_per_kg < 'Infinity'::numeric
    ),
    CONSTRAINT ck_nutrition_configuration_metadata CHECK (jsonb_typeof(metadata) = 'object')
);

-- A soft-deleted configuration may be replaced, while at most one active
-- configuration can exist for an authenticated owner/workspace.
CREATE UNIQUE INDEX IF NOT EXISTS uk_nutrition_configuration_owner_alive
    ON cynexus_appl_life.nutrition_configuration (owner_key)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_nutrition_configuration_owner_updated
    ON cynexus_appl_life.nutrition_configuration (owner_key, updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE cynexus_appl_life.nutrition_configuration IS
    'Owner/workspace nutrition calculation inputs. Macro columns are g/kg multipliers; calorie targets are calculated from current weight and these inputs.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.owner_key IS
    'Authenticated stable owner or workspace identity. Every read and write must enforce this scope.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.body_fat_pct IS
    'Body fat percentage used by the BMR formula; current weight is resolved from workout_body_measurement.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.activity_factor IS
    'Activity multiplier used for TDEE (for example 1.2, 1.375, 1.55, 1.725 or 1.9).';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.calorie_deficit_kcal IS
    'Daily calorie deficit applied to TDEE when calculating the target intake.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.carbs_g_per_kg IS
    'Carbohydrate target multiplier in grams per kilogram of the latest body weight.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.protein_g_per_kg IS
    'Protein target multiplier in grams per kilogram of the latest body weight.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.fat_g_per_kg IS
    'Fat target multiplier in grams per kilogram of the latest body weight.';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.updated_at IS
    'Application-maintained update timestamp; inserts default to now().';
COMMENT ON COLUMN cynexus_appl_life.nutrition_configuration.deleted_at IS
    'Soft deletion marker. Historical rows remain available for audit and are excluded from the active owner uniqueness index.';
