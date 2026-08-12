package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProcessBO;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionProcessRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MediaTranscriptionProcessRepositoryImpl implements MediaTranscriptionProcessRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;

    public MediaTranscriptionProcessRepositoryImpl(JdbcTemplate jdbcTemplate, SnowflakeIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    @Override
    public MediaTranscriptionProcessBO findById(String ownerKey, Long id) {
        return jdbcTemplate.query(baseSelect(" AND owner_key = ? AND id = ?"), this::row, ownerKey, id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public MediaTranscriptionProcessBO findByIdempotencyKey(String ownerKey, String idempotencyKey) {
        return jdbcTemplate.query(baseSelect(" AND owner_key = ? AND idempotency_key = ?") + " LIMIT 1",
                        this::row, ownerKey, idempotencyKey).stream().findFirst().orElse(null);
    }

    @Override
    public List<MediaTranscriptionProcessBO> listActive(String ownerKey, int limit) {
        return listActive(ownerKey, limit, 0);
    }

    @Override
    public List<MediaTranscriptionProcessBO> listActive(String ownerKey, int limit, int offset) {
        return listActive(ownerKey, limit, offset, null);
    }

    @Override
    public List<MediaTranscriptionProcessBO> listActive(String ownerKey, int limit, int offset, String status) {
        boolean filtered = status != null && !status.isBlank();
        String suffix = " AND owner_key = ?" + (filtered ? " AND status = ?" : "")
                + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        Object[] arguments = filtered
                ? new Object[]{ownerKey, status, Math.max(1, Math.min(limit, 2_000)), Math.max(0, offset)}
                : new Object[]{ownerKey, Math.max(1, Math.min(limit, 2_000)), Math.max(0, offset)};
        return jdbcTemplate.query(baseSelect(suffix),
                this::row, arguments);
    }

    @Override
    public long countActive(String ownerKey) {
        return countActive(ownerKey, null);
    }

    @Override
    public long countActive(String ownerKey, String status) {
        boolean filtered = status != null && !status.isBlank();
        String sql = "SELECT count(*) FROM media_transcription_process_history WHERE deleted_at IS NULL AND owner_key = ?"
                + (filtered ? " AND status = ?" : "");
        Long count = jdbcTemplate.queryForObject(
                sql, Long.class, filtered ? new Object[]{ownerKey, status} : new Object[]{ownerKey});
        return count == null ? 0 : count;
    }

    @Override
    public Long create(MediaTranscriptionProcessBO process) {
        long id = idGenerator.nextId();
        jdbcTemplate.update("""
                INSERT INTO media_transcription_process_history
                    (id, owner_key, idempotency_key, source_url, download_url, title, platform, note_type, status, phase, progress)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, process.getOwnerKey(), process.getIdempotencyKey(), process.getSourceUrl(), process.getDownloadUrl(), process.getTitle(),
                process.getPlatform(), process.getNoteType(), process.getStatus(), process.getPhase(), process.getProgress());
        return id;
    }

    @Override
    public boolean update(MediaTranscriptionProcessBO process) {
        return jdbcTemplate.update("""
                UPDATE media_transcription_process_history
                   SET download_url = ?, worker_task_id = ?, history_id = ?, status = ?, phase = ?, progress = ?, file_name = ?,
                       transcript = ?, polished = ?, summary = ?, metadata = CAST(? AS jsonb), error_message = ?,
                       attempt_count = ?, started_at = ?, completed_at = ?, updated_at = now()
                 WHERE id = ? AND owner_key = ? AND deleted_at IS NULL
                """,
                process.getDownloadUrl(), process.getWorkerTaskId(), process.getHistoryId(), process.getStatus(), process.getPhase(), process.getProgress(),
                process.getFileName(), process.getTranscript(), process.getPolished(), process.getSummary(),
                process.getMetadataJson() == null ? "{}" : process.getMetadataJson(), process.getErrorMessage(),
                process.getAttemptCount(), process.getStartedAt(), process.getCompletedAt(), process.getId(), process.getOwnerKey()) > 0;
    }

    @Override
    public MediaTranscriptionProcessBO claimNext() {
        return jdbcTemplate.query("""
                WITH candidate AS (
                    SELECT id
                      FROM media_transcription_process_history
                     WHERE deleted_at IS NULL
                       AND (status = 'queued' OR (status = 'running' AND updated_at < now() - interval '2 hours'))
                     ORDER BY created_at, id
                     FOR UPDATE SKIP LOCKED
                     LIMIT 1
                )
                UPDATE media_transcription_process_history p
                   SET status = 'running', phase = CASE WHEN p.status = 'running' THEN p.phase ELSE 'starting' END,
                       progress = CASE WHEN p.status = 'running' THEN p.progress ELSE 2 END,
                       attempt_count = COALESCE(p.attempt_count, 0) + 1,
                       started_at = COALESCE(p.started_at, now()), updated_at = now()
                  FROM candidate
                 WHERE p.id = candidate.id
                RETURNING p.*
                """, this::row).stream().findFirst().orElse(null);
    }

    private String baseSelect(String suffix) {
        return "SELECT * FROM media_transcription_process_history WHERE deleted_at IS NULL" + suffix;
    }

    private MediaTranscriptionProcessBO row(ResultSet rs, int rowNum) throws SQLException {
        return MediaTranscriptionProcessBO.builder()
                .id(rs.getLong("id"))
                .ownerKey(rs.getString("owner_key"))
                .idempotencyKey(rs.getString("idempotency_key"))
                .sourceUrl(rs.getString("source_url"))
                .downloadUrl(rs.getString("download_url"))
                .title(rs.getString("title"))
                .platform(rs.getString("platform"))
                .noteType(rs.getString("note_type"))
                .workerTaskId(rs.getString("worker_task_id"))
                .historyId((Long) rs.getObject("history_id"))
                .status(rs.getString("status"))
                .phase(rs.getString("phase"))
                .progress((Integer) rs.getObject("progress"))
                .fileName(rs.getString("file_name"))
                .transcript(rs.getString("transcript"))
                .polished(rs.getString("polished"))
                .summary(rs.getString("summary"))
                .metadataJson(rs.getString("metadata"))
                .errorMessage(rs.getString("error_message"))
                .attemptCount((Integer) rs.getObject("attempt_count"))
                .startedAt(rs.getObject("started_at", java.time.OffsetDateTime.class))
                .completedAt(rs.getObject("completed_at", java.time.OffsetDateTime.class))
                .createdAt(rs.getObject("created_at", java.time.OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class))
                .deletedAt(rs.getObject("deleted_at", java.time.OffsetDateTime.class))
                .build();
    }
}
