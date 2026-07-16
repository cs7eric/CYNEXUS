package cn.cccs7.cynexus.ai.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageObservationDTO {
    private String id;
    private String name;
    private String providerCode;
    private String modelCode;
    private String status;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal totalCost;
    private OffsetDateTime startedAt;
    private Long latencyMs;
    private String statusMessage;
}
