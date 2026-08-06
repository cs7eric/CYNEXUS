package cn.cccs7.cynexus.wechat.infra.repository;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistory;
import cn.cccs7.cynexus.wechat.domain.model.WechatEventHistoryPage;
import cn.cccs7.cynexus.wechat.domain.model.WechatImageRoute;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.domain.repository.WechatEventHistoryRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class WechatEventHistoryRepositoryImpl implements WechatEventHistoryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatEventHistoryRepositoryImpl(JdbcTemplate jdbcTemplate, SnowflakeIdGenerator idGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.idGenerator = idGenerator;
    }

    @Override
    public long findActiveIdByMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) return 0L;
        Long id = jdbcTemplate.query(
                "SELECT id FROM cynexus_appl_wechat.wechat_event_history WHERE message_id = ? AND deleted_at IS NULL",
                (rs, rowNum) -> rs.getLong(1), messageId).stream().findFirst().orElse(null);
        return id == null ? 0L : id;
    }

    @Override
    public long create(WechatEventHistory event) {
        long existingId = findActiveIdByMessageId(event.messageId());
        if (existingId > 0L) return existingId;

        long id = idGenerator.nextId();
        try {
            jdbcTemplate.update("""
                    INSERT INTO cynexus_appl_wechat.wechat_event_history
                        (id, message_id, direction, message_type, event_type, from_user_name, to_user_name,
                         content, media_id, pic_url, processing_status, reply_content, error_message)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, event.messageId(), event.direction(), event.messageType(), event.eventType(),
                    event.fromUserName(), event.toUserName(), event.content(), event.mediaId(), event.picUrl(),
                    event.processingStatus(), event.replyContent(), event.errorMessage());
        } catch (DuplicateKeyException exception) {
            long racedId = findActiveIdByMessageId(event.messageId());
            if (racedId > 0L) return racedId;
            throw exception;
        }
        return id;
    }

    @Override
    public boolean attachImage(long id, WechatStoredImage image) {
        return jdbcTemplate.update("""
                UPDATE cynexus_appl_wechat.wechat_event_history
                   SET storage_path = ?, image_content_type = ?, image_size = ?, updated_at = now()
                 WHERE id = ? AND deleted_at IS NULL
                """, image.storagePath(), image.contentType(), image.size(), id) > 0;
    }

    @Override
    public boolean markFailed(long id, String errorMessage) {
        return jdbcTemplate.update("""
                UPDATE cynexus_appl_wechat.wechat_event_history
                   SET processing_status = 'FAILED', error_message = ?, updated_at = now()
                 WHERE id = ? AND deleted_at IS NULL
                """, limit(errorMessage, 2000), id) > 0;
    }

    @Override
    public WechatEventHistory claimNextImage() {
        return jdbcTemplate.query("""
                WITH candidate AS (
                    SELECT id
                      FROM cynexus_appl_wechat.wechat_event_history
                     WHERE deleted_at IS NULL
                       AND message_type = 'image'
                       AND storage_path IS NOT NULL
                       AND (
                           processing_status = 'RECEIVED'
                           OR (processing_status = 'ROUTING' AND updated_at < now() - interval '10 minutes')
                       )
                     ORDER BY occurred_at, id
                     FOR UPDATE SKIP LOCKED
                     LIMIT 1
                )
                UPDATE cynexus_appl_wechat.wechat_event_history history
                   SET processing_status = 'ROUTING', updated_at = now()
                  FROM candidate
                 WHERE history.id = candidate.id
                RETURNING history.*
                """, this::row).stream().findFirst().orElse(null);
    }

    @Override
    public boolean completeRouting(long id, WechatImageRoute route) {
        return jdbcTemplate.update("""
                UPDATE cynexus_appl_wechat.wechat_event_history
                   SET processing_status = 'ROUTED', route_code = ?, route_confidence = ?, route_reason = ?,
                       route_signals = CAST(? AS jsonb), route_warnings = CAST(? AS jsonb), error_message = NULL,
                       updated_at = now()
                 WHERE id = ? AND deleted_at IS NULL
                """, route.route(), route.confidence(), limit(route.reason(), 2000), json(route.signals()),
                json(route.warnings()), id) > 0;
    }

    @Override
    public boolean failRouting(long id, String errorMessage) {
        return jdbcTemplate.update("""
                UPDATE cynexus_appl_wechat.wechat_event_history
                   SET processing_status = 'ROUTING_FAILED', error_message = ?, updated_at = now()
                 WHERE id = ? AND deleted_at IS NULL
                """, limit(errorMessage, 2000), id) > 0;
    }

    @Override
    public WechatEventHistoryPage list(int limit, int offset, String messageType, String processingStatus) {
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 200));
        int safeOffset = Math.max(offset, 0);
        List<Object> filters = new ArrayList<>();
        String where = filterSql(messageType, processingStatus, filters);
        List<WechatEventHistory> items = jdbcTemplate.query("""
                SELECT * FROM cynexus_appl_wechat.wechat_event_history
                """ + where + " ORDER BY occurred_at DESC, id DESC LIMIT ? OFFSET ?",
                this::row, append(filters, safeLimit, safeOffset));
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cynexus_appl_wechat.wechat_event_history" + where,
                Long.class, filters.toArray());
        return new WechatEventHistoryPage(items, total == null ? 0L : total, safeLimit, safeOffset);
    }

    private String filterSql(String messageType, String processingStatus, List<Object> args) {
        StringBuilder sql = new StringBuilder(" WHERE deleted_at IS NULL");
        if (messageType != null && !messageType.isBlank()) {
            sql.append(" AND message_type = ?");
            args.add(messageType.trim());
        }
        if (processingStatus != null && !processingStatus.isBlank()) {
            sql.append(" AND processing_status = ?");
            args.add(processingStatus.trim());
        }
        return sql.toString();
    }

    private Object[] append(List<Object> filters, Object... values) {
        List<Object> args = new ArrayList<>(filters);
        for (Object value : values) args.add(value);
        return args.toArray();
    }

    private WechatEventHistory row(ResultSet rs, int rowNum) throws SQLException {
        return new WechatEventHistory(
                rs.getLong("id"),
                rs.getString("message_id"),
                rs.getString("direction"),
                rs.getString("message_type"),
                rs.getString("event_type"),
                rs.getString("from_user_name"),
                rs.getString("to_user_name"),
                rs.getString("content"),
                rs.getString("media_id"),
                rs.getString("pic_url"),
                rs.getString("storage_path"),
                rs.getString("image_content_type"),
                (Long) rs.getObject("image_size"),
                rs.getString("processing_status"),
                rs.getString("route_code"),
                (BigDecimal) rs.getObject("route_confidence"),
                rs.getString("route_reason"),
                parseStrings(rs.getString("route_signals")),
                parseStrings(rs.getString("route_warnings")),
                rs.getString("reply_content"),
                rs.getString("error_message"),
                rs.getObject("occurred_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private List<String> parseStrings(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            String[] values = objectMapper.readValue(value, String[].class);
            return values == null ? List.of() : List.of(values);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
