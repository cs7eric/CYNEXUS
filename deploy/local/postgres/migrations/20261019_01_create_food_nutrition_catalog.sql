\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_life;

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_nutrition_source (
    id bigint PRIMARY KEY,
    source_key text NOT NULL,
    dataset_name text NOT NULL,
    license text NOT NULL,
    usage_scope text NOT NULL,
    sample_only boolean NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_nutrition_source_dataset UNIQUE (source_key, dataset_name),
    CONSTRAINT ck_food_nutrition_source_metadata CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_nutrition_import_batch (
    id bigint PRIMARY KEY,
    catalog_sha256 varchar(64) NOT NULL,
    catalog_checked_at timestamptz,
    record_count integer NOT NULL,
    complete_count integer NOT NULL,
    usable_count integer NOT NULL,
    source_count integer NOT NULL,
    manifest jsonb NOT NULL DEFAULT '{}'::jsonb,
    imported_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_nutrition_import_batch_sha256 UNIQUE (catalog_sha256),
    CONSTRAINT ck_food_nutrition_import_batch_sha256 CHECK (catalog_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_food_nutrition_import_batch_counts CHECK (
        record_count >= 0 AND complete_count >= 0 AND usable_count >= 0 AND source_count >= 0
        AND complete_count <= usable_count AND usable_count <= record_count
        AND source_count <= record_count
    ),
    CONSTRAINT ck_food_nutrition_import_batch_manifest CHECK (jsonb_typeof(manifest) = 'object')
);

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_nutrition_catalog (
    id bigint PRIMARY KEY,
    source_id bigint NOT NULL REFERENCES cynexus_appl_life.food_nutrition_source(id),
    source_food_id text NOT NULL,
    description text NOT NULL,
    aliases text[] NOT NULL DEFAULT '{}'::text[],
    basis_amount numeric NOT NULL DEFAULT 100,
    basis_unit text NOT NULL DEFAULT 'g',
    calories_kcal numeric,
    carbs_g numeric,
    protein_g numeric,
    fat_g numeric,
    is_complete boolean NOT NULL,
    nutrition_usable boolean NOT NULL,
    estimated_fields text[] NOT NULL DEFAULT '{}'::text[],
    license text NOT NULL,
    usage_scope text NOT NULL,
    import_batch_id bigint NOT NULL REFERENCES cynexus_appl_life.food_nutrition_import_batch(id),
    raw_record jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uk_food_nutrition_catalog_source_food UNIQUE (source_id, source_food_id),
    CONSTRAINT ck_food_nutrition_catalog_basis CHECK (basis_amount = 100 AND basis_unit = 'g'),
    CONSTRAINT ck_food_nutrition_catalog_calories CHECK (
        calories_kcal IS NULL OR (calories_kcal >= 0 AND calories_kcal < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_food_nutrition_catalog_carbs CHECK (
        carbs_g IS NULL OR (carbs_g >= 0 AND carbs_g < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_food_nutrition_catalog_protein CHECK (
        protein_g IS NULL OR (protein_g >= 0 AND protein_g < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_food_nutrition_catalog_fat CHECK (
        fat_g IS NULL OR (fat_g >= 0 AND fat_g < 'Infinity'::numeric)
    ),
    CONSTRAINT ck_food_nutrition_catalog_complete CHECK (
        is_complete = (calories_kcal IS NOT NULL AND carbs_g IS NOT NULL
            AND protein_g IS NOT NULL AND fat_g IS NOT NULL)
    ),
    CONSTRAINT ck_food_nutrition_catalog_usable CHECK (
        nutrition_usable = (calories_kcal IS NOT NULL OR carbs_g IS NOT NULL
            OR protein_g IS NOT NULL OR fat_g IS NOT NULL)
    ),
    CONSTRAINT ck_food_nutrition_catalog_raw_record CHECK (jsonb_typeof(raw_record) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_food_nutrition_catalog_source_alive
    ON cynexus_appl_life.food_nutrition_catalog (source_id, id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_food_nutrition_catalog_batch
    ON cynexus_appl_life.food_nutrition_catalog (import_batch_id);
CREATE INDEX IF NOT EXISTS idx_food_nutrition_catalog_aliases_alive
    ON cynexus_appl_life.food_nutrition_catalog USING gin (aliases)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_food_nutrition_catalog_description_alive
    ON cynexus_appl_life.food_nutrition_catalog USING gin (to_tsvector('simple'::regconfig, description))
    WHERE deleted_at IS NULL;

COMMENT ON SCHEMA cynexus_appl_life IS 'Life application data, including the shared food nutrition reference catalog.';
COMMENT ON TABLE cynexus_appl_life.food_nutrition_source IS 'Shared upstream dataset provenance and reuse restrictions; no personal or tenant-owned records.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_source.source_key IS 'PoC adapter key; dataset_name distinguishes datasets, releases or subsets within that adapter.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_source.license IS 'Reported upstream license; import does not grant additional reuse rights.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_source.usage_scope IS 'Recorded upstream usage restrictions, preserved without widening permission.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_source.sample_only IS 'True for evaluation samples that must not be represented as a full upstream dataset.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_source.metadata IS 'Original dataset provenance, attribution, release and acquisition metadata.';
COMMENT ON TABLE cynexus_appl_life.food_nutrition_import_batch IS 'Idempotent catalog import provenance keyed by the SHA-256 of the input catalog bytes.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_import_batch.catalog_checked_at IS 'Input manifest check time when available; not the date each upstream nutrient was measured.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_import_batch.manifest IS 'Original catalog manifest and import provenance retained for audit.';
COMMENT ON TABLE cynexus_appl_life.food_nutrition_catalog IS 'Shared source-specific food nutrition records normalized to 100 g; records from different sources remain distinct.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.source_food_id IS 'Original upstream identifier; unique only within its source dataset.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.basis_amount IS 'All normalized nutrient values refer to exactly 100 g, not a serving.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.calories_kcal IS 'Energy in kcal per 100 g. NULL means unknown; zero is an observed or upstream-reported zero.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.carbs_g IS 'Carbohydrate grams per 100 g. NULL means unknown; zero remains zero.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.protein_g IS 'Protein grams per 100 g. NULL means unknown; zero remains zero.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.fat_g IS 'Fat grams per 100 g. NULL means unknown; zero remains zero.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.is_complete IS 'True exactly when all four normalized nutrient values are known; does not imply independently verified accuracy.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.nutrition_usable IS 'True exactly when at least one normalized nutrient value is known.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.estimated_fields IS 'Original nutrient field names identified as estimated by the input record.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.license IS 'Record-level license preserved from the input; does not override dataset restrictions.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.usage_scope IS 'Record-level usage restrictions preserved from the input.';
COMMENT ON COLUMN cynexus_appl_life.food_nutrition_catalog.raw_record IS 'Complete original normalized PoC record, including provenance and quality limitations.';
