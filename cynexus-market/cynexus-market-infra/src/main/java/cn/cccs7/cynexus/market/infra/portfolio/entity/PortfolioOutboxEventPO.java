package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioOutboxEventPO {
    private Long id;
    private String messageId;
    private String aggregateType;
    private String aggregateId;
    private String messageType;
    private String eventName;
    private Integer schemaVersion;
    private String routingKey;
    private String payload;
    private String headers;
    private String status;
    private Integer attemptCount;
    private OffsetDateTime availableAt;
}
