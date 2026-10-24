package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.core.domain.workout.WorkoutModels.*;
import cn.cccs7.cynexus.core.domain.workout.WorkoutRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkoutRepositoryImpl implements WorkoutRepository {
    private static final String S = "cynexus_appl_life.";
    private final JdbcTemplate jdbc;
    private final SnowflakeIdGenerator ids;

    public WorkoutRepositoryImpl(JdbcTemplate jdbc, SnowflakeIdGenerator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public void lockOwner(String owner) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 98129))", rs -> {}, owner);
    }

    @Override
    public Plan plan(String owner) {
        return jdbc.query("SELECT cycle_start_date,timezone,name FROM " + S + "workout_plan WHERE owner_key=? AND is_active AND deleted_at IS NULL ORDER BY updated_at DESC,id DESC LIMIT 1",
                (r, n) -> new Plan(r.getObject("cycle_start_date", LocalDate.class), r.getString("timezone"), r.getString("name")), owner)
                .stream().findFirst().orElse(null);
    }

    @Override
    public Snapshot snapshot(String owner) {
        Plan plan = plan(owner);
        LocalDate cycleStart = plan == null ? LocalDate.now() : plan.cycleStart();
        List<Session> sessions = jdbc.query("SELECT id::text,workout_date,category,duration_minutes,split_code,note FROM " + S + "workout_session WHERE owner_key=? AND deleted_at IS NULL ORDER BY workout_date DESC,id DESC LIMIT 500",
                (r, n) -> new Session(r.getString("id"), r.getObject("workout_date", LocalDate.class), r.getString("category"), r.getInt("duration_minutes"), r.getString("split_code"), r.getString("note")), owner);
        List<Observation> observations = jdbc.query("SELECT id::text,metric_code,side,value,measured_at,note FROM " + S + "workout_body_measurement WHERE owner_key=? AND deleted_at IS NULL ORDER BY measured_at DESC,id DESC LIMIT 3000",
                (r, n) -> new Observation(r.getString("id"), r.getString("metric_code"), r.getString("side"), r.getBigDecimal("value"), r.getObject("measured_at", LocalDate.class), r.getString("note")), owner);
        return new Snapshot(cycleStart, false, sessions, observations);
    }

    @Override
    public String savePlan(String owner, Plan value, String key, String fingerprint) {
        Long id = jdbc.query("SELECT id FROM " + S + "workout_plan WHERE owner_key=? ORDER BY is_active DESC,deleted_at NULLS FIRST,updated_at DESC,id DESC LIMIT 1",
                (r, n) -> r.getLong("id"), owner).stream().findFirst().orElse(null);
        if (id == null) id = ids.nextId();
        int changed = jdbc.update("UPDATE " + S + "workout_plan SET name=?,cycle_start_date=?,timezone=?,is_active=true,idempotency_key=?,metadata=jsonb_build_object('requestFingerprint',?::text),deleted_at=NULL,updated_at=now() WHERE owner_key=? AND id=?",
                value.name(), value.cycleStart(), value.timezone(), key, fingerprint, owner, id);
        if (changed == 0) {
            jdbc.update("INSERT INTO " + S + "workout_plan (id,owner_key,name,cycle_start_date,timezone,is_active,idempotency_key,metadata) VALUES (?,?,?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text))",
                    id, owner, value.name(), value.cycleStart(), value.timezone(), true, key, fingerprint);
        }
        String[] splits = {"chest_triceps", "back_biceps", "shoulders", "rest"};
        for (int i = 0; i < splits.length; i++) {
            String dayType = "rest".equals(splits[i]) ? "rest" : "strength";
            int dayChanged = jdbc.update("UPDATE " + S + "workout_plan_day SET day_type=?,split_code=?,display_name=?,deleted_at=NULL,updated_at=now() WHERE owner_key=? AND plan_id=? AND day_index=?",
                    dayType, splits[i], displayName(splits[i]), owner, id, i);
            if (dayChanged == 0) jdbc.update("INSERT INTO " + S + "workout_plan_day (id,owner_key,plan_id,day_index,day_type,split_code,display_name) VALUES (?,?,?,?,?,?,?)",
                    ids.nextId(), owner, id, i, dayType, splits[i], displayName(splits[i]));
        }
        recordRequest(owner, "plan", key, Long.toString(id), fingerprint);
        return Long.toString(id);
    }

    @Override
    public Session sessionByDate(String owner, LocalDate date, boolean includeDeleted) {
        String deleted = includeDeleted ? "" : " AND deleted_at IS NULL";
        return jdbc.query("SELECT id::text,workout_date,category,duration_minutes,split_code,note FROM " + S + "workout_session WHERE owner_key=? AND workout_date=?" + deleted + " ORDER BY deleted_at NULLS FIRST,updated_at DESC,id DESC LIMIT 1",
                (r, n) -> new Session(r.getString("id"), r.getObject("workout_date", LocalDate.class), r.getString("category"), r.getInt("duration_minutes"), r.getString("split_code"), r.getString("note")), owner, date)
                .stream().findFirst().orElse(null);
    }

    @Override
    public void saveSession(String owner, Session session, String key, String fingerprint) {
        Long id = parseId(session.id());
        Session existing = sessionByDate(owner, session.date(), true);
        if (existing != null) id = parseId(existing.id());
        if (id == null) id = ids.nextId();
        Long planId = jdbc.query("SELECT id FROM " + S + "workout_plan WHERE owner_key=? AND is_active AND deleted_at IS NULL ORDER BY updated_at DESC,id DESC LIMIT 1",
                (r, n) -> r.getLong("id"), owner).stream().findFirst().orElse(null);
        int changed = jdbc.update("UPDATE " + S + "workout_session SET plan_id=?,workout_date=?,category=?,duration_minutes=?,split_code=?,note=?,idempotency_key=?,metadata=jsonb_build_object('requestFingerprint',?::text),deleted_at=NULL,updated_at=now() WHERE owner_key=? AND id=?",
                planId, session.date(), session.category(), session.minutes(), session.split(), session.note(), key, fingerprint, owner, id);
        if (changed == 0) {
            jdbc.update("INSERT INTO " + S + "workout_session (id,owner_key,plan_id,workout_date,category,duration_minutes,split_code,note,idempotency_key,metadata) VALUES (?,?,?,?,?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text))",
                    id, owner, planId, session.date(), session.category(), session.minutes(), session.split(), session.note(), key, fingerprint);
        }
        recordRequest(owner, "session", key, Long.toString(id), fingerprint);
    }

    @Override
    public void deleteSession(String owner, String id, String idempotencyKey, String fingerprint) {
        Long numeric = parseId(id);
        if (numeric != null) jdbc.update("UPDATE " + S + "workout_session SET deleted_at=now(),updated_at=now() WHERE owner_key=? AND id=? AND deleted_at IS NULL", owner, numeric);
        markRequestsDeleted(owner, "session", id);
        recordRequest(owner, "session", idempotencyKey, id, fingerprint);
    }

    @Override
    public Observation observation(String owner, String id, boolean includeDeleted) {
        Long numeric = parseId(id);
        if (numeric == null) return null;
        String deleted = includeDeleted ? "" : " AND deleted_at IS NULL";
        return jdbc.query("SELECT id::text,metric_code,side,value,measured_at,note FROM " + S + "workout_body_measurement WHERE owner_key=? AND id=?" + deleted,
                (r, n) -> new Observation(r.getString("id"), r.getString("metric_code"), r.getString("side"), r.getBigDecimal("value"), r.getObject("measured_at", LocalDate.class), r.getString("note")), owner, numeric)
                .stream().findFirst().orElse(null);
    }

    @Override
    public void saveObservation(String owner, Observation observation, String key, String fingerprint) {
        Long id = parseId(observation.id());
        String sideClause = observation.side() == null ? "side IS NULL" : "side=?";
        List<Long> matches = observation.side() == null
                ? jdbc.query("SELECT id FROM " + S + "workout_body_measurement WHERE owner_key=? AND metric_code=? AND side IS NULL AND measured_at=? ORDER BY deleted_at NULLS FIRST,updated_at DESC,id DESC LIMIT 1", (r, n) -> r.getLong("id"), owner, observation.metric(), observation.measuredAt())
                : jdbc.query("SELECT id FROM " + S + "workout_body_measurement WHERE owner_key=? AND metric_code=? AND side=? AND measured_at=? ORDER BY deleted_at NULLS FIRST,updated_at DESC,id DESC LIMIT 1", (r, n) -> r.getLong("id"), owner, observation.metric(), observation.side(), observation.measuredAt());
        if (!matches.isEmpty()) id = matches.get(0);
        if (id == null) id = ids.nextId();
        int changed = jdbc.update("UPDATE " + S + "workout_body_measurement SET metric_code=?,side=?,value=?,unit=?,measured_at=?,note=?,idempotency_key=?,metadata=jsonb_build_object('requestFingerprint',?::text),deleted_at=NULL,updated_at=now() WHERE owner_key=? AND id=?",
                observation.metric(), observation.side(), observation.value(), unit(observation.metric()), observation.measuredAt(), observation.note(), key, fingerprint, owner, id);
        if (changed == 0) {
            jdbc.update("INSERT INTO " + S + "workout_body_measurement (id,owner_key,metric_code,side,value,unit,measured_at,note,idempotency_key,metadata) VALUES (?,?,?,?,?,?,?,?,?,jsonb_build_object('requestFingerprint',?::text))",
                    id, owner, observation.metric(), observation.side(), observation.value(), unit(observation.metric()), observation.measuredAt(), observation.note(), key, fingerprint);
        }
        recordRequest(owner, "measurement", key, Long.toString(id), fingerprint);
    }

    @Override
    public void deleteObservation(String owner, String id, String idempotencyKey, String fingerprint) {
        Long numeric = parseId(id);
        if (numeric != null) jdbc.update("UPDATE " + S + "workout_body_measurement SET deleted_at=now(),updated_at=now() WHERE owner_key=? AND id=? AND deleted_at IS NULL", owner, numeric);
        markRequestsDeleted(owner, "measurement", id);
        recordRequest(owner, "measurement", idempotencyKey, id, fingerprint);
    }

    @Override
    public StoredRequest request(String owner, String resource, String key) {
        StoredRequest durable = jdbc.query("SELECT entity_id,request_fingerprint,deleted_at IS NOT NULL AS deleted FROM " + S + "workout_request WHERE owner_key=? AND resource=? AND idempotency_key=?",
                (r, n) -> new StoredRequest(r.getString("entity_id"), r.getString("request_fingerprint"), r.getBoolean("deleted")), owner, resource, key)
                .stream().findFirst().orElse(null);
        if (durable != null) return durable;
        String table = switch (resource) {
            case "plan" -> "workout_plan";
            case "session" -> "workout_session";
            case "measurement" -> "workout_body_measurement";
            default -> throw new IllegalArgumentException("Invalid workout resource");
        };
        return jdbc.query("SELECT id::text,metadata->>'requestFingerprint' AS fingerprint,deleted_at IS NOT NULL AS deleted FROM " + S + table + " WHERE owner_key=? AND idempotency_key=? ORDER BY updated_at DESC LIMIT 1",
                (r, n) -> new StoredRequest(r.getString("id"), r.getString("fingerprint"), r.getBoolean("deleted")), owner, key)
                .stream().findFirst().orElse(null);
    }

    @Override
    public void recordRequest(String owner, String resource, String key, String id, String fingerprint) {
        jdbc.update("INSERT INTO " + S + "workout_request (owner_key,resource,idempotency_key,entity_id,request_fingerprint) VALUES (?,?,?,?,?)",
                owner, resource, key, id, fingerprint);
    }

    @Override
    public void markRequestsDeleted(String owner, String resource, String id) {
        jdbc.update("UPDATE " + S + "workout_request SET deleted_at=now() WHERE owner_key=? AND resource=? AND entity_id=? AND deleted_at IS NULL",
                owner, resource, id);
    }

    private static Long parseId(String id) {
        if (id == null) return null;
        try { long value = Long.parseLong(id); return value > 0 ? value : null; } catch (RuntimeException ignored) { return null; }
    }
    private static String unit(String metric) { return "weight".equals(metric) ? "kg" : "cm"; }
    private static String displayName(String split) {
        return switch (split) { case "chest_triceps" -> "Chest + Triceps"; case "back_biceps" -> "Back + Biceps"; case "shoulders" -> "Shoulders"; default -> "Rest"; };
    }
}
