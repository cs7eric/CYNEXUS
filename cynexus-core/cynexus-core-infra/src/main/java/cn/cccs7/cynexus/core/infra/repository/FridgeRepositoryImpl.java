package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.core.domain.food.FridgeModels.*;
import cn.cccs7.cynexus.core.domain.food.FoodModels;
import cn.cccs7.cynexus.core.domain.food.FridgeRepository;
import cn.cccs7.cynexus.core.domain.food.FridgeAssetMappingResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC adapter for lot-based fridge inventory. All reads are owner scoped. */
@Repository
public class FridgeRepositoryImpl implements FridgeRepository, FridgeAssetMappingResolver {
    private static final String S = "cynexus_appl_life.";
    private final JdbcTemplate jdbc;
    private final SnowflakeIdGenerator ids;
    private final ObjectMapper json = new ObjectMapper();

    public FridgeRepositoryImpl(JdbcTemplate jdbc, SnowflakeIdGenerator ids) { this.jdbc = jdbc; this.ids = ids; }

    @Override
    public void lockOwner(String owner) { jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 98127))", rs -> {}, owner); }

    @Override
    public Inventory inventory(String owner) {
        List<Item> items = jdbc.query("""
            SELECT v.*, i.created_at AS item_created_at, i.updated_at AS item_updated_at,
                   i.mapping_candidates, i.mapping_confidence,
                   i.asset_catalog_key, i.asset_key, am.asset_url,
                   (SELECT l.grams_per_unit FROM cynexus_appl_life.food_fridge_stock_lot l
                      WHERE l.owner_key=v.owner_key AND l.fridge_item_id=v.fridge_item_id
                        AND l.deleted_at IS NULL AND l.status='active' AND l.quantity_remaining>0
                      ORDER BY l.best_before,l.id LIMIT 1) AS grams_per_unit,
                   (SELECT l.nutrition_status FROM cynexus_appl_life.food_fridge_stock_lot l
                      WHERE l.owner_key=v.owner_key AND l.fridge_item_id=v.fridge_item_id
                        AND l.deleted_at IS NULL AND l.status='active' AND l.quantity_remaining>0
                      ORDER BY l.best_before,l.id LIMIT 1) AS nutrition_status
            FROM cynexus_appl_life.food_fridge_inventory_current v
            JOIN cynexus_appl_life.food_fridge_item i ON i.owner_key=v.owner_key AND i.id=v.fridge_item_id
            LEFT JOIN cynexus_appl_life.food_asset_mapping am
              ON am.catalog_key=i.asset_catalog_key AND am.deleted_at IS NULL
            WHERE v.owner_key=? ORDER BY v.earliest_best_before, v.name, v.fridge_item_id
            """, this::itemRow, owner);
        List<Operation> operations = jdbc.query("""
            SELECT e.id,e.fridge_item_id,i.name,e.event_kind,e.quantity_delta,e.quantity_after,e.occurred_at
            FROM cynexus_appl_life.food_fridge_stock_event e
            JOIN cynexus_appl_life.food_fridge_item i ON i.owner_key=e.owner_key AND i.id=e.fridge_item_id
            WHERE e.owner_key=? AND e.deleted_at IS NULL ORDER BY e.occurred_at DESC,e.id DESC LIMIT 200
            """, this::operationRow, owner);
        return new Inventory(1, false, items, operations);
    }

    @Override
    public Item item(String owner, String itemId) {
        return inventory(owner).items().stream()
                .filter(value -> value.id().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public MealStock mealStock(String owner, String itemId) {
        long parsedItemId = parseId(itemId);
        List<FridgeIdentity> identities = jdbc.query("""
            SELECT id,catalog_food_id,custom_food_id,name,brand,normalized_name
            FROM cynexus_appl_life.food_fridge_item
            WHERE owner_key=? AND id=? AND deleted_at IS NULL
            FOR UPDATE
            """, (r,n) -> new FridgeIdentity(r.getLong("id"), nullableId(r.getLong("catalog_food_id")),
                nullableId(r.getLong("custom_food_id")), r.getString("name"), r.getString("brand"),
                r.getString("normalized_name")), owner, parsedItemId);
        if (identities.isEmpty()) return null;
        FridgeIdentity identity = identities.get(0);
        List<MealLotRow> lots = jdbc.query("""
            SELECT id,quantity_remaining,unit,grams_per_unit,density_g_per_ml,
                   calories_per100g,carbs_per100g,protein_per100g,fat_per100g
            FROM cynexus_appl_life.food_fridge_stock_lot
            WHERE owner_key=? AND fridge_item_id=? AND deleted_at IS NULL
              AND status='active' AND quantity_remaining>0
            ORDER BY best_before,id
            FOR UPDATE
            """, (r,n) -> new MealLotRow(r.getLong("id"), r.getBigDecimal("quantity_remaining"),
                r.getString("unit"), r.getBigDecimal("grams_per_unit"), r.getBigDecimal("density_g_per_ml"),
                new FoodModels.Nutrients(r.getBigDecimal("calories_per100g"), r.getBigDecimal("carbs_per100g"),
                    r.getBigDecimal("protein_per100g"), r.getBigDecimal("fat_per100g"))), owner, parsedItemId);
        List<MealLot> stockLots = lots.stream().map(lot -> new MealLot(lot.id(), lot.quantity(), lot.unit(),
                lot.gramsPerUnit(), lot.density(), lot.nutrients())).toList();
        return new MealStock(itemId, identity.catalogFoodId() == null ? identity.customFoodId() : identity.catalogFoodId(),
                identity.catalogFoodId() == null ? "custom" : "catalog", identity.name(), identity.brand(), stockLots);
    }

    @Override
    public void consumeMeal(String owner, MealSource source, String mealItemId, String idempotencyKey) {
        long mealId = parseId(mealItemId);
        for (MealAllocation allocation : source.allocations()) {
            List<BigDecimal> currentRows = jdbc.query("""
                SELECT quantity_remaining FROM cynexus_appl_life.food_fridge_stock_lot
                WHERE owner_key=? AND fridge_item_id=? AND id=? AND deleted_at IS NULL AND status='active'
                FOR UPDATE
                """, (r,n) -> r.getBigDecimal(1), owner, parseId(source.fridgeItemId()), allocation.lotId());
            if (currentRows.isEmpty()) throw new IllegalArgumentException("Fridge stock changed while the meal was being logged");
            BigDecimal before = currentRows.get(0);
            if (before.compareTo(allocation.quantity()) < 0) throw new IllegalArgumentException("Fridge stock changed while the meal was being logged");
            BigDecimal after = before.subtract(allocation.quantity());
            int changed = jdbc.update("""
                UPDATE cynexus_appl_life.food_fridge_stock_lot
                SET quantity_remaining=?,status=?,updated_at=now()
                WHERE owner_key=? AND fridge_item_id=? AND id=? AND quantity_remaining=? AND status='active'
                """, after, after.signum() == 0 ? "depleted" : "active", owner,
                parseId(source.fridgeItemId()), allocation.lotId(), before);
            if (changed != 1) throw new IllegalArgumentException("Fridge stock changed while the meal was being logged");
            long eventId = ids.nextId();
            String eventKey = derivedKey(idempotencyKey + ":event:" + allocation.lotId());
            jdbc.update("""
                INSERT INTO cynexus_appl_life.food_fridge_stock_event
                  (id,owner_key,fridge_item_id,lot_id,event_kind,quantity_delta,unit,grams_delta,
                   quantity_before,quantity_after,source,idempotency_key)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, eventId, owner, parseId(source.fridgeItemId()), allocation.lotId(), "consume",
                allocation.quantity().negate(), allocation.unit(), allocation.consumedGrams().negate(),
                before, after, "meal", eventKey);
            jdbc.update("""
                INSERT INTO cynexus_appl_life.food_meal_item_fridge_lot
                  (id,owner_key,meal_item_id,lot_id,event_id,quantity,unit,consumed_grams,idempotency_key)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, ids.nextId(), owner, mealId, allocation.lotId(), eventId,
                allocation.quantity(), allocation.unit(), allocation.consumedGrams(),
                derivedKey(idempotencyKey + ":allocation:" + allocation.lotId()));
        }
    }

    @Override
    public void restoreMeal(String owner, String fridgeItemId, String mealItemId, String idempotencyKey) {
        long fridgeId = parseId(fridgeItemId);
        long mealId = parseId(mealItemId);
        List<MealAllocationRow> allocations = jdbc.query("""
            SELECT a.id AS allocation_id,a.lot_id,a.quantity,a.unit,a.consumed_grams,
                   l.quantity_remaining,l.status
            FROM cynexus_appl_life.food_meal_item_fridge_lot a
            JOIN cynexus_appl_life.food_fridge_stock_lot l
              ON l.owner_key=a.owner_key AND l.id=a.lot_id
            WHERE a.owner_key=? AND a.meal_item_id=? AND a.deleted_at IS NULL
              AND l.fridge_item_id=? AND l.deleted_at IS NULL
            ORDER BY a.id
            FOR UPDATE OF a,l
            """, (r,n) -> new MealAllocationRow(r.getLong("allocation_id"), r.getLong("lot_id"),
                r.getBigDecimal("quantity"), r.getString("unit"), r.getBigDecimal("consumed_grams"),
                r.getBigDecimal("quantity_remaining"), r.getString("status")), owner, mealId, fridgeId);
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No active fridge stock allocation was found for this meal");
        }
        for (MealAllocationRow allocation : allocations) {
            if ("discarded".equals(allocation.status())) {
                throw new IllegalArgumentException("Consumed fridge stock was discarded and cannot be restored");
            }
            BigDecimal before = allocation.quantityRemaining();
            BigDecimal after = before.add(allocation.quantity());
            int changed = jdbc.update("""
                UPDATE cynexus_appl_life.food_fridge_stock_lot
                SET quantity_remaining=?,status='active',updated_at=now()
                WHERE owner_key=? AND fridge_item_id=? AND id=? AND quantity_remaining=?
                  AND status IN ('active','depleted') AND deleted_at IS NULL
                """, after, owner, fridgeId, allocation.lotId(), before);
            if (changed != 1) throw new IllegalArgumentException("Fridge stock changed while restoring this meal");

            long eventId = ids.nextId();
            jdbc.update("""
                INSERT INTO cynexus_appl_life.food_fridge_stock_event
                  (id,owner_key,fridge_item_id,lot_id,event_kind,quantity_delta,unit,grams_delta,
                   quantity_before,quantity_after,source,idempotency_key)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, eventId, owner, fridgeId, allocation.lotId(), "add", allocation.quantity(),
                allocation.unit(), allocation.consumedGrams(), before, after, "meal_restore",
                derivedKey(idempotencyKey + ":event:" + allocation.allocationId()));
            jdbc.update("""
                UPDATE cynexus_appl_life.food_meal_item_fridge_lot
                SET deleted_at=now(),updated_at=now()
                WHERE owner_key=? AND id=? AND meal_item_id=? AND deleted_at IS NULL
                """, owner, allocation.allocationId(), mealId);
        }
    }

    @Override
    public Inventory add(String owner, AddCommand command) {
        List<String> existing = jdbc.query("SELECT id FROM "+S+"food_fridge_stock_lot WHERE owner_key=? AND idempotency_key=? AND deleted_at IS NULL", (r,n) -> r.getString("id"), owner, command.idempotencyKey()+":lot");
        if (!existing.isEmpty()) return inventory(owner);

        Mapping mapping = resolveMapping(owner, command);
        AssetMapping asset = command.catalogKey() == null || command.catalogKey().isBlank()
                ? null : assetMapping(command.catalogKey());
        if (command.catalogKey() != null && asset == null) throw new IllegalArgumentException("Asset mapping was not found");
        long itemId = ids.nextId();
        String itemKey = command.idempotencyKey();
        jdbc.update("""
            INSERT INTO cynexus_appl_life.food_fridge_item
              (id,owner_key,catalog_food_id,custom_food_id,asset_catalog_key,asset_key,name,brand,normalized_name,mapping_status,
               mapping_confidence,mapping_candidates,unit_policy,idempotency_key)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?)
            """, itemId, owner, mapping.catalogId(), mapping.customId(), command.catalogKey(), asset == null ? null : asset.assetKey(),
            command.name().trim(), command.brand(), normalize(command.name()),
            mapping.status(), mapping.confidence(), mapping.candidatesJson(), unitPolicy(command), itemKey);

        Nutrients nutrients = nutrition(owner, mapping);
        String nutritionStatus = status(nutrients);
        String snapshot = snapshot(mapping, nutrients);
        long lotId = ids.nextId();
        BigDecimal density = volumeUnit(command.unit()) ? command.gramsPerUnit() : null;
        BigDecimal gramsPerUnit = volumeUnit(command.unit()) || directWeight(command.unit()) ? null : command.gramsPerUnit();
        jdbc.update("""
            INSERT INTO cynexus_appl_life.food_fridge_stock_lot
              (id,owner_key,fridge_item_id,quantity_remaining,unit,grams_per_unit,density_g_per_ml,
               best_before,nutrition_status,calories_per100g,carbs_per100g,protein_per100g,fat_per100g,
               nutrition_source_snapshot,idempotency_key)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?)
            """, lotId, owner, itemId, command.quantity(), command.unit(), gramsPerUnit, density, command.bestBefore(), nutritionStatus,
            nutrients.caloriesKcal(), nutrients.carbsG(), nutrients.proteinG(), nutrients.fatG(), snapshot, command.idempotencyKey()+":lot");
        BigDecimal grams = grams(command.quantity(), command.unit(), gramsPerUnit, density);
        long eventId = ids.nextId();
        jdbc.update("""
            INSERT INTO cynexus_appl_life.food_fridge_stock_event
              (id,owner_key,fridge_item_id,lot_id,event_kind,quantity_delta,unit,grams_delta,
               quantity_before,quantity_after,source,idempotency_key)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """, eventId, owner, itemId, lotId, "add", command.quantity(), command.unit(), grams, BigDecimal.ZERO, command.quantity(), "user", command.idempotencyKey());
        return inventory(owner);
    }

    @Override
    public Inventory adjust(String owner, String itemId, AdjustCommand command) {
        long parsed = parseId(itemId);
        String idempotencyKey = command.idempotencyKey();
        String derivedKeyPrefix = idempotencyKey + ":";
        if (jdbc.queryForObject("SELECT count(*) FROM "+S+"food_fridge_stock_event WHERE owner_key=? AND (idempotency_key=? OR left(idempotency_key,length(?))=?)", Long.class, owner, idempotencyKey, derivedKeyPrefix, derivedKeyPrefix) > 0) return inventory(owner);
        List<Lot> lots = jdbc.query("""
            SELECT id,fridge_item_id,quantity_remaining,unit,grams_per_unit,density_g_per_ml
            FROM cynexus_appl_life.food_fridge_stock_lot
            WHERE owner_key=? AND fridge_item_id=? AND deleted_at IS NULL AND status='active' AND quantity_remaining>0
            ORDER BY best_before,id FOR UPDATE
            """, this::lotRow, owner, parsed);
        if (lots.isEmpty()) throw new IllegalArgumentException("Fridge item has no active stock");
        BigDecimal requested = command.quantityDelta();
        if (command.unit() != null && !command.unit().equals(lots.get(0).unit())) throw new IllegalArgumentException("Adjustment unit must match the stored lot unit");
        if (requested.signum() > 0) {
            Lot lot = lots.get(0);
            applyDelta(owner, lot, requested, command, command.idempotencyKey(), "add");
        } else {
            BigDecimal remaining = requested.abs();
            int index = 0;
            while (remaining.signum() > 0 && index < lots.size()) {
                Lot lot = lots.get(index++);
                BigDecimal used = remaining.min(lot.quantity());
                applyDelta(owner, lot, used.negate(), command, command.idempotencyKey()+":"+lot.id(), "remove");
                remaining = remaining.subtract(used);
            }
            if (remaining.signum() > 0) throw new IllegalArgumentException("Quantity exceeds current fridge stock");
        }
        return inventory(owner);
    }

    @Override
    public Inventory confirmMapping(String owner, String itemId, String candidateId, String kind) {
        long fridgeItemId = parseId(itemId);
        long foodId = parseId(candidateId);
        FridgeIdentity identity = jdbc.query("""
            SELECT id,name,brand,normalized_name
            FROM cynexus_appl_life.food_fridge_item
            WHERE owner_key=? AND id=? AND deleted_at IS NULL
            FOR UPDATE
            """, (r, n) -> new FridgeIdentity(r.getLong("id"), null, null, r.getString("name"),
                r.getString("brand"), r.getString("normalized_name")), owner, fridgeItemId)
            .stream().findFirst().orElse(null);
        if (identity == null) throw new IllegalArgumentException("Fridge item was not found");

        Mapping mapping;
        if ("catalog".equals(kind)) {
            boolean exists = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM cynexus_appl_life.food_nutrition_catalog
                              WHERE id=? AND deleted_at IS NULL AND nutrition_usable=true)
                """, Boolean.class, foodId);
            if (!exists) throw new IllegalArgumentException("Food catalog candidate was not found");
            mapping = new Mapping(foodId, null, "user_confirmed", BigDecimal.ONE, "[]");
        } else if ("custom".equals(kind)) {
            boolean exists = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM cynexus_appl_life.food_custom
                              WHERE owner_key=? AND id=? AND deleted_at IS NULL)
                """, Boolean.class, owner, foodId);
            if (!exists) throw new IllegalArgumentException("Custom food candidate was not found");
            mapping = new Mapping(null, foodId, "user_confirmed", BigDecimal.ONE, "[]");
        } else {
            throw new IllegalArgumentException("Food candidate kind must be catalog or custom");
        }

        List<Long> existingAliases = jdbc.query("""
            SELECT id FROM cynexus_appl_life.food_fridge_mapping
            WHERE owner_key=? AND normalized_name=?
              AND lower(btrim(brand)) IS NOT DISTINCT FROM lower(btrim(?))
            ORDER BY deleted_at NULLS FIRST LIMIT 1 FOR UPDATE
            """, (r, n) -> r.getLong(1), owner, identity.normalizedName(), identity.brand());
        if (existingAliases.isEmpty()) {
            jdbc.update("""
                INSERT INTO cynexus_appl_life.food_fridge_mapping
                    (id,owner_key,normalized_name,brand,catalog_food_id,custom_food_id)
                VALUES (?,?,?,?,?,?)
                """, ids.nextId(), owner, identity.normalizedName(), identity.brand(),
                mapping.catalogId(), mapping.customId());
        } else {
            jdbc.update("""
                UPDATE cynexus_appl_life.food_fridge_mapping
                SET catalog_food_id=?,custom_food_id=?,updated_at=now(),deleted_at=NULL
                WHERE owner_key=? AND id=?
                """, mapping.catalogId(), mapping.customId(), owner, existingAliases.get(0));
        }

        jdbc.update("""
            UPDATE cynexus_appl_life.food_fridge_item
            SET catalog_food_id=?,custom_food_id=?,mapping_status='user_confirmed',
                mapping_confidence=1,mapping_candidates='[]'::jsonb,updated_at=now()
            WHERE owner_key=? AND normalized_name=?
              AND lower(btrim(brand)) IS NOT DISTINCT FROM lower(btrim(?)) AND deleted_at IS NULL
            """, mapping.catalogId(), mapping.customId(), owner, identity.normalizedName(), identity.brand());
        jdbc.update("""
            UPDATE cynexus_appl_life.food_fridge_item i
            SET asset_catalog_key=COALESCE(am.catalog_key,i.asset_catalog_key),
                asset_key=COALESCE(am.asset_key,i.asset_key),updated_at=now()
            FROM (SELECT DISTINCT owner_key,normalized_name,brand
                  FROM cynexus_appl_life.food_fridge_item
                  WHERE owner_key=? AND normalized_name=?
                    AND lower(btrim(brand)) IS NOT DISTINCT FROM lower(btrim(?)) AND deleted_at IS NULL) selected
            LEFT JOIN LATERAL (
                SELECT candidate.catalog_key,candidate.asset_key
                FROM (
                    SELECT a.catalog_key,a.asset_key,0 AS priority
                    FROM cynexus_appl_life.food_asset_catalog_mapping relation
                    JOIN cynexus_appl_life.food_asset_mapping a
                      ON a.catalog_key=relation.catalog_key AND a.deleted_at IS NULL
                    WHERE relation.catalog_food_id=? AND relation.deleted_at IS NULL
                    UNION ALL
                    SELECT min(a.catalog_key),min(a.asset_key),1 AS priority
                    FROM cynexus_appl_life.food_asset_mapping a
                    WHERE a.deleted_at IS NULL AND a.catalog_food_id=?
                    HAVING count(DISTINCT a.catalog_key)=1
                    UNION ALL
                    SELECT min(a.catalog_key),min(a.asset_key),2 AS priority
                    FROM cynexus_appl_life.food_nutrition_catalog c
                    CROSS JOIN LATERAL unnest(ARRAY[c.description] || COALESCE(c.aliases,'{}'::text[])) food_name(name)
                    JOIN cynexus_appl_life.food_asset_mapping a ON a.deleted_at IS NULL
                    CROSS JOIN LATERAL unnest(ARRAY[a.canonical_name,a.english_name] || COALESCE(a.aliases,'{}'::text[])) asset_name(name)
                    WHERE c.id=? AND c.deleted_at IS NULL
                      AND lower(regexp_replace(btrim(food_name.name), '\\s+', ' ', 'g'))=
                          lower(regexp_replace(btrim(asset_name.name), '\\s+', ' ', 'g'))
                    HAVING count(DISTINCT a.catalog_key)=1
                    UNION ALL
                    SELECT min(a.catalog_key),min(a.asset_key),3 AS priority
                    FROM cynexus_appl_life.food_asset_mapping a
                    WHERE a.deleted_at IS NULL
                      AND EXISTS (
                          SELECT 1
                          FROM unnest(ARRAY[a.canonical_name,a.english_name] || COALESCE(a.aliases,'{}'::text[])) asset_name(name)
                          WHERE lower(regexp_replace(btrim(asset_name.name), '\\s+', ' ', 'g'))=selected.normalized_name
                      )
                    HAVING count(DISTINCT a.catalog_key)=1
                ) candidate
                ORDER BY candidate.priority,candidate.catalog_key
                LIMIT 1
            ) am ON true
            WHERE i.owner_key=selected.owner_key AND i.normalized_name=selected.normalized_name
              AND lower(btrim(i.brand)) IS NOT DISTINCT FROM lower(btrim(selected.brand))
              AND i.deleted_at IS NULL
            """, owner, identity.normalizedName(), identity.brand(), mapping.catalogId(),
            mapping.catalogId(), mapping.catalogId());
        if (mapping.catalogId() != null) {
            jdbc.update("""
                INSERT INTO cynexus_appl_life.food_asset_catalog_mapping
                    (catalog_food_id,catalog_key,association_source)
                SELECT ?, resolved.catalog_key, 'fridge_confirmation'
                FROM (
                    SELECT min(i.asset_catalog_key) AS catalog_key
                    FROM cynexus_appl_life.food_fridge_item i
                    WHERE i.owner_key=? AND i.normalized_name=?
                      AND lower(btrim(i.brand)) IS NOT DISTINCT FROM lower(btrim(?))
                      AND i.deleted_at IS NULL AND i.asset_catalog_key IS NOT NULL
                    HAVING count(DISTINCT i.asset_catalog_key)=1
                ) resolved
                WHERE resolved.catalog_key IS NOT NULL
                ON CONFLICT (catalog_food_id) DO UPDATE
                SET catalog_key=EXCLUDED.catalog_key,
                    association_source='fridge_confirmation',updated_at=now(),deleted_at=NULL
                """, foodId, owner, identity.normalizedName(), identity.brand());
        }
        Nutrients nutrients = nutrition(owner, mapping);
        String nutritionStatus = status(nutrients);
        String snapshot = snapshot(mapping, nutrients);
        jdbc.update("""
            UPDATE cynexus_appl_life.food_fridge_stock_lot
            SET nutrition_status=?,calories_per100g=?,carbs_per100g=?,protein_per100g=?,fat_per100g=?,
                nutrition_source_snapshot=?::jsonb,updated_at=now()
            WHERE owner_key=? AND deleted_at IS NULL AND status='active'
              AND quantity_remaining>0
              AND fridge_item_id IN (
                  SELECT id FROM cynexus_appl_life.food_fridge_item
                  WHERE owner_key=? AND normalized_name=?
                    AND lower(btrim(brand)) IS NOT DISTINCT FROM lower(btrim(?)) AND deleted_at IS NULL
              )
            """, nutritionStatus, nutrients.caloriesKcal(), nutrients.carbsG(), nutrients.proteinG(), nutrients.fatG(),
            snapshot, owner, owner, identity.normalizedName(), identity.brand());
        return inventory(owner);
    }

    @Override
    public MappingCandidate findConfirmedMapping(String owner, String name, String brand) {
        String normalizedName = normalize(name);
        return jdbc.query("""
            SELECT CASE WHEN m.catalog_food_id IS NOT NULL THEN m.catalog_food_id::text ELSE m.custom_food_id::text END AS id,
                   CASE WHEN m.catalog_food_id IS NOT NULL THEN 'catalog' ELSE 'custom' END AS kind,
                   COALESCE(c.description, f.name) AS name, f.brand,
                   1.0 AS confidence,
                   COALESCE(c.calories_kcal, f.calories_kcal) AS calories_kcal,
                   COALESCE(c.carbs_g, f.carbs_g) AS carbs_g,
                   COALESCE(c.protein_g, f.protein_g) AS protein_g,
                   COALESCE(c.fat_g, f.fat_g) AS fat_g
            FROM cynexus_appl_life.food_fridge_mapping m
            LEFT JOIN cynexus_appl_life.food_nutrition_catalog c
              ON c.id=m.catalog_food_id AND c.deleted_at IS NULL AND c.nutrition_usable=true
            LEFT JOIN cynexus_appl_life.food_custom f
              ON f.owner_key=m.owner_key AND f.id=m.custom_food_id AND f.deleted_at IS NULL
            WHERE m.owner_key=? AND m.normalized_name=?
              AND lower(btrim(m.brand)) IS NOT DISTINCT FROM lower(btrim(?))
              AND m.deleted_at IS NULL AND (c.id IS NOT NULL OR f.id IS NOT NULL)
            ORDER BY m.updated_at DESC,m.id DESC LIMIT 1
            """, this::candidateRow, owner, normalizedName, brand).stream().findFirst().orElse(null);
    }

    private void applyDelta(String owner, Lot lot, BigDecimal delta, AdjustCommand command, String key, String eventKind) {
        BigDecimal after = lot.quantity().add(delta);
        String status = after.signum() == 0 ? "depleted" : "active";
        jdbc.update("UPDATE "+S+"food_fridge_stock_lot SET quantity_remaining=?,status=?,updated_at=now() WHERE owner_key=? AND id=? AND deleted_at IS NULL", after, status, owner, lot.id());
        long eventId = ids.nextId();
        jdbc.update("""
            INSERT INTO cynexus_appl_life.food_fridge_stock_event
              (id,owner_key,fridge_item_id,lot_id,event_kind,quantity_delta,unit,grams_delta,
               quantity_before,quantity_after,source,idempotency_key)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """, eventId, owner, lot.itemId(), lot.id(), eventKind, delta, lot.unit(), grams(delta, lot.unit(), lot.gramsPerUnit(), lot.density()), lot.quantity(), after,
            command.source() == null || command.source().isBlank() ? "user" : command.source(), key);
        if ("meal".equals(command.source()) && command.mealItemId() != null && !command.mealItemId().isBlank()) {
            long mealItemId = parseId(command.mealItemId());
            jdbc.update("""
                INSERT INTO cynexus_appl_life.food_meal_item_fridge_lot
                  (id,owner_key,meal_item_id,lot_id,event_id,quantity,unit,consumed_grams,idempotency_key)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, ids.nextId(), owner, mealItemId, lot.id(), eventId, delta.abs(), lot.unit(), grams(delta.abs(), lot.unit(), lot.gramsPerUnit(), lot.density()), key+":allocation");
        }
    }

    @Override
    public List<MappingCandidate> searchMapping(String owner, String query, int limit) {
        String pattern = "%"+escape(query.toLowerCase())+"%";
        List<MappingCandidate> values = new ArrayList<>(jdbc.query("""
            SELECT c.id::text AS id,'catalog' AS kind,c.description AS name,NULL::text AS brand,
                   c.calories_kcal,c.carbs_g,c.protein_g,c.fat_g,
                   CASE WHEN lower(btrim(c.description))=lower(btrim(?))
                              OR EXISTS (SELECT 1 FROM unnest(c.aliases) a WHERE lower(btrim(a))=lower(btrim(?)))
                        THEN 1.0 ELSE 0.65 END AS confidence
            FROM cynexus_appl_life.food_nutrition_catalog c
            WHERE c.deleted_at IS NULL AND c.nutrition_usable=true
              AND (lower(c.description) LIKE ? ESCAPE '\\' OR EXISTS (SELECT 1 FROM unnest(c.aliases) a WHERE lower(a) LIKE ? ESCAPE '\\'))
            ORDER BY confidence DESC,c.description,c.id LIMIT ?
            """, this::candidateRow, query, query, pattern, pattern, limit));
        values.addAll(jdbc.query("""
            SELECT c.id::text,'custom',c.name,c.brand,c.calories_kcal,c.carbs_g,c.protein_g,c.fat_g,
                   CASE WHEN lower(btrim(c.name))=lower(btrim(?)) THEN 1.0 ELSE 0.65 END AS confidence
            FROM cynexus_appl_life.food_custom c
            WHERE c.owner_key=? AND c.deleted_at IS NULL AND lower(c.name) LIKE ? ESCAPE '\\'
            ORDER BY confidence DESC,c.name,c.id LIMIT ?
            """, this::candidateRow, query, owner, pattern, limit));
        return values.stream()
                .sorted(java.util.Comparator.comparing(MappingCandidate::confidence,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(MappingCandidate::name, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(MappingCandidate::kind))
                .limit(limit)
                .toList();
    }

    @Override
    public List<AssetMapping> assetMappings() {
        return jdbc.query("""
            SELECT catalog_key,canonical_name,english_name,aliases,asset_key,asset_url,category
            FROM cynexus_appl_life.food_asset_mapping
            WHERE deleted_at IS NULL ORDER BY catalog_key
            """, (r, n) -> new AssetMapping(r.getString("catalog_key"), r.getString("canonical_name"),
                r.getString("english_name"), java.util.Arrays.asList((String[]) r.getArray("aliases").getArray()),
                r.getString("asset_key"), r.getString("asset_url"), r.getString("category")));
    }

    @Override
    public AssetMapping assetMapping(String catalogKey) {
        return jdbc.query("""
            SELECT catalog_key,canonical_name,english_name,aliases,asset_key,asset_url,category
            FROM cynexus_appl_life.food_asset_mapping
            WHERE catalog_key=? AND deleted_at IS NULL
            """, (r, n) -> new AssetMapping(r.getString("catalog_key"), r.getString("canonical_name"),
                r.getString("english_name"), java.util.Arrays.asList((String[]) r.getArray("aliases").getArray()),
                r.getString("asset_key"), r.getString("asset_url"), r.getString("category")), catalogKey).stream().findFirst().orElse(null);
    }

    private Mapping resolveMapping(String owner, AddCommand command) {
        if (command.mappingId() != null && !command.mappingId().isBlank()) {
            long mappingId = parseId(command.mappingId());
            Mapping confirmed = jdbc.query("SELECT catalog_food_id,custom_food_id FROM "+S+"food_fridge_mapping WHERE owner_key=? AND id=? AND deleted_at IS NULL", (r,n) -> new Mapping((Long)r.getObject("catalog_food_id"), (Long)r.getObject("custom_food_id"), "user_confirmed", BigDecimal.ONE, "[]"), owner, mappingId).stream().findFirst().orElse(null);
            if (confirmed == null) throw new IllegalArgumentException("Food mapping was not found");
            return confirmed;
        }
        if (command.catalogFoodId() != null) { parseId(command.catalogFoodId()); return new Mapping(parseId(command.catalogFoodId()), null, "matched", BigDecimal.ONE, "[]"); }
        if (command.customFoodId() != null) { parseId(command.customFoodId()); return new Mapping(null, parseId(command.customFoodId()), "user_confirmed", BigDecimal.ONE, "[]"); }
        if (command.mappingResolution() != null) {
            MappingResolution resolution = command.mappingResolution();
            Long catalogId = resolution.catalogFoodId() == null ? null : parseId(resolution.catalogFoodId());
            Long customId = resolution.customFoodId() == null ? null : parseId(resolution.customFoodId());
            return new Mapping(catalogId, customId, resolution.status(), resolution.confidence(),
                    candidatesJson(resolution.candidates() == null ? List.of() : resolution.candidates()));
        }
        List<MappingCandidate> candidates = searchMapping(owner, command.name(), 5);
        List<MappingCandidate> exact = candidates.stream().filter(c -> BigDecimal.ONE.compareTo(c.confidence()) == 0).toList();
        if (exact.size() == 1) {
            MappingCandidate c = exact.get(0);
            return new Mapping("catalog".equals(c.kind()) ? parseId(c.id()) : null, "custom".equals(c.kind()) ? parseId(c.id()) : null, "catalog".equals(c.kind()) ? "matched" : "user_confirmed", c.confidence(), "[]");
        }
        return new Mapping(null, null, candidates.isEmpty() ? "unmatched" : "pending", candidates.isEmpty() ? null : candidates.get(0).confidence(), candidatesJson(candidates));
    }

    private Nutrients nutrition(String owner, Mapping mapping) {
        if (mapping.catalogId() != null) return jdbc.query("SELECT calories_kcal,carbs_g,protein_g,fat_g FROM "+S+"food_nutrition_catalog WHERE id=? AND deleted_at IS NULL", (r,n) -> nutrients(r), mapping.catalogId()).stream().findFirst().orElse(Nutrients.unknown());
        if (mapping.customId() != null) return jdbc.query("SELECT calories_kcal,carbs_g,protein_g,fat_g FROM "+S+"food_custom WHERE owner_key=? AND id=? AND deleted_at IS NULL", (r,n) -> nutrients(r), owner, mapping.customId()).stream().findFirst().orElse(Nutrients.unknown());
        return Nutrients.unknown();
    }

    private Item itemRow(ResultSet r, int row) throws SQLException {
        BigDecimal quantity = r.getBigDecimal("display_quantity");
        String unit = r.getString("display_unit");
        if (quantity == null) { quantity = r.getBigDecimal("total_quantity_grams"); unit = "g"; }
        String status = mapStatus(r.getString("mapping_status"));
        BigDecimal grams = r.getBigDecimal("total_quantity_grams");
        Nutrients nutrition = new Nutrients(r.getBigDecimal("known_calories_kcal"), r.getBigDecimal("known_carbs_g"), r.getBigDecimal("known_protein_g"), r.getBigDecimal("known_fat_g"));
        String nutritionStatus = r.getInt("missing_weight_count") > 0 ? "unavailable" : (r.getInt("missing_calories_count") + r.getInt("missing_carbs_count") + r.getInt("missing_protein_count") + r.getInt("missing_fat_count") == 0 ? "complete" : "partial");
        return new Item(r.getString("fridge_item_id"), nullableId(r.getLong("catalog_food_id")), nullableId(r.getLong("custom_food_id")), r.getString("name"), quantity, unit, r.getObject("earliest_best_before", LocalDate.class), instant(r, "item_created_at"), instant(r, "item_updated_at"), status, parseCandidates(r.getString("mapping_candidates")), r.getBigDecimal("mapping_confidence"), grams, r.getBigDecimal("grams_per_unit"), nutrition, nutritionStatus, r.getInt("visual_stack_count"), overflow(quantity, r.getInt("visual_stack_count")), r.getString("asset_catalog_key"), r.getString("asset_key"), r.getString("asset_url"));
    }
    private Operation operationRow(ResultSet r, int row) throws SQLException { return new Operation(r.getString("id"), r.getString("fridge_item_id"), r.getString("name"), "add".equals(r.getString("event_kind")) ? "add" : "remove".equals(r.getString("event_kind")) ? "remove" : "update", r.getBigDecimal("quantity_delta"), r.getBigDecimal("quantity_after"), instant(r, "occurred_at")); }
    private Lot lotRow(ResultSet r, int row) throws SQLException { return new Lot(r.getLong("id"), r.getLong("fridge_item_id"), r.getBigDecimal("quantity_remaining"), r.getString("unit"), r.getBigDecimal("grams_per_unit"), r.getBigDecimal("density_g_per_ml")); }
    private static String derivedKey(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }
    private MappingCandidate candidateRow(ResultSet r, int row) throws SQLException { return new MappingCandidate(r.getString("id"), r.getString("kind"), r.getString("name"), r.getString("brand"), r.getBigDecimal("confidence"), nutrients(r)); }
    private Nutrients nutrients(ResultSet r) throws SQLException { return new Nutrients(r.getBigDecimal("calories_kcal"), r.getBigDecimal("carbs_g"), r.getBigDecimal("protein_g"), r.getBigDecimal("fat_g")); }
    private List<MappingCandidate> parseCandidates(String raw) { List<MappingCandidate> result = new ArrayList<>(); try { JsonNode node = json.readTree(raw == null ? "[]" : raw); if (node.isArray()) for (JsonNode c : node) result.add(new MappingCandidate(c.path("id").asText(), c.path("kind").asText(), c.path("name").asText(), c.path("brand").isNull() ? null : c.path("brand").asText(), c.path("confidence").isNumber() ? c.path("confidence").decimalValue() : null, null)); } catch (Exception ignored) {} return result; }
    private String candidatesJson(List<MappingCandidate> candidates) { try { return json.writeValueAsString(candidates); } catch (Exception e) { return "[]"; } }
    private String snapshot(Mapping mapping, Nutrients nutrients) { try { return json.writeValueAsString(java.util.Map.of("catalogFoodId", mapping.catalogId() == null ? "" : mapping.catalogId(), "customFoodId", mapping.customId() == null ? "" : mapping.customId(), "caloriesKcal", nutrients.caloriesKcal() == null ? "" : nutrients.caloriesKcal())); } catch (Exception e) { return "{}"; } }
    private String unitPolicy(AddCommand c) { return "{}"; }
    private String status(Nutrients n) { int known = (int)n.values().stream().filter(java.util.Objects::nonNull).count(); return known == 4 ? "known" : known == 0 ? "pending" : "partial"; }
    private static boolean directWeight(String u) { return "mg".equals(u) || "g".equals(u) || "kg".equals(u); }
    private static boolean volumeUnit(String u) { return "ml".equals(u) || "L".equals(u); }
    private static BigDecimal grams(BigDecimal q, String unit, BigDecimal gpu, BigDecimal density) { if (q == null) return null; return switch (unit) { case "mg" -> q.movePointLeft(3); case "g" -> q; case "kg" -> q.multiply(BigDecimal.valueOf(1000)); case "ml" -> density == null ? null : q.multiply(density); case "L" -> density == null ? null : q.multiply(density).multiply(BigDecimal.valueOf(1000)); default -> gpu == null ? null : q.multiply(gpu); }; }
    private static String mapStatus(String status) { return "matched".equals(status) ? "matched" : "user_confirmed".equals(status) ? "user_confirmed" : "unmatched".equals(status) ? "unavailable" : "needs_mapping"; }
    private static String normalize(String value) { return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
    private static long parseId(String value) { try { long id = Long.parseLong(value); if (id > 0) return id; } catch (RuntimeException ignored) {} throw new IllegalArgumentException("Invalid food resource id"); }
    private static String nullableId(long value) { return value == 0 ? null : Long.toString(value); }
    private static Instant instant(ResultSet r, String column) throws SQLException { java.sql.Timestamp timestamp = r.getTimestamp(column); return timestamp == null ? null : timestamp.toInstant(); }
    private static BigDecimal overflow(BigDecimal quantity, int stack) { return quantity == null ? BigDecimal.ZERO : quantity.subtract(BigDecimal.valueOf(stack)).max(BigDecimal.ZERO); }
    private record Mapping(Long catalogId, Long customId, String status, BigDecimal confidence, String candidatesJson) {}
    private record Lot(long id, long itemId, BigDecimal quantity, String unit, BigDecimal gramsPerUnit, BigDecimal density) {}
    private record FridgeIdentity(long id, String catalogFoodId, String customFoodId, String name,
                                  String brand, String normalizedName) {}
    private record MealLotRow(long id, BigDecimal quantity, String unit, BigDecimal gramsPerUnit,
                              BigDecimal density, FoodModels.Nutrients nutrients) {}
    private record MealAllocationRow(long allocationId, long lotId, BigDecimal quantity, String unit,
                                     BigDecimal consumedGrams, BigDecimal quantityRemaining, String status) {}
}
