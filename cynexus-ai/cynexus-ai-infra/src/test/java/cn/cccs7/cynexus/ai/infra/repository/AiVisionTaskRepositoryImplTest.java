package cn.cccs7.cynexus.ai.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class AiVisionTaskRepositoryImplTest {

    @Test
    void mapsSmallintProgressAndReturnsClaimToken() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(42L);
        when(resultSet.getString("images_json")).thenReturn("[]");
        when(resultSet.getString("task_status")).thenReturn("running");
        when(resultSet.getString("claimed_by")).thenReturn("ai-vision-worker:claim-token");
        when(resultSet.getObject("progress")).thenReturn(Short.valueOf((short) 5));
        when(resultSet.getObject("attempt_count")).thenReturn(1);
        when(resultSet.getObject("max_attempts")).thenReturn(2);
        doAnswer(invocation -> {
            RowMapper<AiVisionTaskBO> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(resultSet, 0));
        }).when(jdbcTemplate).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AiVisionTaskBO>>any(), any(Object[].class));

        AiVisionTaskBO task = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper()).claimNext("ai-vision-worker");

        assertThat(task.getProgress()).isEqualTo(5);
        assertThat(task.getClaimToken()).isEqualTo("ai-vision-worker:claim-token");
    }

    @Test
    void mapsPostgresTimestampObjectsThroughTypedJdbcReads() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        OffsetDateTime created = OffsetDateTime.parse("2026-10-24T15:30:00Z");
        OffsetDateTime started = created.plusSeconds(1);
        OffsetDateTime completed = created.plusSeconds(5);
        when(resultSet.getLong("id")).thenReturn(42L);
        when(resultSet.getString("images_json")).thenReturn("[]");
        for (String column : List.of("started_at", "completed_at", "created_at", "updated_at")) {
            // PostgreSQL exposes timestamptz as Timestamp for untyped reads.
            when(resultSet.getObject(column)).thenReturn(Timestamp.from(created.toInstant()));
        }
        when(resultSet.getObject("started_at", OffsetDateTime.class)).thenReturn(started);
        when(resultSet.getObject("completed_at", OffsetDateTime.class)).thenReturn(completed);
        when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(created);
        when(resultSet.getObject("updated_at", OffsetDateTime.class)).thenReturn(completed);
        doAnswer(invocation -> {
            RowMapper<AiVisionTaskBO> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(resultSet, 0));
        }).when(jdbcTemplate).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AiVisionTaskBO>>any(), any(Object[].class));

        AiVisionTaskBO task = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper()).find(42L, "tenant", "principal");

        assertThat(task.getStartedAt()).isEqualTo(started);
        assertThat(task.getCompletedAt()).isEqualTo(completed);
        assertThat(task.getCreatedAt()).isEqualTo(created);
        assertThat(task.getUpdatedAt()).isEqualTo(completed);
    }

    @Test
    void terminalUpdatesRequireTheCurrentClaimToken() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiVisionTaskRepositoryImpl repository = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper());
        AiVisionTaskBO task = AiVisionTaskBO.builder().id(42L).claimToken("claim-42").build();

        repository.markSucceeded(task);
        repository.markFailed(task, "failure");

        verify(jdbcTemplate).update(contains("images_json = '[]'::jsonb"),
                isNull(), isNull(), isNull(), isNull(), eq(42L), eq("claim-42"));
        verify(jdbcTemplate).update(contains("where id = ? and task_status = 'running' and claimed_by = ?"),
                eq("failure"), eq(42L), eq("claim-42"));
    }

    @Test
    void releaseClaimRequeuesTheClaimAndRollsBackTheClaimAttempt() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiVisionTaskRepositoryImpl repository = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper());
        AiVisionTaskBO task = AiVisionTaskBO.builder().id(42L).claimToken("claim-42").build();

        repository.releaseClaim(task);

        verify(jdbcTemplate).update(contains("task_status = 'queued'"), eq(42L), eq("claim-42"));
    }

    @Test
    void ownerPollingDoesNotLoadStoredImagePayloads() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiVisionTaskRepositoryImpl repository = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper());

        repository.find(42L, "tenant", "principal");

        verify(jdbcTemplate).query(contains("'[]'::jsonb as images_json"),
                org.mockito.ArgumentMatchers.<RowMapper<AiVisionTaskBO>>any(), eq(42L), eq("tenant"), eq("principal"));
    }

    @Test
    void failedRetryTransfersImagesUnderARowLockAndEnforcesTheCutoff() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiVisionTaskRepositoryImpl repository = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper());
        OffsetDateTime cutoff = OffsetDateTime.parse("2026-10-17T00:00:00Z");
        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), any(Object[].class)))
                .thenReturn(List.of());

        assertThat(repository.retryFailed(42L, "tenant", "principal", cutoff)).isNull();

        org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(),
                eq(42L), eq("tenant"), eq("principal"), eq(cutoff));
        assertThat(sql.getValue()).contains("completed_at > ? and jsonb_array_length(images_json) > 0", "for update",
                "source.preferred_model_code, source.images_json", "set images_json = '[]'::jsonb",
                "where id in (select retry_of_id from retry)", "on conflict (retry_of_id)");
    }

    @Test
    void cleanupPassesTheDomainCutoffAndOnlyScrubsTerminalInputs() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiVisionTaskRepositoryImpl repository = new AiVisionTaskRepositoryImpl(jdbcTemplate, new ObjectMapper());
        OffsetDateTime cutoff = OffsetDateTime.parse("2026-10-17T00:00:00Z");
        when(jdbcTemplate.update(anyString(), eq(cutoff))).thenReturn(3);

        assertThat(repository.purgeTerminalInputs(cutoff)).isEqualTo(3);

        org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture(), eq(cutoff));
        assertThat(sql.getValue()).contains("images_json = '[]'::jsonb", "task_status = 'succeeded'",
                "task_status = 'failed' and coalesce(completed_at, updated_at, created_at) <= ?");
    }
}
