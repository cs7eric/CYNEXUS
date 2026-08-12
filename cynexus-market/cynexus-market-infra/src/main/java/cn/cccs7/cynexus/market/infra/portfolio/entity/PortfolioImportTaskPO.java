package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioImportTaskPO {
    private Long id;
    private String principalKey;
    private String tenantKey;
    private Long assetId;
    private String idempotencyKey;
    private OffsetDateTime createdAt;
    private String status;
    private Long version;
    private Integer attemptCount;
    private OffsetDateTime queuedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime confirmedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
}
