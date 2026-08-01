package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.common.messaging.api.MessageDestination;
import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;
import cn.cccs7.cynexus.common.messaging.api.MessageType;
import cn.cccs7.cynexus.common.messaging.outbox.OutboxRecord;
import cn.cccs7.cynexus.common.messaging.outbox.OutboxStore;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioOutboxEventPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioOutboxMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioOutboxStoreAdapter implements OutboxStore {
    private static final TypeReference<Map<String, String>> HEADERS = new TypeReference<>() { };
    private final PortfolioOutboxMapper mapper;
    private final ObjectMapper objectMapper;

    public PortfolioOutboxStoreAdapter(PortfolioOutboxMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OutboxRecord> claimReady(Instant now, int limit, String leaseOwner, Duration leaseDuration) {
        OffsetDateTime current = now.atOffset(ZoneOffset.UTC);
        OffsetDateTime leaseUntil = now.plus(leaseDuration).atOffset(ZoneOffset.UTC);
        return mapper.claimReady(current, limit, leaseOwner, leaseUntil).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    public void markPublished(String recordId, String leaseOwner, Instant publishedAt) {
        mapper.markPublished(Long.parseLong(recordId), leaseOwner, publishedAt.atOffset(ZoneOffset.UTC));
    }

    @Override
    public void markRetry(String recordId, String leaseOwner, Instant availableAt, String errorCode,
                          String errorMessage) {
        mapper.markRetry(Long.parseLong(recordId), leaseOwner, availableAt.atOffset(ZoneOffset.UTC), errorCode,
                errorMessage);
    }

    @Override
    public void markFailed(String recordId, String leaseOwner, String errorCode, String errorMessage) {
        mapper.markFailed(Long.parseLong(recordId), leaseOwner, errorCode, errorMessage);
    }

    private OutboxRecord toRecord(PortfolioOutboxEventPO event) {
        try {
            MessageEnvelope envelope = new MessageEnvelope(
                    event.getMessageId(), MessageType.valueOf(event.getMessageType()), event.getEventName(),
                    event.getSchemaVersion(), "cynexus-market", event.getAggregateType(), event.getAggregateId(),
                    event.getAvailableAt().toInstant(), event.getMessageId(), event.getMessageId(), null,
                    event.getPayload(), objectMapper.readValue(event.getHeaders(), HEADERS));
            return new OutboxRecord(event.getId().toString(),
                    new MessageDestination("cynexus.command", event.getRoutingKey()), envelope,
                    event.getAvailableAt().toInstant(), event.getAttemptCount());
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid portfolio outbox event " + event.getId(), exception);
        }
    }
}
