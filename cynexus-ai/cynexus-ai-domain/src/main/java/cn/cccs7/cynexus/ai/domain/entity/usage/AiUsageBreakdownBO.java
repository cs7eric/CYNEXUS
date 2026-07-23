package cn.cccs7.cynexus.ai.domain.entity.usage;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageBreakdownBO {
    private String key;
    private String providerCode;
    private String modelCode;
    private String capability;
    private long requests;
    private long succeeded;
    private long failed;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private BigDecimal totalCost;
}
