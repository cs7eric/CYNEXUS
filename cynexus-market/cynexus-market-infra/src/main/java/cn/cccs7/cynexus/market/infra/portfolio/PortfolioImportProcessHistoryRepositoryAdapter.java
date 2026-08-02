package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessEventType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryAggregateBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryBO;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportProcessHistoryRepository;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioImportProcessHistoryPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioImportProcessHistoryMapper;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioIdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioImportProcessHistoryRepositoryAdapter implements PortfolioImportProcessHistoryRepository {
    private final PortfolioImportProcessHistoryMapper mapper;
    private final PortfolioIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public PortfolioImportProcessHistoryRepositoryAdapter(PortfolioImportProcessHistoryMapper mapper,
                                                           PortfolioIdGenerator idGenerator,
                                                           ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(PortfolioImportProcessHistoryBO history) {
        PortfolioImportProcessHistoryPO po = new PortfolioImportProcessHistoryPO();
        long eventId = history.id() > 0 ? history.id() : idGenerator.nextId();
        po.setId(eventId);
        po.setEventId(eventId);
        po.setTaskId(history.taskId());
        po.setPrincipalKey(history.principalKey());
        po.setTenantKey(history.tenantKey());
        po.setAttemptNo(history.attemptNo());
        po.setEventType(history.eventType().name());
        po.setFromStatus(history.fromStatus());
        po.setToStatus(history.toStatus());
        po.setMessageId(history.messageId());
        po.setCorrelationId(history.correlationId());
        po.setProviderCode(history.providerCode());
        po.setModelCode(history.modelCode());
        po.setErrorCode(history.errorCode());
        po.setErrorMessage(history.errorMessage());
        po.setDetails(history.details());
        po.setOccurredAt(history.occurredAt());
        po.setDurationMs(history.durationMs());
        if (mapper.append(po) != 1) throw new IllegalStateException("Unable to persist portfolio process history");
    }

    @Override
    public Optional<PortfolioImportProcessHistoryAggregateBO> findByTaskId(String principalKey, String tenantKey, long taskId) {
        PortfolioImportProcessHistoryPO po = mapper.findByTaskId(principalKey, tenantKey, taskId);
        if (po == null) return Optional.empty();
        return Optional.of(new PortfolioImportProcessHistoryAggregateBO(po.getId(), po.getTaskId(),
                po.getPrincipalKey(), po.getTenantKey(), toEvents(po), po.getCreatedAt(), po.getUpdatedAt()));
    }

    private List<PortfolioImportProcessHistoryBO> toEvents(PortfolioImportProcessHistoryPO po) {
        try {
            JsonNode root = objectMapper.readTree(po.getEvents());
            List<PortfolioImportProcessHistoryBO> events = new ArrayList<>();
            if (root != null && root.isArray()) {
                for (JsonNode event : root) {
                    events.add(new PortfolioImportProcessHistoryBO(event.path("id").asLong(), po.getTaskId(),
                            po.getPrincipalKey(), po.getTenantKey(), event.path("attemptNo").asInt(),
                            PortfolioImportProcessEventType.valueOf(event.path("eventType").asText()),
                            nullableText(event, "fromStatus"), nullableText(event, "toStatus"),
                            nullableText(event, "messageId"), nullableText(event, "correlationId"),
                            nullableText(event, "providerCode"), nullableText(event, "modelCode"),
                            nullableText(event, "errorCode"), nullableText(event, "errorMessage"),
                            event.path("details").isMissingNode() ? "{}" : event.path("details").toString(),
                            OffsetDateTime.parse(event.path("occurredAt").asText()),
                            event.path("durationMs").isNull() || event.path("durationMs").isMissingNode()
                                    ? null : event.path("durationMs").asLong()));
                }
            }
            return List.copyOf(events);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read portfolio process history", exception);
        }
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
