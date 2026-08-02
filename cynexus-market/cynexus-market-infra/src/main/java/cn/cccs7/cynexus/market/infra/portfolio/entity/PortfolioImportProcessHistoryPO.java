package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioImportProcessHistoryPO {
    private Long id;
    private Long taskId;
    private String principalKey;
    private String tenantKey;
    private Long eventId;
    private Integer attemptNo;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String messageId;
    private String correlationId;
    private String providerCode;
    private String modelCode;
    private String errorCode;
    private String errorMessage;
    private String details;
    private OffsetDateTime occurredAt;
    private Long durationMs;
    private String events;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
