package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioImportProcessHistoryDTO {
    long id;
    long taskId;
    int attemptNo;
    String eventType;
    String fromStatus;
    String toStatus;
    String messageId;
    String correlationId;
    String providerCode;
    String modelCode;
    String errorCode;
    String errorMessage;
    String details;
    OffsetDateTime occurredAt;
    Long durationMs;
}
