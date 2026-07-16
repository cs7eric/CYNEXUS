package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.MarketInformationAiTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.MarketInformationAiTaskRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketInformationAiTaskRepositoryImpl implements MarketInformationAiTaskRepository {
    private final JdbcTemplate jdbcTemplate;

    public MarketInformationAiTaskRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int recoverStaleTasks(int timeoutMinutes) {
        return jdbcTemplate.update("""
                update market_information_ai_task
                   set task_status = case when attempt_count >= max_attempts then 'FAILED' else 'PENDING' end,
                       next_attempt_at = now(), claimed_at = null, claimed_by = null,
                       last_error = coalesce(last_error, 'Recovered after worker timeout'), updated_at = now()
                 where task_status = 'RUNNING'
                   and claimed_at < now() - (? * interval '1 minute')
                """, Math.max(timeoutMinutes, 1));
    }

    @Override
    public boolean tryReserveProviderRequest(String providerCode, String usageScope, int limit) {
        if (limit <= 0) return false;
        List<Integer> rows = jdbcTemplate.query("""
                insert into market_information_provider_usage (provider_code, usage_scope, request_count)
                values (?, ?, 1)
                on conflict (provider_code, usage_scope) do update
                   set request_count = market_information_provider_usage.request_count + 1,
                       updated_at = now()
                 where market_information_provider_usage.request_count < ?
                returning request_count
                """, (rs, rowNum) -> rs.getInt(1), providerCode, usageScope, limit);
        return !rows.isEmpty();
    }

    @Override
    public MarketInformationAiTaskBO claimNext(String workerId) {
        List<MarketInformationAiTaskBO> rows = jdbcTemplate.query("""
                with candidate as (
                    select id from market_information_ai_task
                     where task_status = 'PENDING' and next_attempt_at <= now()
                     order by priority desc, created_at
                     for update skip locked limit 1
                )
                update market_information_ai_task task
                   set task_status = 'RUNNING', claimed_at = now(), claimed_by = ?,
                       attempt_count = task.attempt_count + 1, updated_at = now()
                  from candidate
                 where task.id = candidate.id
                returning task.id, task.task_type, task.cluster_id, task.cluster_content_hash,
                          task.attempt_count, task.max_attempts
                """, (rs, n) -> MarketInformationAiTaskBO.builder().id(rs.getLong("id"))
                .taskType(rs.getString("task_type")).clusterId(rs.getObject("cluster_id", Long.class))
                .clusterContentHash(rs.getString("cluster_content_hash")).attemptCount(rs.getInt("attempt_count"))
                .maxAttempts(rs.getInt("max_attempts")).build(), workerId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public MarketInformationAiTaskBO loadInput(MarketInformationAiTaskBO task) {
        if (task == null || task.getClusterId() == null) return task;
        jdbcTemplate.query("""
                select display_title, category, content_hash
                  from cynexus_appl_market.market_information_event_cluster where id = ?
                """, rs -> {
            if (rs.next()) {
                task.setTitle(rs.getString("display_title"));
                task.setCategory(rs.getString("category"));
                task.setClusterContentHash(rs.getString("content_hash"));
            }
            return task;
        }, task.getClusterId());
        List<MarketInformationAiTaskBO.Source> sources = jdbcTemplate.query("""
                select member.source, member.source_record_id, cache.title, cache.content_text, cache.content_quality
                  from cynexus_appl_market.market_information_event_member member
                  left join lateral (
                      select title, content_text, content_quality
                        from cynexus_appl_market.market_information_content_cache value
                       where value.source = member.source and value.source_record_id = member.source_record_id
                       order by case value.content_quality when 'full_text' then 0 when 'ocr' then 1 else 2 end,
                                value.updated_at desc limit 1
                  ) cache on true
                 where member.cluster_id = ? order by member.published_at desc
                """, (rs, n) -> MarketInformationAiTaskBO.Source.builder()
                .sourceId(rs.getString("source") + ":" + rs.getString("source_record_id"))
                .source(rs.getString("source")).title(rs.getString("title"))
                .content(rs.getString("content_text")).contentQuality(rs.getString("content_quality")).build(),
                task.getClusterId());
        for (MarketInformationAiTaskBO.Source source : sources) {
            source.setDeterministicImpact(jdbcTemplate.query("""
                    with raw as (
                        select nullif(trim(code), '') as code, title, summary from cynexus_appl_market.a_stock_news where source = ? and news_id = ?
                        union all
                        select nullif(trim(code), ''), title, null from cynexus_appl_market.a_stock_announcement where source = ? and announcement_id = ?
                    ), latest as (select max(snapshot_date) snapshot_date from cynexus_appl_market.ths_industry_constituent_snapshot where deleted_at is null)
                    select distinct map.tdx_name || ' [' || case when exists (
                        select 1 from cynexus_appl_market.ths_industry_constituent_snapshot c cross join latest
                         where c.deleted_at is null and c.snapshot_date = latest.snapshot_date and c.stock_code = raw.code and c.industry_code = map.ths_code
                    ) then 'Direct' else 'Inferred' end || ']'
                      from raw join cynexus_appl_market.tdx_ths_industry_mapping map on map.deleted_at is null
                     where exists (
                        select 1 from cynexus_appl_market.ths_industry_constituent_snapshot c cross join latest
                         where c.deleted_at is null and c.snapshot_date = latest.snapshot_date and c.stock_code = raw.code and c.industry_code = map.ths_code
                     ) or ((raw.title || ' ' || coalesce(raw.summary, '')) like '%' || map.tdx_name || '%')
                     order by 1
                    """, (rs, n) -> rs.getString(1), source.getSource(), source.getSourceId().substring(source.getSource().length() + 1), source.getSource(), source.getSourceId().substring(source.getSource().length() + 1)));
        }
        task.setSources(sources);
        return task;
    }

    @Override
    public String loadBriefInput(String eventSchemaVersion, String eventPromptVersion) {
        List<String> rows = jdbcTemplate.query("""
                with ranked as (
                    select analysis.cluster_id, analysis.analysis_json, analysis.updated_at as analysis_updated_at,
                           cluster.display_title, cluster.category, cluster.priority_score,
                           cluster.first_published_at, cluster.last_published_at,
                           row_number() over (
                               partition by analysis.cluster_id
                               order by analysis.updated_at desc, analysis.id desc
                           ) as version_rank
                      from market_information_event_analysis analysis
                      join cynexus_appl_market.market_information_event_cluster cluster
                        on cluster.id = analysis.cluster_id
                       and cluster.content_hash = analysis.cluster_content_hash
                     where analysis.analysis_status = 'SUCCEEDED'
                       and analysis.schema_version = ?
                       and analysis.prompt_version = ?
                       and analysis.updated_at >= now() - interval '36 hours'
                ), selected as (
                    select * from ranked
                     where version_rank = 1
                     order by priority_score desc, last_published_at desc, analysis_updated_at desc
                     limit 30
                ), coverage as (
                    select member.source, count(distinct member.source_record_id)::integer as source_count
                      from selected
                      join cynexus_appl_market.market_information_event_member member
                        on member.cluster_id = selected.cluster_id
                     group by member.source
                ), payload as (
                    select coalesce(jsonb_agg(jsonb_build_object(
                               'clusterId', cluster_id,
                               'title', display_title,
                               'category', category,
                               'priorityScore', priority_score,
                               'firstPublishedAt', first_published_at,
                               'lastPublishedAt', last_published_at,
                               'analysis', analysis_json
                           ) order by priority_score desc, last_published_at desc), '[]'::jsonb) as events,
                           jsonb_build_object(
                               'from', min(first_published_at),
                               'to', max(last_published_at),
                               'eventCount', count(*)
                           ) as analysis_window
                      from selected
                )
                select case when jsonb_array_length(payload.events) = 0 then null else jsonb_build_object(
                    'analysisWindow', payload.analysis_window,
                    'sourceCoverage', coalesce(
                        (select jsonb_object_agg(source, source_count order by source) from coverage),
                        '{}'::jsonb
                    ),
                    'events', payload.events
                )::text end
                  from payload
                """, (rs, n) -> rs.getString(1), eventSchemaVersion, eventPromptVersion);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void saveEventAnalysis(MarketInformationAiTaskBO task, String schemaVersion, String promptVersion,
                                  String providerCode, String modelCode, String analysisJson,
                                  String responsePreview, boolean fallbackUsed) {
        jdbcTemplate.update("""
                insert into market_information_event_analysis
                    (cluster_id, cluster_content_hash, schema_version, prompt_version, provider_code, model_code,
                     analysis_status, analysis_json, response_preview, fallback_used)
                values (?, ?, ?, ?, ?, ?, 'SUCCEEDED', cast(? as jsonb), ?, ?)
                on conflict (cluster_id, cluster_content_hash, schema_version, prompt_version, provider_code, model_code)
                do update set analysis_status = 'SUCCEEDED', analysis_json = excluded.analysis_json,
                              response_preview = excluded.response_preview, fallback_used = excluded.fallback_used,
                              error_message = null, updated_at = now()
                """, task.getClusterId(), task.getClusterContentHash(), schemaVersion, promptVersion,
                providerCode, modelCode, analysisJson, responsePreview, fallbackUsed);
    }

    @Override
    public void saveDailyBrief(MarketInformationAiTaskBO task, String schemaVersion, String promptVersion,
                               String providerCode, String modelCode, String briefJson, boolean fallbackUsed) {
        String contentHash = task.getClusterContentHash() == null ? sha256(briefJson) : task.getClusterContentHash();
        jdbcTemplate.update("""
                insert into market_information_daily_brief
                    (brief_date, content_hash, schema_version, prompt_version, provider_code, model_code,
                     brief_status, brief_json, fallback_used)
                values (current_date, ?, ?, ?, ?, ?, 'SUCCEEDED', cast(? as jsonb), ?)
                on conflict (brief_date, content_hash, schema_version, prompt_version, provider_code, model_code)
                do update set brief_status = 'SUCCEEDED', brief_json = excluded.brief_json,
                              fallback_used = excluded.fallback_used, error_message = null, updated_at = now()
                """, contentHash, schemaVersion, promptVersion, providerCode, modelCode, briefJson, fallbackUsed);
    }

    @Override
    public void markSucceeded(long taskId) {
        jdbcTemplate.update("update market_information_ai_task set task_status = 'SUCCEEDED', updated_at = now() where id = ?", taskId);
    }

    @Override
    public void markFailed(MarketInformationAiTaskBO task, String errorMessage) {
        boolean exhausted = task.getAttemptCount() >= task.getMaxAttempts();
        jdbcTemplate.update("""
                update market_information_ai_task
                   set task_status = ?, next_attempt_at = now() + (? * interval '1 minute'),
                       last_error = ?, claimed_at = null, claimed_by = null, updated_at = now()
                 where id = ?
                """, exhausted ? "FAILED" : "PENDING", Math.max(1, task.getAttemptCount() * 2),
                truncate(errorMessage, 2000), task.getId());
    }

    @Override
    public void enqueueDailyBrief(String eventSchemaVersion, String eventPromptVersion) {
        String input = loadBriefInput(eventSchemaVersion, eventPromptVersion);
        if (input == null) return;
        String hash = sha256(input);
        jdbcTemplate.update("""
                insert into market_information_ai_task
                    (task_type, cluster_content_hash, priority, requested_by)
                select 'DAILY_BRIEF', ?, 5, 'event-analysis'
                 where not exists (
                    select 1 from market_information_ai_task
                     where task_type = 'DAILY_BRIEF' and cluster_content_hash = ?
                       and task_status in ('PENDING', 'RUNNING', 'SUCCEEDED')
                 )
                """, hash, hash);
    }

    private String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash Market Information AI input", exception);
        }
    }
}
