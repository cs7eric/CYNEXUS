package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRunDTO {
    private String runNo;
    private String conversationNo;
    private String status;
    private String userInput;
    private String answer;
    private String providerCode;
    private String modelCode;
    private String planJson;
    private String contextJson;
    private String assumptionsJson;
    private String clarificationJson;
    private String evidenceJson;
    private String warningsJson;
    private Integer turnCount;
    private Integer toolCallCount;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private java.math.BigDecimal estimatedCost;
    private String usageSource;
    private String errorMessage;
    private String failureCategory;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private List<AgentRunStepDTO> steps;
}
