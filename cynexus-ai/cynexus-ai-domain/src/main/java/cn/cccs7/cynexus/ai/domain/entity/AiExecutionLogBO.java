package cn.cccs7.cynexus.ai.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecutionLogBO {

    private Long id;
    private String executionNo;
    private String capability;
    private String promptKey;
    private String promptVersion;
    private String providerCode;
    private String modelCode;
    private String status;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Long latencyMs;
    private String requestJson;
    private String responseJson;
    private String errorMessage;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
