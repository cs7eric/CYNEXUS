package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AgentTaskDTO {
    String taskNo;
    String conversationNo;
    String userInput;
    String preferredModelCode;
    OffsetDateTime scheduleAt;
    String status;
    Integer attemptCount;
    Integer maxAttempts;
    String runNo;
    String lastError;
    OffsetDateTime nextAttemptAt;
    OffsetDateTime completedAt;
}
