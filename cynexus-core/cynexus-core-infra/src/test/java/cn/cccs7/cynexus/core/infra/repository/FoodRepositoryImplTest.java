package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodRepositoryImplTest {
    @Test
    void assetKeyAndUrlAlwaysComeFromTheSameResolvedMapping() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FoodRepositoryImpl repository = new FoodRepositoryImpl(jdbc, (SnowflakeIdGenerator) null);

        repository.favorites("owner");

        String catalogSql = jdbc.queries.stream()
                .filter(sql -> sql.contains("food_nutrition_catalog c"))
                .findFirst().orElseThrow();
        assertTrue(catalogSql.contains(
                "COALESCE(linked_asset.asset_key,legacy_asset.asset_key,matched_asset.asset_key) AS asset_key"));
        assertTrue(catalogSql.contains(
                "CASE WHEN linked_asset.catalog_key IS NOT NULL THEN linked_asset.asset_url"));
        assertTrue(catalogSql.contains("HAVING count(DISTINCT m.catalog_key)=1"));
        assertFalse(catalogSql.contains(
                "COALESCE(linked_asset.asset_key,legacy_asset.asset_key,matched_asset.asset_key,c.raw_record"));

        String customSql = jdbc.queries.stream()
                .filter(sql -> sql.contains("food_custom c"))
                .findFirst().orElseThrow();
        assertTrue(customSql.contains("COALESCE(am.asset_key,matched_asset.asset_key) AS asset_key"));
        assertTrue(customSql.contains("WHEN am.catalog_key IS NOT NULL THEN am.asset_url"));
        assertTrue(customSql.contains("WHEN ca.custom_food_id IS NOT NULL THEN '/api/core/food/custom-foods/' || c.id::text || '/asset'"));
        assertTrue(customSql.contains("LEFT JOIN cynexus_appl_life.food_custom_asset ca"));
        assertFalse(customSql.contains("COALESCE(am.asset_key,matched_asset.asset_key,c.metadata->>'asset_key')"));
    }

    @Test
    void customSaveStoresTheSelectedMappingKeyInMetadata() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FoodRepositoryImpl repository = new FoodRepositoryImpl(jdbc, new SnowflakeIdGenerator());

        repository.saveCustom("owner", "Soup", null,
                new cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients(java.math.BigDecimal.TEN, null, null, null),
                "request", "fingerprint", "food-201");

        assertTrue(jdbc.updates.get(0).sql().contains("'asset_key',?::text"));
        assertEquals("food-201", jdbc.updates.get(0).args()[10]);
    }

    @Test
    void customSavePersistsUploadedPngAndOwnerScopedReadsUseBothOwnerAndFoodId() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FoodRepositoryImpl repository = new FoodRepositoryImpl(jdbc, new SnowflakeIdGenerator());
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};

        repository.saveCustom("owner-a", "Soup", null,
                new cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients(java.math.BigDecimal.TEN, null, null, null),
                "request", "fingerprint", null, png, "a".repeat(64));
        repository.customAsset("owner-a", "42");

        assertEquals(2, jdbc.updates.size());
        assertTrue(jdbc.updates.get(1).sql().contains("food_custom_asset"));
        assertEquals(png, jdbc.updates.get(1).args()[3]);
        assertEquals("a".repeat(64), jdbc.updates.get(1).args()[4]);
        assertTrue(jdbc.queries.stream().anyMatch(sql -> sql.contains("WHERE owner_key=? AND custom_food_id=?")));
    }

    private static final class RecordingJdbc extends JdbcTemplate {
        private final List<String> queries = new ArrayList<>();
        private final List<Update> updates = new ArrayList<>();

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            queries.add(sql);
            return List.of();
        }

        @Override
        public int update(String sql, Object... args) {
            updates.add(new Update(sql, args));
            return 1;
        }

        private record Update(String sql, Object[] args) {}
    }
}
