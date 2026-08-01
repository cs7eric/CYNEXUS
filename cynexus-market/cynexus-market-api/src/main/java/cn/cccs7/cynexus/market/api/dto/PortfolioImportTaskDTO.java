package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioImportTaskDTO {
    private Long id;
    private String status;
    private Long version;
    private Integer attemptCount;
    private Long assetId;
    private OffsetDateTime queuedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime confirmedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private OffsetDateTime createdAt;
}
