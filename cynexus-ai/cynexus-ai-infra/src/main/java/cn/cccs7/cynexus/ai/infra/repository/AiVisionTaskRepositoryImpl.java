package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AiVisionTaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiVisionTaskRepositoryImpl implements AiVisionTaskRepository {
    private static final TypeReference<List<AiVisionTaskBO.Image>> IMAGES_TYPE = new TypeReference<>() { };
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiVisionTaskRepositoryImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiVisionTaskBO create(AiVisionTaskBO task) {
        String imagesJson;
        try {
            imagesJson = objectMapper.writeValueAsString(task.getImages());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist vision images", exception);
        }
        Long id = jdbcTemplate.queryForObject("""
                insert into cynexus_appl_ai.ai_vision_task
                    (scenario_code, user_input, preferred_model_code, images_json, tenant_key, principal_key, idempotency_key,
                     task_status, progress, attempt_count, max_attempts)
                values (?, ?, ?, cast(? as jsonb), ?, ?, ?, 'queued', 0, 0, ?)
                on conflict (tenant_key, principal_key, idempotency_key) where idempotency_key is not null
                do update set updated_at = cynexus_appl_ai.ai_vision_task.updated_at
                returning id
                """, Long.class, task.getScenarioCode(), task.getUserInput(), task.getPreferredModelCode(),
                imagesJson, task.getTenantKey(), task.getPrincipalKey(), task.getIdempotencyKey(),
                task.getMaxAttempts() == null ? 2 : task.getMaxAttempts());
        return find(id, task.getTenantKey(), task.getPrincipalKey());
    }

    @Override
    public AiVisionTaskBO find(Long id, String tenantKey, String principalKey) {
        List<AiVisionTaskBO> rows = jdbcTemplate.query("""
                select id, scenario_code, user_input, preferred_model_code, '[]'::jsonb as images_json, tenant_key, principal_key, idempotency_key,
                       task_status, progress, attempt_count, max_attempts, claimed_by, execution_no, provider_code, model_code,
                       result_content, error_message, started_at, completed_at, created_at, updated_at,
                       jsonb_array_length(task.images_json) > 0 as inputs_available,
                       exists (select 1 from cynexus_appl_ai.ai_vision_task child where child.retry_of_id = task.id) as has_retry
                  from cynexus_appl_ai.ai_vision_task task
                 where task.id = ? and task.tenant_key is not distinct from ? and task.principal_key is not distinct from ?
                """, (rs, rowNum) -> map(rs), id, tenantKey, principalKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<AiVisionTaskBO> listByOwner(int limit, String tenantKey, String principalKey) {
        return jdbcTemplate.query("""
                select id, scenario_code, user_input, preferred_model_code, '[]'::jsonb as images_json,
                       tenant_key, principal_key, idempotency_key, task_status, progress, attempt_count, max_attempts,
                       claimed_by, execution_no, provider_code, model_code, null::text as result_content, error_message,
                       started_at, completed_at, created_at, updated_at,
                       jsonb_array_length(task.images_json) > 0 as inputs_available,
                       exists (select 1 from cynexus_appl_ai.ai_vision_task child where child.retry_of_id = task.id) as has_retry
                  from cynexus_appl_ai.ai_vision_task task
                 where task.tenant_key is not distinct from ? and task.principal_key is not distinct from ?
                 order by created_at desc, id desc
                 limit ?
                """, (rs, rowNum) -> map(rs), tenantKey, principalKey, limit);
    }

    @Override
    public AiVisionTaskBO retryFailed(Long id, String tenantKey, String principalKey, OffsetDateTime failedAfter) {
        List<Long> ids = jdbcTemplate.query("""
                with source as (
                    select * from cynexus_appl_ai.ai_vision_task
                     where id = ? and tenant_key is not distinct from ?
                       and principal_key is not distinct from ? and task_status = 'failed'
                       and completed_at > ? and jsonb_array_length(images_json) > 0
                     for update
                ), retry as (
                    insert into cynexus_appl_ai.ai_vision_task
                        (scenario_code, user_input, preferred_model_code, images_json, tenant_key, principal_key,
                         idempotency_key, task_status, progress, attempt_count, max_attempts, retry_of_id)
                    select source.scenario_code, source.user_input, source.preferred_model_code, source.images_json,
                           source.tenant_key, source.principal_key, null, 'queued', 0, 0, source.max_attempts, source.id
                      from source
                    on conflict (retry_of_id) where retry_of_id is not null do nothing
                    returning id, retry_of_id
                ), cleared_source as (
                    update cynexus_appl_ai.ai_vision_task
                       set images_json = '[]'::jsonb
                     where id in (select retry_of_id from retry)
                    returning id
                )
                select id from retry
                """, (rs, rowNum) -> rs.getLong("id"), id, tenantKey, principalKey, failedAfter);
        return ids.isEmpty() ? null : find(ids.get(0), tenantKey, principalKey);
    }

    @Override
    public int purgeTerminalInputs(OffsetDateTime failedBefore) {
        return jdbcTemplate.update("""
                update cynexus_appl_ai.ai_vision_task
                   set images_json = '[]'::jsonb
                 where jsonb_array_length(images_json) > 0
                   and (task_status = 'succeeded'
                        or (task_status = 'failed' and coalesce(completed_at, updated_at, created_at) <= ?))
                """, failedBefore);
    }

    @Override
    public AiVisionTaskBO claimNext(String workerId) {
        String claimToken = (workerId == null || workerId.isBlank() ? "ai-vision-worker" : workerId)
                + ":" + UUID.randomUUID();
        List<AiVisionTaskBO> rows = jdbcTemplate.query("""
                with candidate as (
                    select id
                      from cynexus_appl_ai.ai_vision_task
                     where task_status = 'queued'
                     order by created_at, id
                     for update skip locked limit 1
                )
                    update cynexus_appl_ai.ai_vision_task task
                       set task_status = 'running', progress = 5, attempt_count = task.attempt_count + 1,
                           claimed_by = ?, started_at = now(), updated_at = now()
                      from candidate
                     where task.id = candidate.id
                    returning task.*, jsonb_array_length(task.images_json) > 0 as inputs_available,
                              false as has_retry
                """, (rs, rowNum) -> map(rs), claimToken);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void releaseClaim(AiVisionTaskBO task) {
        jdbcTemplate.update("""
                update cynexus_appl_ai.ai_vision_task
                   set task_status = 'queued', progress = 0,
                       attempt_count = greatest(attempt_count - 1, 0),
                       claimed_by = null, started_at = null, error_message = null, updated_at = now()
                 where id = ? and task_status = 'running' and claimed_by = ?
                """, task.getId(), task.getClaimToken());
    }

    @Override
    public int recoverStaleTasks(int timeoutMinutes) {
        return jdbcTemplate.update("""
                update cynexus_appl_ai.ai_vision_task
                   set task_status = case when attempt_count >= max_attempts then 'failed' else 'queued' end,
                       progress = case when attempt_count >= max_attempts then 100 else 0 end,
                       error_message = case when attempt_count >= max_attempts then coalesce(error_message, 'Vision worker timed out') else error_message end,
                       completed_at = case when attempt_count >= max_attempts then now() else null end,
                       claimed_by = null, updated_at = now()
                 where task_status = 'running' and started_at < now() - (? * interval '1 minute')
                """, Math.max(1, timeoutMinutes));
    }

    @Override
    public void markSucceeded(AiVisionTaskBO task) {
        jdbcTemplate.update("""
                update cynexus_appl_ai.ai_vision_task
                   set task_status = 'succeeded', progress = 100, execution_no = ?, provider_code = ?, model_code = ?,
                       result_content = ?, images_json = '[]'::jsonb, error_message = null,
                       claimed_by = null, completed_at = now(), updated_at = now()
                 where id = ? and task_status = 'running' and claimed_by = ?
                """, task.getExecutionNo(), task.getProviderCode(), task.getModelCode(), task.getContent(), task.getId(),
                task.getClaimToken());
    }

    @Override
    public void markFailed(AiVisionTaskBO task, String errorMessage) {
        // The domain retention cutoff bounds how long failed inputs remain available to explicit retries.
        jdbcTemplate.update("""
                update cynexus_appl_ai.ai_vision_task
                   set task_status = 'failed', progress = 100, error_message = ?, claimed_by = null,
                       completed_at = now(), updated_at = now()
                 where id = ? and task_status = 'running' and claimed_by = ?
                """, truncate(errorMessage, 4000), task.getId(), task.getClaimToken());
    }

    private AiVisionTaskBO map(ResultSet rs) {
        try {
            return AiVisionTaskBO.builder()
                    .id(rs.getLong("id"))
                    .scenarioCode(rs.getString("scenario_code"))
                    .userInput(rs.getString("user_input"))
                    .preferredModelCode(rs.getString("preferred_model_code"))
                    .images(objectMapper.readValue(rs.getString("images_json"), IMAGES_TYPE))
                    .tenantKey(rs.getString("tenant_key"))
                    .principalKey(rs.getString("principal_key"))
                    .idempotencyKey(rs.getString("idempotency_key"))
                    .status(rs.getString("task_status"))
                    .progress(integerValue(rs, "progress"))
                    .attemptCount(integerValue(rs, "attempt_count"))
                    .maxAttempts(integerValue(rs, "max_attempts"))
                    .claimToken(rs.getString("claimed_by"))
                    .hasRetry(rs.getBoolean("has_retry"))
                    .inputsAvailable(rs.getBoolean("inputs_available"))
                    .executionNo(rs.getString("execution_no"))
                    .providerCode(rs.getString("provider_code"))
                    .modelCode(rs.getString("model_code"))
                    .content(rs.getString("result_content"))
                    .errorMessage(rs.getString("error_message"))
                    .startedAt(rs.getObject("started_at", OffsetDateTime.class))
                    .completedAt(rs.getObject("completed_at", OffsetDateTime.class))
                    .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                    .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read vision task", exception);
        }
    }

    private Integer integerValue(ResultSet rs, String column) throws java.sql.SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.intValue();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "Vision task failed";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
