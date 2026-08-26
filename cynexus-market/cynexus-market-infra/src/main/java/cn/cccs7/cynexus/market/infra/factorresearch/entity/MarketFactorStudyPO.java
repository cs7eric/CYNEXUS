package cn.cccs7.cynexus.market.infra.factorresearch.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketFactorStudyPO {
    private Long id;
    private String studyCode;
    private String tenantId;
    private String ownerId;
    private String canonicalConfig;
    private Integer cohortDefinitionVersion;
    private String featureRegistryVersion;
    private OffsetDateTime dataCutoffAt;
    private String leakageCheckStatus;
    private String status;
    private String progress;
    private String warnings;
    private String errorCode;
    private String idempotencyKey;
    private OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;
}
