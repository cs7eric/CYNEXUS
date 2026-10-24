\set ON_ERROR_STOP on

BEGIN;

UPDATE cynexus_appl_life.food_asset_mapping
SET aliases = ARRAY(
        SELECT DISTINCT alias
        FROM unnest(COALESCE(aliases, '{}'::text[]) ||
                    ARRAY['燕麦', '燕麦片', 'Oat', 'Oats', 'Oatmeal', 'Instant oats']::text[]) AS alias
        ORDER BY alias
    ),
    updated_at = now()
WHERE catalog_key = 'food-201' AND deleted_at IS NULL;

UPDATE cynexus_appl_life.food_asset_mapping
SET aliases = ARRAY(
        SELECT DISTINCT alias
        FROM unnest(COALESCE(aliases, '{}'::text[]) ||
                    ARRAY['原味燕麦', 'Rolled oats']::text[]) AS alias
        ORDER BY alias
    ),
    updated_at = now()
WHERE catalog_key = 'food-202' AND deleted_at IS NULL;

WITH resolved AS (
    SELECT i.owner_key, i.id AS fridge_item_id, asset.catalog_key, asset.asset_key
    FROM cynexus_appl_life.food_fridge_item i
    JOIN LATERAL (
        SELECT a.catalog_key, a.asset_key
        FROM cynexus_appl_life.food_asset_mapping a
        WHERE a.deleted_at IS NULL
          AND (
              a.catalog_food_id = i.catalog_food_id
              OR lower(btrim(a.canonical_name)) = i.normalized_name
              OR lower(btrim(a.english_name)) = i.normalized_name
              OR EXISTS (
                  SELECT 1 FROM unnest(a.aliases) alias
                  WHERE lower(btrim(alias)) = i.normalized_name
              )
          )
        ORDER BY CASE WHEN a.catalog_food_id = i.catalog_food_id THEN 0 ELSE 1 END,
                 a.catalog_key
        LIMIT 1
    ) asset ON true
    WHERE i.deleted_at IS NULL
      AND (i.asset_catalog_key IS NULL OR i.asset_key IS NULL)
)
UPDATE cynexus_appl_life.food_fridge_item i
SET asset_catalog_key = resolved.catalog_key,
    asset_key = resolved.asset_key,
    updated_at = now()
FROM resolved
WHERE i.owner_key = resolved.owner_key AND i.id = resolved.fridge_item_id;

COMMIT;
