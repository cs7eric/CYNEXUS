package cn.cccs7.cynexus.market.infra.portfolio;

import cn.cccs7.cynexus.common.messaging.api.MessageEnvelope;
import cn.cccs7.cynexus.common.messaging.api.MessageType;
import cn.cccs7.cynexus.market.domain.portfolio.PortfolioImportDispatchPort;
import cn.cccs7.cynexus.market.infra.portfolio.entity.PortfolioOutboxEventPO;
import cn.cccs7.cynexus.market.infra.portfolio.mapper.PortfolioOutboxMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioImportDispatchAdapter implements PortfolioImportDispatchPort {
    private static final String EVENT_NAME = "portfolio.import.parse-requested";
    private static final String ROUTING_KEY = "portfolio.import.parse-requested.v1";
    private final PortfolioOutboxMapper mapper;
    private final PortfolioIdGeneratorAdapter idGenerator;

    public PortfolioImportDispatchAdapter(PortfolioOutboxMapper mapper, PortfolioIdGeneratorAdapter idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public void dispatch(long taskId, String principalKey, String tenantKey) {
        String messageId = UUID.randomUUID().toString();
        MessageEnvelope envelope = new MessageEnvelope(messageId, MessageType.COMMAND, EVENT_NAME, 1,
                "cynexus-market", "PortfolioImportTask", Long.toString(taskId), Instant.now(),
                messageId, messageId, null, "{\"taskId\":" + taskId + "}",
                Map.of("tenant", tenantKey, "principal", principalKey));
        PortfolioOutboxEventPO event = new PortfolioOutboxEventPO();
        event.setId(idGenerator.nextId());
        event.setMessageId(messageId);
        event.setAggregateType(envelope.aggregateType());
        event.setAggregateId(envelope.aggregateId());
        event.setMessageType(envelope.messageType().name());
        event.setEventName(envelope.name());
        event.setSchemaVersion(envelope.schemaVersion());
        event.setRoutingKey(ROUTING_KEY);
        event.setPayload(envelope.payloadJson());
        event.setHeaders("{\"tenant\":\"" + escape(tenantKey) + "\",\"principal\":\"" + escape(principalKey) + "\"}");
        event.setStatus("PENDING");
        event.setAttemptCount(0);
        event.setAvailableAt(java.time.OffsetDateTime.now());
        mapper.insert(event);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
