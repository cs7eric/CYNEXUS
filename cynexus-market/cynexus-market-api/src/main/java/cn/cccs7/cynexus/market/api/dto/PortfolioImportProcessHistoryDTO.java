package cn.cccs7.cynexus.market.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PortfolioImportProcessHistoryDTO {
    long id;
    long taskId;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    List<PortfolioImportProcessHistoryEventDTO> events;
}
