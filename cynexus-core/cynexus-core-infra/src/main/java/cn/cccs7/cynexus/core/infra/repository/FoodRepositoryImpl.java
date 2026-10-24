package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.core.domain.food.FoodRepository;
import cn.cccs7.cynexus.core.domain.food.FoodModels.*;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FoodRepositoryImpl implements FoodRepository {
    private static final String S = "cynexus_appl_life.";
    private final JdbcTemplate jdbc;
    private final SnowflakeIdGenerator ids;
    public FoodRepositoryImpl(JdbcTemplate jdbc, SnowflakeIdGenerator ids) { this.jdbc = jdbc; this.ids = ids; }
    public void lockOwner(String owner) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 98127))", rs -> {}, owner);
    }
    public List<FoodAsset> assets() {
        return jdbc.query("SELECT catalog_key,catalog_food_id::text AS catalog_food_id,asset_key,asset_url,source_atlas,asset_row,asset_column,printed_label FROM " + S + "food_asset_mapping WHERE deleted_at IS NULL ORDER BY catalog_key",
                (r, n) -> new FoodAsset(r.getString("catalog_key"), r.getString("catalog_food_id"), r.getString("asset_key"),
                        r.getString("asset_url"), r.getString("source_atlas"),
                        (Integer) r.getObject("asset_row"), (Integer) r.getObject("asset_column"),
                        (Integer) r.getObject("printed_label")));
    }
    public FoodAsset asset(String key) {
        return jdbc.query("SELECT catalog_key,catalog_food_id::text AS catalog_food_id,asset_key,asset_url,source_atlas,asset_row,asset_column,printed_label FROM " + S + "food_asset_mapping WHERE deleted_at IS NULL AND (catalog_key=? OR asset_key=?) ORDER BY catalog_key LIMIT 1",
                (r, n) -> new FoodAsset(r.getString("catalog_key"), r.getString("catalog_food_id"), r.getString("asset_key"),
                        r.getString("asset_url"), r.getString("source_atlas"),
                        (Integer) r.getObject("asset_row"), (Integer) r.getObject("asset_column"),
                        (Integer) r.getObject("printed_label")), key, key).stream().findFirst().orElse(null);
    }
    private static long id(String id) {
        try { long value = Long.parseLong(id); if (value > 0) return value; }
        catch (RuntimeException ignored) { }
        throw new IllegalArgumentException("Invalid food resource id");
    }
    private String catalogSql() {
        return """
            SELECT c.id, 'catalog' AS kind, c.description AS name, NULL::text AS brand,
              c.calories_kcal,c.carbs_g,c.protein_g,c.fat_g,
              s.dataset_name AS source,c.license,c.usage_scope,c.is_complete,
              COALESCE(linked_asset.asset_key,legacy_asset.asset_key,matched_asset.asset_key) AS asset_key,
              CASE WHEN linked_asset.catalog_key IS NOT NULL THEN linked_asset.asset_url
                   WHEN legacy_asset.catalog_key IS NOT NULL THEN legacy_asset.asset_url
                   ELSE matched_asset.asset_url END AS asset_url,
              (SELECT f.id FROM cynexus_appl_life.food_favorite f WHERE f.owner_key=? AND f.catalog_food_id=c.id AND f.deleted_at IS NULL) AS favorite_id,
              jsonb_build_object('sourceKey',s.source_key,'datasetName',s.dataset_name,
                'sourceFoodId',c.source_food_id,'sampleOnly',s.sample_only,'sourceMetadata',s.metadata,
                'license',c.license,'usageScope',c.usage_scope,'estimatedFields',c.estimated_fields,
                'importBatchId',c.import_batch_id::text,
                'assetKey',COALESCE(linked_asset.asset_key,legacy_asset.asset_key,matched_asset.asset_key),
                'assetUrl',CASE WHEN linked_asset.catalog_key IS NOT NULL THEN linked_asset.asset_url
                                WHEN legacy_asset.catalog_key IS NOT NULL THEN legacy_asset.asset_url
                                ELSE matched_asset.asset_url END) AS source_snapshot
            FROM cynexus_appl_life.food_nutrition_catalog c
            JOIN cynexus_appl_life.food_nutrition_source s ON s.id=c.source_id AND s.deleted_at IS NULL
            LEFT JOIN LATERAL (
              SELECT candidate.catalog_key,candidate.asset_key,candidate.asset_url
              FROM (
                SELECT min(m.catalog_key) AS catalog_key,min(m.asset_key) AS asset_key,
                       min(m.asset_url) AS asset_url,0 AS priority
                FROM cynexus_appl_life.food_asset_mapping m
                WHERE m.deleted_at IS NULL AND m.catalog_food_id=c.id
                HAVING count(DISTINCT m.catalog_key)=1
                UNION ALL
                SELECT m.catalog_key,m.asset_key,m.asset_url,1 AS priority
                FROM cynexus_appl_life.food_asset_mapping m
                WHERE m.deleted_at IS NULL
                  AND m.catalog_key=COALESCE(c.raw_record->>'asset_key',c.raw_record->'metadata'->>'asset_key')
              ) candidate
              ORDER BY candidate.priority,candidate.catalog_key
              LIMIT 1
            ) legacy_asset ON TRUE
            LEFT JOIN LATERAL (
              SELECT m.catalog_key,m.asset_key,m.asset_url
              FROM cynexus_appl_life.food_asset_catalog_mapping relation
              JOIN cynexus_appl_life.food_asset_mapping m
                ON m.catalog_key=relation.catalog_key AND m.deleted_at IS NULL
              WHERE relation.catalog_food_id=c.id AND relation.deleted_at IS NULL
              LIMIT 1
            ) linked_asset ON TRUE
            LEFT JOIN LATERAL (
              SELECT min(m.catalog_key) AS catalog_key,min(m.asset_key) AS asset_key,
                     min(m.asset_url) AS asset_url
              FROM cynexus_appl_life.food_asset_mapping m
              WHERE m.deleted_at IS NULL
                AND EXISTS (
                  SELECT 1
                  FROM unnest(ARRAY[c.description] || COALESCE(c.aliases,'{}'::text[])) food_name
                  CROSS JOIN LATERAL unnest(ARRAY[m.canonical_name,m.english_name] || COALESCE(m.aliases,'{}'::text[])) asset_name
                  WHERE lower(regexp_replace(btrim(food_name), '\\s+', ' ', 'g')) =
                        lower(regexp_replace(btrim(asset_name), '\\s+', ' ', 'g'))
                )
              HAVING count(DISTINCT m.catalog_key)=1
            ) matched_asset ON TRUE
            WHERE c.deleted_at IS NULL AND c.nutrition_usable=true
            """;
    }
    private String customSql() {
        return """
            SELECT c.id,'custom' AS kind,c.name,c.brand,c.calories_kcal,c.carbs_g,c.protein_g,c.fat_g,
              'Owner entered'::text AS source,NULL::text AS license,NULL::text AS usage_scope,
              COALESCE(am.asset_key,matched_asset.asset_key) AS asset_key,
              CASE WHEN ca.custom_food_id IS NOT NULL THEN '/api/core/food/custom-foods/' || c.id::text || '/asset'
                   WHEN am.catalog_key IS NOT NULL THEN am.asset_url ELSE matched_asset.asset_url END AS asset_url,
              (num_nonnulls(c.calories_kcal,c.carbs_g,c.protein_g,c.fat_g)=4) AS is_complete,
              (SELECT f.id FROM cynexus_appl_life.food_favorite f WHERE f.owner_key=c.owner_key AND f.custom_food_id=c.id AND f.deleted_at IS NULL) AS favorite_id,
              jsonb_build_object('customFoodId',c.id::text,'provenance',c.provenance,
                'assetKey',COALESCE(am.asset_key,matched_asset.asset_key),
                'assetUrl',CASE WHEN ca.custom_food_id IS NOT NULL THEN '/api/core/food/custom-foods/' || c.id::text || '/asset'
                               WHEN am.catalog_key IS NOT NULL THEN am.asset_url ELSE matched_asset.asset_url END) AS source_snapshot
            FROM cynexus_appl_life.food_custom c
            LEFT JOIN cynexus_appl_life.food_custom_asset ca
              ON ca.owner_key=c.owner_key AND ca.custom_food_id=c.id AND ca.deleted_at IS NULL
            LEFT JOIN LATERAL (
              SELECT m.catalog_key,m.asset_key,m.asset_url
              FROM cynexus_appl_life.food_asset_mapping m
              WHERE m.deleted_at IS NULL AND m.catalog_key=c.metadata->>'asset_key'
              LIMIT 1
            ) am ON TRUE
            LEFT JOIN LATERAL (
              SELECT min(m.catalog_key) AS catalog_key,min(m.asset_key) AS asset_key,
                     min(m.asset_url) AS asset_url
              FROM cynexus_appl_life.food_asset_mapping m
              WHERE m.deleted_at IS NULL
                AND EXISTS (
                  SELECT 1
                  FROM unnest(ARRAY[c.name]) food_name
                  CROSS JOIN LATERAL unnest(ARRAY[m.canonical_name,m.english_name] || COALESCE(m.aliases,'{}'::text[])) asset_name
                  WHERE lower(regexp_replace(btrim(food_name), '\\s+', ' ', 'g')) =
                        lower(regexp_replace(btrim(asset_name), '\\s+', ' ', 'g'))
                )
              HAVING count(DISTINCT m.catalog_key)=1
            ) matched_asset ON TRUE
            WHERE c.owner_key=? AND c.deleted_at IS NULL
            """;
    }
    public List<Food> search(String owner, String query, int limit) {
        String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        // Each source is bounded independently, then merged deterministically. JDBC timeout also bounds scans.
        List<Food> custom = jdbc.query(customSql()+" AND c.name ILIKE ? ORDER BY c.name,c.id LIMIT ?", this::foodRow, owner, pattern, limit);
        List<Food> catalog = jdbc.query(connection -> {
            var statement = connection.prepareStatement(catalogSql()+" AND (c.description ILIKE ? OR EXISTS (SELECT 1 FROM unnest(c.aliases) a WHERE a ILIKE ?)) ORDER BY c.is_complete DESC,c.id LIMIT ?");
            statement.setQueryTimeout(5); statement.setString(1,owner); statement.setString(2,pattern); statement.setString(3,pattern); statement.setInt(4,limit);
            return statement;
        }, this::foodRow);
        return java.util.stream.Stream.concat(custom.stream(), catalog.stream()).limit(limit).toList();
    }
    public List<Food> customFoods(String owner) { return jdbc.query(customSql()+" ORDER BY c.created_at DESC,c.id DESC LIMIT 200",this::foodRow,owner); }
    public List<Food> favorites(String owner) {
        List<Food> custom = jdbc.query(customSql()+" AND EXISTS (SELECT 1 FROM "+S+"food_favorite f WHERE f.owner_key=c.owner_key AND f.custom_food_id=c.id AND f.deleted_at IS NULL) ORDER BY c.name,c.id LIMIT 100",this::foodRow,owner);
        List<Food> catalog = jdbc.query(catalogSql()+" AND EXISTS (SELECT 1 FROM "+S+"food_favorite f WHERE f.owner_key=? AND f.catalog_food_id=c.id AND f.deleted_at IS NULL) ORDER BY c.description,c.id LIMIT 100",this::foodRow,owner,owner);
        return java.util.stream.Stream.concat(custom.stream(),catalog.stream()).limit(100).toList();
    }
    public Food food(String owner,String kind,String foodId) {
        if ("catalog".equals(kind)) return jdbc.query(catalogSql()+" AND c.id=?",this::foodRow,owner,id(foodId)).stream().findFirst().orElse(null);
        if ("custom".equals(kind)) return jdbc.query(customSql()+" AND c.id=?",this::foodRow,owner,id(foodId)).stream().findFirst().orElse(null);
        throw new IllegalArgumentException("Invalid food kind");
    }
    public byte[] customAsset(String owner, String foodId) {
        return jdbc.query("SELECT png_bytes FROM " + S + "food_custom_asset WHERE owner_key=? AND custom_food_id=? AND deleted_at IS NULL",
                (r, n) -> r.getBytes("png_bytes"), owner, id(foodId)).stream().findFirst().orElse(null);
    }
    public DayState day(String owner,LocalDate date) {
        return jdbc.query("SELECT * FROM "+S+"food_diary_day WHERE owner_key=? AND diary_date=? AND deleted_at IS NULL",(r,n)->new DayState(r.getString("id"),date,r.getString("status"),r.getString("timezone"),nutrients(r,"target_")),owner,date).stream().findFirst().orElse(null);
    }
    public DayState createDay(String owner,LocalDate date,String timezone,Goal goal) {
        Nutrients n=goal==null?Nutrients.unknown():goal.nutrients();
        jdbc.update("INSERT INTO "+S+"food_diary_day (id,owner_key,diary_date,timezone,goal_history_id,target_calories_kcal,target_carbs_g,target_protein_g,target_fat_g,idempotency_key) VALUES (?,?,?,?,?,?,?,?,?,?)",
                ids.nextId(),owner,date,timezone,goal==null?null:id(goal.id()),n.caloriesKcal(),n.carbsG(),n.proteinG(),n.fatG(),"day:"+date);
        return day(owner,date);
    }
    private String itemSql() {
        return "SELECT i.*,m.meal_type FROM "+S+"food_meal_item i JOIN "+S+"food_meal m ON m.id=i.meal_id AND m.owner_key=i.owner_key WHERE i.owner_key=? AND m.diary_day_id=? AND m.deleted_at IS NULL AND i.deleted_at IS NULL";
    }
    public List<Item> items(String owner,String dayId) { return jdbc.query(itemSql()+" ORDER BY m.sort_order,m.id,i.sort_order,i.id LIMIT 201",this::itemRow,owner,id(dayId)); }
    public Item item(String owner,String dayId,String itemId) { return jdbc.query(itemSql()+" AND i.id=?",this::itemRow,owner,id(dayId),id(itemId)).stream().findFirst().orElse(null); }
    public void insertItem(String owner,String dayId,Item i,String key,String fingerprint,String sourceSnapshot) {
        long mealId=ids.nextId();
        jdbc.update("INSERT INTO "+S+"food_meal (id,owner_key,diary_day_id,meal_type,idempotency_key) VALUES (?,?,?,?,?)",mealId,owner,id(dayId),i.mealType(),"item:"+fingerprint);
        boolean quick="quick".equals(i.entryKind());
        Nutrients p=i.per100g(),q=quick?i.nutrients():Nutrients.unknown();
        jdbc.update("INSERT INTO "+S+"food_meal_item (id,owner_key,meal_id,entry_kind,catalog_food_id,custom_food_id,food_name,brand,quantity,unit,grams_per_unit,calories_per100g,carbs_per100g,protein_per100g,fat_per100g,quick_calories_kcal,quick_carbs_g,quick_protein_g,quick_fat_g,idempotency_key,metadata,source_snapshot) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text),?::jsonb)",
                ids.nextId(),owner,mealId,i.entryKind(),"catalog".equals(i.entryKind())?id(i.foodId()):null,"custom".equals(i.entryKind())?id(i.foodId()):null,i.foodName(),i.brand(),i.quantity(),i.unit(),i.gramsPerUnit(),p.caloriesKcal(),p.carbsG(),p.proteinG(),p.fatG(),q.caloriesKcal(),q.carbsG(),q.proteinG(),q.fatG(),key,fingerprint,sourceSnapshot);
    }
    public void updateItem(String owner,String dayId,Item i) {
        Nutrients q="quick".equals(i.entryKind())?i.nutrients():Nutrients.unknown();
        Nutrients p=i.per100g()==null?Nutrients.unknown():i.per100g();
        jdbc.update("UPDATE "+S+"food_meal_item SET quantity=?,unit=?,grams_per_unit=?,calories_per100g=?,carbs_per100g=?,protein_per100g=?,fat_per100g=?,quick_calories_kcal=?,quick_carbs_g=?,quick_protein_g=?,quick_fat_g=?,updated_at=now() WHERE owner_key=? AND id=? AND meal_id IN (SELECT id FROM "+S+"food_meal WHERE owner_key=? AND diary_day_id=? AND deleted_at IS NULL) AND deleted_at IS NULL",
                i.quantity(),i.unit(),i.gramsPerUnit(),p.caloriesKcal(),p.carbsG(),p.proteinG(),p.fatG(),q.caloriesKcal(),q.carbsG(),q.proteinG(),q.fatG(),owner,id(i.id()),owner,id(dayId));
        jdbc.update("UPDATE "+S+"food_meal SET meal_type=?,updated_at=now() WHERE owner_key=? AND id=(SELECT meal_id FROM "+S+"food_meal_item WHERE owner_key=? AND id=? AND deleted_at IS NULL) AND diary_day_id=? AND deleted_at IS NULL",
                i.mealType(),owner,owner,id(i.id()),id(dayId));
    }
    public void deleteItem(String owner,String dayId,String itemId) {
        jdbc.update("UPDATE "+S+"food_meal_item SET deleted_at=now(),updated_at=now() WHERE owner_key=? AND id=? AND meal_id IN (SELECT id FROM "+S+"food_meal WHERE owner_key=? AND diary_day_id=? AND deleted_at IS NULL) AND deleted_at IS NULL",owner,id(itemId),owner,id(dayId));
    }
    public void status(String owner,String dayId,String status) {
        jdbc.update("UPDATE "+S+"food_diary_day SET status=?,completed_at=CASE WHEN ?='complete' THEN COALESCE(completed_at,now()) ELSE NULL END,updated_at=now() WHERE owner_key=? AND id=? AND deleted_at IS NULL",status,status,owner,id(dayId));
    }
    public StoredRequest request(String owner,String resource,String key) {
        String table=switch(resource) {case "item" -> "food_meal_item"; case "goal" -> "food_goal_history"; case "custom" -> "food_custom"; case "favorite" -> "food_favorite"; default -> throw new IllegalArgumentException("Invalid resource");};
        return jdbc.query("SELECT id,metadata->>'requestFingerprint' AS fingerprint,deleted_at IS NOT NULL AS deleted FROM "+S+table+" WHERE owner_key=? AND idempotency_key=?",(r,n)->new StoredRequest(r.getString("id"),r.getString("fingerprint"),r.getBoolean("deleted")),owner,key).stream().findFirst().orElse(null);
    }
    public Goal effectiveGoal(String owner,LocalDate date) { return jdbc.query("SELECT * FROM "+S+"food_goal_history WHERE owner_key=? AND effective_from<=? AND deleted_at IS NULL ORDER BY effective_from DESC LIMIT 1",this::goalRow,owner,date).stream().findFirst().orElse(null); }
    public List<Goal> goals(String owner) { return jdbc.query("SELECT * FROM "+S+"food_goal_history WHERE owner_key=? AND deleted_at IS NULL ORDER BY effective_from DESC LIMIT 200",this::goalRow,owner); }
    public String saveGoal(String owner,LocalDate date,Nutrients n,String key,String fingerprint) {
        long id=ids.nextId();
        jdbc.update("INSERT INTO "+S+"food_goal_history (id,owner_key,effective_from,calories_kcal,carbs_g,protein_g,fat_g,idempotency_key,metadata) VALUES (?,?,?,?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text))",id,owner,date,n.caloriesKcal(),n.carbsG(),n.proteinG(),n.fatG(),key,fingerprint);
        return Long.toString(id);
    }
    public String saveCustom(String owner,String name,String brand,Nutrients n,String key,String fingerprint,String assetKey) {
        return saveCustom(owner,name,brand,n,key,fingerprint,assetKey,null,null);
    }
    public String saveCustom(String owner,String name,String brand,Nutrients n,String key,String fingerprint,String assetKey,
                             byte[] assetImage,String assetDigest) {
        long id=ids.nextId();
        jdbc.update("INSERT INTO "+S+"food_custom (id,owner_key,name,brand,calories_kcal,carbs_g,protein_g,fat_g,idempotency_key,metadata) VALUES (?,?,?,?,?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text,'asset_key',?::text))",id,owner,name,brand,n.caloriesKcal(),n.carbsG(),n.proteinG(),n.fatG(),key,fingerprint,assetKey);
        if (assetImage != null) {
            jdbc.update("INSERT INTO " + S + "food_custom_asset (owner_key,custom_food_id,content_type,png_bytes,sha256) VALUES (?,?,?,?,?)",
                    owner, id, "image/png", assetImage, assetDigest);
        }
        return Long.toString(id);
    }
    public String saveFavorite(String owner,String kind,String foodId,String key,String fingerprint) {
        long id=ids.nextId();
        jdbc.update("INSERT INTO "+S+"food_favorite (id,owner_key,catalog_food_id,custom_food_id,idempotency_key,metadata) VALUES (?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text))",id,owner,"catalog".equals(kind)?id(foodId):null,"custom".equals(kind)?id(foodId):null,key,fingerprint);
        return Long.toString(id);
    }
    public void deleteFavorite(String owner,String id) { jdbc.update("UPDATE "+S+"food_favorite SET deleted_at=now(),updated_at=now() WHERE owner_key=? AND id=? AND deleted_at IS NULL",owner,id(id)); }
    private Nutrients nutrients(ResultSet r,String prefix) throws SQLException { return new Nutrients(r.getBigDecimal(prefix+"calories_kcal"),r.getBigDecimal(prefix+"carbs_g"),r.getBigDecimal(prefix+"protein_g"),r.getBigDecimal(prefix+"fat_g")); }
    private Goal goalRow(ResultSet r,int row) throws SQLException { return new Goal(r.getString("id"),r.getObject("effective_from",LocalDate.class),nutrients(r,"")); }
    private Food foodRow(ResultSet r,int row) throws SQLException { return new Food(r.getString("id"),r.getString("kind"),r.getString("name"),r.getString("brand"),nutrients(r,""),r.getString("source"),r.getString("license"),r.getString("usage_scope"),r.getBoolean("is_complete"),r.getString("favorite_id"),r.getString("source_snapshot"),r.getString("asset_key"),r.getString("asset_url")); }
    private Item itemRow(ResultSet r,int row) throws SQLException {
        return new Item(r.getString("id"),r.getString("meal_type"),r.getString("entry_kind"),r.getString("catalog_food_id")==null?r.getString("custom_food_id"):r.getString("catalog_food_id"),r.getString("food_name"),r.getString("brand"),r.getBigDecimal("quantity"),r.getString("unit"),r.getBigDecimal("grams_per_unit"),r.getBigDecimal("consumed_grams"),new Nutrients(r.getBigDecimal("calories_per100g"),r.getBigDecimal("carbs_per100g"),r.getBigDecimal("protein_per100g"),r.getBigDecimal("fat_per100g")),nutrients(r,""),assetKey(r.getString("source_snapshot")),sourceFridgeItemId(r.getString("source_snapshot")),assetUrl(r.getString("source_snapshot")));
    }
    private String assetKey(String snapshot) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(snapshot).path("assetKey").textValue(); }
        catch (java.io.IOException ignored) { return null; }
    }
    private String assetUrl(String snapshot) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(snapshot).path("assetUrl").textValue(); }
        catch (java.io.IOException ignored) { return null; }
    }
    private String sourceFridgeItemId(String snapshot) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(snapshot).path("fridgeItemId").textValue(); }
        catch (java.io.IOException ignored) { return null; }
    }
}
