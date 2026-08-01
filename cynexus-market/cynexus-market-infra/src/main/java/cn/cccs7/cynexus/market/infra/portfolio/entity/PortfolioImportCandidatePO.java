package cn.cccs7.cynexus.market.infra.portfolio.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PortfolioImportCandidatePO {
    private Long id;
    private Long taskId;
    private Integer rowNo;
    private String rawPayload;
    private String normalizedPayload;
    private Double confidence;
    private String warnings;
    private String reviewStatus;
    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
