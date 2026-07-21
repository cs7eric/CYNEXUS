package cn.cccs7.cynexus.ai.domain.agent;

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
    private String status;
    private String userInput;
    private String answer;
    private String modelCode;
    private String planJson;
    private String contextJson;
    private String assumptionsJson;
    private String evidenceJson;
    private String warningsJson;
    private String errorMessage;
    private Integer turnCount;
    private Integer toolCallCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
