\set ON_ERROR_STOP on

CREATE TABLE IF NOT EXISTS cynexus_appl_life.food_asset_catalog_mapping (
    catalog_food_id bigint PRIMARY KEY
        REFERENCES cynexus_appl_life.food_nutrition_catalog(id),
    catalog_key text NOT NULL
        REFERENCES cynexus_appl_life.food_asset_mapping(catalog_key),
    association_source text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_food_asset_catalog_mapping_source CHECK (
        association_source IN ('name_match', 'fridge_confirmation', 'manual', 'legacy')
    )
);

ALTER TABLE cynexus_appl_life.food_asset_catalog_mapping
    ADD COLUMN IF NOT EXISTS deleted_at timestamptz;

CREATE INDEX IF NOT EXISTS idx_food_asset_catalog_mapping_catalog_key
    ON cynexus_appl_life.food_asset_catalog_mapping (catalog_key);

COMMENT ON TABLE cynexus_appl_life.food_asset_catalog_mapping IS
    'Associates each source-specific nutrition food with at most one shared artwork key; one artwork key may serve many nutrition foods.';
COMMENT ON COLUMN cynexus_appl_life.food_asset_catalog_mapping.association_source IS
    'Association provenance: exact name/alias match, fridge confirmation, manual selection, or migrated legacy link.';
COMMENT ON COLUMN cynexus_appl_life.food_asset_mapping.catalog_food_id IS
    'Legacy single-food link retained for compatibility; food_asset_catalog_mapping is the canonical many-food-to-one-asset relation.';

-- Preserve an unambiguous existing direct association before attempting names.
WITH legacy_matches AS (
    SELECT a.catalog_food_id,
           min(a.catalog_key) AS catalog_key
    FROM cynexus_appl_life.food_asset_mapping a
    JOIN cynexus_appl_life.food_nutrition_catalog c
      ON c.id = a.catalog_food_id
     AND c.deleted_at IS NULL
    WHERE a.catalog_food_id IS NOT NULL
      AND a.deleted_at IS NULL
    GROUP BY a.catalog_food_id
    HAVING count(DISTINCT a.catalog_key) = 1
)
INSERT INTO cynexus_appl_life.food_asset_catalog_mapping
    (catalog_food_id, catalog_key, association_source)
SELECT catalog_food_id, catalog_key, 'legacy'
FROM legacy_matches
ON CONFLICT (catalog_food_id) DO NOTHING;

-- Match the full normalized catalog name/alias sets. Ambiguous foods stay
-- unlinked until a user confirms the artwork association.
WITH asset_names AS (
    SELECT DISTINCT a.catalog_key,
           lower(regexp_replace(btrim(n.name), '\s+', ' ', 'g')) AS normalized_name
    FROM cynexus_appl_life.food_asset_mapping a
    CROSS JOIN LATERAL unnest(
        ARRAY[a.canonical_name, a.english_name] || COALESCE(a.aliases, '{}'::text[])
    ) AS n(name)
    WHERE a.deleted_at IS NULL
      AND n.name IS NOT NULL
      AND btrim(n.name) <> ''
),
food_names AS (
    SELECT DISTINCT c.id AS catalog_food_id,
           lower(regexp_replace(btrim(n.name), '\s+', ' ', 'g')) AS normalized_name
    FROM cynexus_appl_life.food_nutrition_catalog c
    CROSS JOIN LATERAL (
        SELECT c.description AS name
        UNION ALL
        SELECT unnest(COALESCE(c.aliases, '{}'::text[])) AS name
    ) AS n
    WHERE c.deleted_at IS NULL
      AND n.name IS NOT NULL
      AND btrim(n.name) <> ''
),
candidate_assets AS (
    SELECT DISTINCT f.catalog_food_id, a.catalog_key
    FROM food_names f
    JOIN asset_names a USING (normalized_name)
),
unambiguous_matches AS (
    SELECT catalog_food_id,
           min(catalog_key) AS catalog_key
    FROM candidate_assets
    GROUP BY catalog_food_id
    HAVING count(DISTINCT catalog_key) = 1
)
INSERT INTO cynexus_appl_life.food_asset_catalog_mapping
    (catalog_food_id, catalog_key, association_source)
SELECT catalog_food_id, catalog_key, 'name_match'
FROM unambiguous_matches
ON CONFLICT (catalog_food_id) DO NOTHING;
