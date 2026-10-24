package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.food.FridgeModels.AdjustCommand;
import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FridgeRepositoryImplTest {
    @Test
    void adjustmentRetryLookupMatchesOnlyTheRequestOrItsDerivedEventKeys() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FridgeRepositoryImpl repository = new FridgeRepositoryImpl(jdbc, (SnowflakeIdGenerator) null);

        assertThrows(IllegalArgumentException.class, () -> repository.adjust("owner", "42",
                new AdjustCommand(new BigDecimal("1"), "g", "abc%", "user", null)));

        assertEquals("SELECT count(*) FROM cynexus_appl_life.food_fridge_stock_event "
                        + "WHERE owner_key=? AND (idempotency_key=? OR left(idempotency_key,length(?))=?)",
                jdbc.sql);
        assertEquals(List.of("owner", "abc%", "abc%:", "abc%:"), jdbc.arguments);
    }

    @Test
    void nutritionSearchTreatsExactCatalogAliasesAsExactAndCombinesCustomMatchesBeforeLimiting() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FridgeRepositoryImpl repository = new FridgeRepositoryImpl(jdbc, (SnowflakeIdGenerator) null);

        repository.searchMapping("owner", "Oats", 20);

        assertEquals(2, jdbc.queries.size());
        assertTrue(jdbc.queries.get(0).contains("unnest(c.aliases) a WHERE lower(btrim(a))=lower(btrim(?))"));
        assertTrue(jdbc.queries.get(1).contains("ORDER BY confidence DESC,c.name,c.id LIMIT ?"));
        assertEquals(List.of("Oats", "Oats", "%oats%", "%oats%", 20), jdbc.argumentsByQuery.get(0));
        assertEquals(List.of("Oats", "owner", "%oats%", 20), jdbc.argumentsByQuery.get(1));
    }

    @Test
    void rememberedMappingLookupUsesTheOwnerNameAndBrandIdentity() {
        RecordingJdbc jdbc = new RecordingJdbc();
        FridgeRepositoryImpl repository = new FridgeRepositoryImpl(jdbc, (SnowflakeIdGenerator) null);

        assertEquals(null, repository.findConfirmedMapping("owner", "  My   oats ", "Brand"));

        assertTrue(jdbc.queries.get(0).contains("FROM cynexus_appl_life.food_fridge_mapping m"));
        assertTrue(jdbc.queries.get(0).contains("lower(btrim(m.brand)) IS NOT DISTINCT FROM lower(btrim(?))"));
        assertEquals(List.of("owner", "my oats", "Brand"), jdbc.argumentsByQuery.get(0));
    }

    @Test
    void confirmingNutritionMappingAlsoResolvesAndPersistsTheFoodArtwork() {
        ConfirmMappingJdbc jdbc = new ConfirmMappingJdbc();
        FridgeRepositoryImpl repository = new FridgeRepositoryImpl(jdbc, (SnowflakeIdGenerator) null);

        repository.confirmMapping("owner", "42", "1234", "catalog");

        UpdateCall artworkUpdate = jdbc.updates.stream()
                .filter(call -> call.sql().contains("SET asset_catalog_key=COALESCE"))
                .findFirst().orElseThrow();
        assertTrue(artworkUpdate.sql().contains("relation.deleted_at IS NULL"));
        assertTrue(artworkUpdate.sql().contains("food_nutrition_catalog c"));
        assertTrue(artworkUpdate.sql().contains("HAVING count(DISTINCT a.catalog_key)=1"));
        assertTrue(artworkUpdate.sql().contains("ORDER BY candidate.priority,candidate.catalog_key"));
        assertEquals(Arrays.asList("owner", "oats", null, 1234L, 1234L, 1234L), artworkUpdate.arguments());
        UpdateCall associationUpdate = jdbc.updates.stream()
                .filter(call -> call.sql().contains("INSERT INTO cynexus_appl_life.food_asset_catalog_mapping"))
                .findFirst().orElseThrow();
        assertTrue(associationUpdate.sql().contains("deleted_at=NULL"));
    }

    private static final class RecordingJdbc extends JdbcTemplate {
        private String sql;
        private List<Object> arguments;
        private final List<String> queries = new ArrayList<>();
        private final List<List<Object>> argumentsByQuery = new ArrayList<>();

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            this.sql = sql;
            this.arguments = List.of(args);
            return requiredType.cast(0L);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.queries.add(sql);
            this.argumentsByQuery.add(List.of(args));
            return new ArrayList<>();
        }
    }

    private record UpdateCall(String sql, List<Object> arguments) {}

    private static final class ConfirmMappingJdbc extends JdbcTemplate {
        private final List<UpdateCall> updates = new ArrayList<>();

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            return requiredType.cast(Boolean.TRUE);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            if (sql.contains("SELECT id,name,brand,normalized_name")) {
                return List.of(map(rowMapper, row(Map.of(
                        "id", 42L, "name", "Oats", "normalized_name", "oats"))));
            }
            if (sql.contains("SELECT id FROM cynexus_appl_life.food_fridge_mapping")) {
                return List.of(map(rowMapper, row(Map.of("id", 77L))));
            }
            return List.of();
        }

        @Override
        public int update(String sql, Object... args) {
            updates.add(new UpdateCall(sql, Arrays.asList(args)));
            return 1;
        }

        private static <T> T map(RowMapper<T> mapper, ResultSet resultSet) {
            try {
                return mapper.mapRow(resultSet, 0);
            } catch (SQLException exception) {
                throw new AssertionError(exception);
            }
        }

        private static ResultSet row(Map<String, Object> values) {
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        if ("getLong".equals(method.getName())) {
                            String column = args[0] instanceof Integer ? "id" : String.valueOf(args[0]);
                            return ((Number) values.get(column)).longValue();
                        }
                        if ("getString".equals(method.getName())) {
                            Object value = values.get(String.valueOf(args[0]));
                            return value == null ? null : value.toString();
                        }
                        if ("wasNull".equals(method.getName())) return false;
                        return null;
                    });
        }
    }
}
