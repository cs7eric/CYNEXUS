package cn.cccs7.cynexus.ai.api.request;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AgentTaskRequest {
    private String conversationNo;
    private String userInput;
    private String preferredModelCode;
    private OffsetDateTime scheduleAt;
    private Integer maxAttempts;
}
