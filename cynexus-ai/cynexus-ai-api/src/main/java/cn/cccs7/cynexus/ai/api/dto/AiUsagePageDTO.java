package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsagePageDTO {
    private String source;
    private boolean available;
    private String message;
    private OffsetDateTime from;
    private OffsetDateTime to;
    private long requests;
    private long succeeded;
    private long failed;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private java.math.BigDecimal totalCost;
    private List<AiUsageBreakdownDTO> breakdown;
    private List<AiUsageObservationDTO> observations;
}
