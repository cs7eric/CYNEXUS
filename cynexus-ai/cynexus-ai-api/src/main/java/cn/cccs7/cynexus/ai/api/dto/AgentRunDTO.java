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
    private String modelCode;
    private String planJson;
    private Integer turnCount;
    private Integer toolCallCount;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private List<AgentRunStepDTO> steps;
}
