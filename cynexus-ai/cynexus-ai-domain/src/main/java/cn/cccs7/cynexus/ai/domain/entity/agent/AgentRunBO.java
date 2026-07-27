package cn.cccs7.cynexus.ai.domain.entity.agent;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRunBO {

    private Long id;
    private String runNo;
    private String conversationNo;
    private String ownerKey;
    private String idempotencyKey;
    private String status;
    private String userInput;
    private String answer;
    private String providerCode;
    private String modelCode;
    private String reasoningEffort;
    private String planJson;
    private String contextJson;
    private String assumptionsJson;
    private String clarificationJson;
    private String evidenceJson;
    private String warningsJson;
    private String errorMessage;
    private String failureCategory;
    private Integer turnCount;
    private Integer toolCallCount;
    private Integer inputTokens;
    private Integer cachedInputTokens;
    private Integer cacheWriteTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private java.math.BigDecimal estimatedCost;
    private String usageSource;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime cancellationRequestedAt;
    private String cancellationRequestedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
