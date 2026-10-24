\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE _fridge_nutrition_matches ON COMMIT DROP AS
WITH candidates AS (
    SELECT i.owner_key,
           i.id AS fridge_item_id,
           confirmed.catalog_food_id AS confirmed_catalog_id,
           confirmed.custom_food_id AS confirmed_custom_id,
           exact_match.catalog_food_id AS exact_catalog_id,
           exact_match.match_count,
           CASE
               WHEN confirmed.catalog_food_id IS NOT NULL OR confirmed.custom_food_id IS NOT NULL
                   THEN 'user_confirmed'
               ELSE 'matched'
           END AS mapping_status
    FROM cynexus_appl_life.food_fridge_item i
    LEFT JOIN LATERAL (
        SELECT m.catalog_food_id, m.custom_food_id
        FROM cynexus_appl_life.food_fridge_mapping m
        LEFT JOIN cynexus_appl_life.food_nutrition_catalog c
          ON c.id=m.catalog_food_id AND c.deleted_at IS NULL AND c.nutrition_usable=true
        LEFT JOIN cynexus_appl_life.food_custom f
          ON f.owner_key=m.owner_key AND f.id=m.custom_food_id AND f.deleted_at IS NULL
        WHERE m.owner_key=i.owner_key
          AND m.normalized_name=i.normalized_name
          AND lower(btrim(m.brand)) IS NOT DISTINCT FROM lower(btrim(i.brand))
          AND m.deleted_at IS NULL
          AND (c.id IS NOT NULL OR f.id IS NOT NULL)
        ORDER BY m.updated_at DESC,m.id DESC
        LIMIT 1
    ) confirmed ON true
    LEFT JOIN LATERAL (
        SELECT min(c.id) AS catalog_food_id, count(*) AS match_count
        FROM cynexus_appl_life.food_nutrition_catalog c
        WHERE c.deleted_at IS NULL AND c.nutrition_usable=true
          AND (
              lower(regexp_replace(btrim(c.description), '\s+', ' ', 'g'))=i.normalized_name
              OR EXISTS (
                  SELECT 1 FROM unnest(c.aliases) alias
                  WHERE lower(regexp_replace(btrim(alias), '\s+', ' ', 'g'))=i.normalized_name
              )
          )
    ) exact_match ON true
    WHERE i.deleted_at IS NULL
      AND i.mapping_status IN ('pending','unmatched')
      AND i.catalog_food_id IS NULL AND i.custom_food_id IS NULL
), resolved AS (
    SELECT owner_key, fridge_item_id,
           COALESCE(confirmed_catalog_id,
                    CASE WHEN confirmed_custom_id IS NULL AND match_count=1 THEN exact_catalog_id END) AS catalog_food_id,
           confirmed_custom_id AS custom_food_id,
           CASE WHEN confirmed_catalog_id IS NOT NULL OR confirmed_custom_id IS NOT NULL
                THEN 'user_confirmed' ELSE 'matched' END AS mapping_status
    FROM candidates
)
SELECT r.owner_key,r.fridge_item_id,r.catalog_food_id,r.custom_food_id,r.mapping_status,
       CASE WHEN r.catalog_food_id IS NOT NULL THEN c.calories_kcal ELSE f.calories_kcal END AS calories_kcal,
       CASE WHEN r.catalog_food_id IS NOT NULL THEN c.carbs_g ELSE f.carbs_g END AS carbs_g,
       CASE WHEN r.catalog_food_id IS NOT NULL THEN c.protein_g ELSE f.protein_g END AS protein_g,
       CASE WHEN r.catalog_food_id IS NOT NULL THEN c.fat_g ELSE f.fat_g END AS fat_g
FROM resolved r
LEFT JOIN cynexus_appl_life.food_nutrition_catalog c
  ON c.id=r.catalog_food_id AND c.deleted_at IS NULL
LEFT JOIN cynexus_appl_life.food_custom f
  ON f.owner_key=r.owner_key AND f.id=r.custom_food_id AND f.deleted_at IS NULL
WHERE r.catalog_food_id IS NOT NULL OR r.custom_food_id IS NOT NULL;

UPDATE cynexus_appl_life.food_fridge_item i
SET catalog_food_id=m.catalog_food_id,
    custom_food_id=m.custom_food_id,
    mapping_status=m.mapping_status,
    mapping_confidence=1,
    mapping_candidates='[]'::jsonb,
    updated_at=now()
FROM _fridge_nutrition_matches m
WHERE i.owner_key=m.owner_key AND i.id=m.fridge_item_id;

UPDATE cynexus_appl_life.food_fridge_stock_lot l
SET nutrition_status=CASE
        WHEN num_nonnulls(m.calories_kcal,m.carbs_g,m.protein_g,m.fat_g)=4 THEN 'known'
        WHEN num_nonnulls(m.calories_kcal,m.carbs_g,m.protein_g,m.fat_g)>0 THEN 'partial'
        ELSE 'pending'
    END,
    calories_per100g=m.calories_kcal,
    carbs_per100g=m.carbs_g,
    protein_per100g=m.protein_g,
    fat_per100g=m.fat_g,
    nutrition_source_snapshot=jsonb_build_object(
        'catalogFoodId',COALESCE(m.catalog_food_id::text,''),
        'customFoodId',COALESCE(m.custom_food_id::text,''),
        'caloriesKcal',COALESCE(m.calories_kcal::text,'')
    ),
    updated_at=now()
FROM _fridge_nutrition_matches m
JOIN cynexus_appl_life.food_fridge_item i
  ON i.owner_key=m.owner_key AND i.id=m.fridge_item_id
WHERE l.owner_key=m.owner_key AND l.fridge_item_id=m.fridge_item_id
  AND l.deleted_at IS NULL AND l.status='active' AND l.quantity_remaining>0;

COMMIT;
