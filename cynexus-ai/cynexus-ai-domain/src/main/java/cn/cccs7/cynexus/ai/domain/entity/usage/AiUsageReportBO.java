package cn.cccs7.cynexus.ai.domain.entity.usage;

import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import java.math.BigDecimal;
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
public class AiUsageReportBO {
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
    private BigDecimal totalCost;
    private List<AiUsageBreakdownBO> breakdown;
    private List<AiUsageObservationBO> observations;
}
