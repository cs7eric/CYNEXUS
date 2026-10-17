package cn.cccs7.cynexus.ai.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompletionResultBO {

    private String content;
    private String providerCode;
    private String modelCode;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private String rawResponse;
    /** Correlation fields kept on the completion result for fallback/repair and logging. */
    private String executionNo;
    private Long latencyMs;
    private String errorType;
}
