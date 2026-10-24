\set ON_ERROR_STOP on

CREATE SCHEMA IF NOT EXISTS cynexus_appl_life;

-- Shared, versioned artwork mapping for the food catalog.  The UI keeps the
-- binary files, while this table is the source of truth for their catalog
-- identity and atlas coordinates.
CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_asset_mapping (
    catalog_key text PRIMARY KEY,
    canonical_name text,
    catalog_food_id bigint REFERENCES cynexus_appl_life.food_nutrition_catalog(id),
    asset_key text NOT NULL UNIQUE,
    asset_url text NOT NULL,
    source_atlas text,
    asset_row integer,
    asset_column integer,
    printed_label integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_food_asset_mapping_catalog_key CHECK (catalog_key ~ '^food-[0-9]{3}$'),
    CONSTRAINT ck_food_asset_mapping_asset_key CHECK (asset_key ~ '^[A-Za-z0-9_-]{1,80}$'),
    CONSTRAINT ck_food_asset_mapping_asset_url CHECK (asset_url ~ '^/mobile/[A-Za-z0-9_./-]+$'),
    CONSTRAINT ck_food_asset_mapping_coordinates CHECK (
        (source_atlas IS NULL AND asset_row IS NULL AND asset_column IS NULL)
        OR (source_atlas IS NOT NULL AND asset_row > 0 AND asset_column > 0)
    )
);

ALTER TABLE cynexus_appl_life.food_asset_mapping
    ADD COLUMN IF NOT EXISTS canonical_name text;

CREATE INDEX IF NOT EXISTS idx_food_asset_mapping_alive
    ON cynexus_appl_life.food_asset_mapping (catalog_key)
    WHERE deleted_at IS NULL;

-- The first 100 artwork files predate the supplied atlases.  The first
-- supplied sheet is a 10 x 10 grid (101-200); the second is a 10 x 9 grid
-- (201-290) and its printed labels intentionally contain gaps/typos.
INSERT INTO cynexus_appl_life.food_asset_mapping
    (catalog_key, canonical_name, asset_key, asset_url, source_atlas, asset_row, asset_column, printed_label)
SELECT
    format('food-%s', lpad(n::text, 3, '0')),
    format('food-%s', lpad(n::text, 3, '0')),
    format('food-%s', lpad(n::text, 3, '0')),
    format('/mobile/fridge/foods/food-%s.webp', lpad(n::text, 3, '0')),
    CASE WHEN n BETWEEN 101 AND 200 THEN 'fridge-foods-101-200.png'
         WHEN n >= 201 THEN 'fridge-foods-201-300-source.png' END,
    CASE WHEN n BETWEEN 101 AND 200 THEN ((n - 101) / 10) + 1
         WHEN n >= 201 THEN ((n - 201) / 9) + 1 END,
    CASE WHEN n BETWEEN 101 AND 200 THEN ((n - 101) % 10) + 1
         WHEN n >= 201 THEN ((n - 201) % 9) + 1 END,
    CASE WHEN n BETWEEN 101 AND 200 THEN n
         WHEN n >= 201 THEN (ARRAY[
             201,202,203,204,205,206,207,208,209,
             211,212,213,214,215,216,217,218,220,
             221,222,223,224,225,226,227,228,230,
             231,232,233,234,235,236,237,238,240,
             241,242,243,244,245,246,247,248,250,
             251,252,253,254,255,256,257,258,259,
             261,262,263,264,265,266,267,268,279,
             271,272,273,274,275,276,277,278,280,
             281,282,283,284,285,286,287,289,299,
             291,292,293,294,295,296,297,298,300
         ])[n - 200] END
FROM generate_series(1, 290) AS numbers(n)
ON CONFLICT (catalog_key) DO UPDATE SET
    canonical_name = EXCLUDED.canonical_name,
    asset_key = EXCLUDED.asset_key,
    asset_url = EXCLUDED.asset_url,
    source_atlas = EXCLUDED.source_atlas,
    asset_row = EXCLUDED.asset_row,
    asset_column = EXCLUDED.asset_column,
    printed_label = EXCLUDED.printed_label,
    deleted_at = NULL,
    updated_at = now();

COMMENT ON TABLE cynexus_appl_life.food_asset_mapping IS
    'Database-owned mapping from food catalog IDs to the checked-in mobile artwork files.';
COMMENT ON COLUMN cynexus_appl_life.food_asset_mapping.printed_label IS
    'Number printed next to an item in the source atlas; nullable because source sheets can be absent or mislabeled.';
