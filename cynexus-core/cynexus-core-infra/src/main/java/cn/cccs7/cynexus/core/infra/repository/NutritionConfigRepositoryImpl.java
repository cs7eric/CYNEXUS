package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.core.domain.food.NutritionConfigModels.Config;
import cn.cccs7.cynexus.core.domain.food.NutritionConfigRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC adapter for the owner-scoped nutrition configuration table. */
@Repository
public class NutritionConfigRepositoryImpl implements NutritionConfigRepository {
    private static final String TABLE = "cynexus_appl_life.nutrition_configuration";
    private final JdbcTemplate jdbc;
    private final SnowflakeIdGenerator ids;

    public NutritionConfigRepositoryImpl(JdbcTemplate jdbc, SnowflakeIdGenerator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public void lockOwner(String owner) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 98131))", rs -> {}, owner);
    }

    @Override
    public Config find(String owner) {
        return jdbc.query("SELECT sex,body_fat_pct,age_years,activity_factor,calorie_deficit_kcal,"
                        + "carbs_g_per_kg,protein_g_per_kg,fat_g_per_kg FROM " + TABLE
                        + " WHERE owner_key=? AND deleted_at IS NULL ORDER BY updated_at DESC,id DESC LIMIT 1",
                this::map, owner).stream().findFirst().orElse(null);
    }

    @Override
    public Config upsert(String owner, Config config) {
        int changed = jdbc.update("UPDATE " + TABLE + " SET sex=?,body_fat_pct=?,age_years=?,activity_factor=?,"
                        + "calorie_deficit_kcal=?,carbs_g_per_kg=?,protein_g_per_kg=?,fat_g_per_kg=?,"
                        + "metadata=jsonb_build_object('source','configuration-center'),deleted_at=NULL,updated_at=now()"
                        + " WHERE owner_key=? AND deleted_at IS NULL",
                config.sex(), config.bodyFatPct(), config.ageYears(), config.activityFactor(), config.calorieDeficit(),
                config.carbsMultiplier(), config.proteinMultiplier(), config.fatMultiplier(), owner);
        if (changed == 0) {
            jdbc.update("INSERT INTO " + TABLE + " (id,owner_key,sex,body_fat_pct,age_years,activity_factor,"
                            + "calorie_deficit_kcal,carbs_g_per_kg,protein_g_per_kg,fat_g_per_kg,metadata)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,jsonb_build_object('source','configuration-center'))",
                    ids.nextId(), owner, config.sex(), config.bodyFatPct(), config.ageYears(), config.activityFactor(),
                    config.calorieDeficit(), config.carbsMultiplier(), config.proteinMultiplier(), config.fatMultiplier());
        }
        return find(owner);
    }

    @Override
    public void delete(String owner) {
        jdbc.update("UPDATE " + TABLE + " SET deleted_at=now(),updated_at=now() WHERE owner_key=? AND deleted_at IS NULL", owner);
    }

    private Config map(ResultSet row, int ignored) throws SQLException {
        int ageValue = row.getInt("age_years");
        Integer age = row.wasNull() ? null : ageValue;
        return new Config(row.getString("sex"), row.getBigDecimal("body_fat_pct"),
                age, row.getBigDecimal("activity_factor"),
                row.getBigDecimal("calorie_deficit_kcal"), row.getBigDecimal("carbs_g_per_kg"),
                row.getBigDecimal("protein_g_per_kg"), row.getBigDecimal("fat_g_per_kg"));
    }
}
