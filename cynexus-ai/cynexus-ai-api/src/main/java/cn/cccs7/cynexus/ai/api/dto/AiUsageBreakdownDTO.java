package cn.cccs7.cynexus.ai.api.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageBreakdownDTO {
    private String key;
    private String providerCode;
    private String modelCode;
    private String capability;
    private Long requests;
    private Long succeeded;
    private Long failed;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private BigDecimal totalCost;
}
