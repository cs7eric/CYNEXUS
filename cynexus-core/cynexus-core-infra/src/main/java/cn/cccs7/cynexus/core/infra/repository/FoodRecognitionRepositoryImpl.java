package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.*;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FoodRecognitionRepositoryImpl implements FoodRecognitionRepository {
    private static final String TABLE = "cynexus_appl_life.food_recognition_task";
    private static final String SUMMARY_COLUMNS = "id,owner_key,tenant_key,principal_key,request_fingerprint,task_status,phase,ai_task_id,"
            + "failure_count,retry_count,claimed_by,food_id,food_name,error_message,created_at,cycle_started_at,updated_at,completed_at,"
            + "null::jsonb as command_json,null::jsonb as prepared_json";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public FoodRecognitionRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Task create(Identity identity, Command command, String fingerprint) {
        Long id = jdbc.queryForObject("""
                insert into cynexus_appl_life.food_recognition_task
                    (owner_key,tenant_key,principal_key,idempotency_key,request_fingerprint,command_json)
                values (?,?,?,?,?,cast(? as jsonb))
                on conflict (owner_key,idempotency_key) do update
                    set updated_at=cynexus_appl_life.food_recognition_task.updated_at
                returning id
                """, Long.class, identity.owner(), identity.tenant(), identity.principal(), command.idempotencyKey(),
                fingerprint, json(command));
        if (id == null) throw new IllegalStateException("Food recognition task was not persisted");
        return find(id, identity.owner());
    }

    @Override
    public Task find(long id, String owner) {
        return first(jdbc.query("select " + SUMMARY_COLUMNS + " from " + TABLE + " where id=? and owner_key=?",
                (rs, row) -> map(rs), id, owner));
    }

    @Override
    public List<Task> list(String owner, int limit) {
        return jdbc.query("select " + SUMMARY_COLUMNS + " from " + TABLE
                + " where owner_key=? order by created_at desc,id desc limit ?", (rs, row) -> map(rs), owner, limit);
    }

    @Override
    public Task retry(long id, String owner) {
        List<Long> ids = jdbc.query("""
                update cynexus_appl_life.food_recognition_task
                   set task_status='queued',phase='submit',ai_task_id=null,prepared_json=null,
                       failure_count=0,retry_count=retry_count+1,error_message=null,claimed_by=null,claimed_until=null,
                       available_at=now(),cycle_started_at=now(),completed_at=null,updated_at=now()
                 where id=? and owner_key=? and task_status='failed' and command_json is not null and retry_count<20
                   and completed_at>=now()-interval '7 days'
                returning id
                """, (rs, row) -> rs.getLong(1), id, owner);
        return ids.isEmpty() ? null : find(ids.get(0), owner);
    }

    @Override
    public int purgeExpiredInputs() {
        return jdbc.update("""
                update cynexus_appl_life.food_recognition_task
                   set command_json=null,prepared_json=null,updated_at=now()
                 where task_status='failed' and completed_at<now()-interval '7 days'
                   and (command_json is not null or prepared_json is not null)
                """);
    }

    @Override
    public Task claimNext() {
        String token = UUID.randomUUID().toString();
        return first(jdbc.query("""
                with candidate as (
                    select id from cynexus_appl_life.food_recognition_task
                     where task_status in ('queued','running') and available_at<=now()
                       and (claimed_until is null or claimed_until<now())
                     order by available_at,id for update skip locked limit 1
                )
                update cynexus_appl_life.food_recognition_task task
                   set task_status='running',claimed_by=?,claimed_until=now()+interval '5 minutes',updated_at=now()
                  from candidate where task.id=candidate.id returning task.*
                """, (rs, row) -> map(rs), token));
    }

    @Override
    public void awaitAi(Task task, String aiTaskId) {
        jdbc.update("""
                update cynexus_appl_life.food_recognition_task
                   set phase='poll',ai_task_id=?,failure_count=0,available_at=now()+interval '3 seconds',
                       claimed_by=null,claimed_until=null,updated_at=now()
                 where id=? and task_status='running' and claimed_by=? and claimed_until>now()
                """, aiTaskId, task.id(), task.claimToken());
    }

    @Override
    public boolean prepare(Task task, Prepared prepared) {
        return jdbc.update("""
                update cynexus_appl_life.food_recognition_task
                   set phase='save',prepared_json=cast(? as jsonb),failure_count=0,updated_at=now()
                 where id=? and task_status='running' and claimed_by=? and claimed_until>now()
                """, json(prepared), task.id(), task.claimToken()) == 1;
    }

    @Override
    public Task lockForCompletion(Task task) {
        return first(jdbc.query("select * from " + TABLE
                + " where id=? and task_status='running' and phase='save' and claimed_by=? and claimed_until>now() for update",
                (rs, row) -> map(rs), task.id(), task.claimToken()));
    }

    @Override
    public void succeed(Task task, String foodId, String foodName) {
        int updated = jdbc.update("""
                update cynexus_appl_life.food_recognition_task
                   set task_status='succeeded',food_id=?,food_name=?,command_json=null,prepared_json=null,
                       error_message=null,claimed_by=null,claimed_until=null,completed_at=now(),updated_at=now()
                 where id=? and task_status='running' and claimed_by=?
                """, foodId, foodName, task.id(), task.claimToken());
        if (updated != 1) throw new IllegalStateException("Food recognition task claim expired");
    }

    @Override
    public void fail(Task task, String error, boolean terminal, int retryDelaySeconds) {
        String message = error == null || error.isBlank() ? "Food recognition failed" : error;
        jdbc.update("""
                update cynexus_appl_life.food_recognition_task
                   set task_status=?,failure_count=failure_count+1,error_message=?,claimed_by=null,claimed_until=null,
                       available_at=now()+(?*interval '1 second'),completed_at=case when ? then now() else null end,updated_at=now()
                 where id=? and task_status='running' and claimed_by=?
                """, terminal ? "failed" : "running", message.substring(0, Math.min(message.length(), 1000)),
                retryDelaySeconds, terminal, task.id(), task.claimToken());
    }

    private Task map(ResultSet rs) throws SQLException {
        try {
            String command = rs.getString("command_json"), prepared = rs.getString("prepared_json");
            return new Task(rs.getLong("id"), new Identity(rs.getString("owner_key"), rs.getString("tenant_key"), rs.getString("principal_key")),
                    rs.getString("request_fingerprint"), command == null ? null : mapper.readValue(command, Command.class),
                    rs.getString("task_status"), rs.getString("phase"), rs.getString("ai_task_id"),
                    prepared == null ? null : mapper.readValue(prepared, Prepared.class), rs.getInt("failure_count"),
                    rs.getInt("retry_count"), rs.getString("claimed_by"), rs.getString("food_id"), rs.getString("food_name"),
                    rs.getString("error_message"), rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("cycle_started_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class),
                    rs.getObject("completed_at", OffsetDateTime.class));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Stored food recognition payload is invalid", exception);
        }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new IllegalStateException("Food recognition payload could not be stored", exception); }
    }

    private static Task first(List<Task> tasks) { return tasks.isEmpty() ? null : tasks.get(0); }
}
