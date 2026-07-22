package cn.cccs7.cynexus.ai.domain.agent;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentTaskBO {
    private Long id;
    private String taskNo;
    private String ownerKey;
    private String tenantKey;
    private String principalKey;
    private String conversationNo;
    private String userInput;
    private String preferredModelCode;
    private OffsetDateTime scheduleAt;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private String leaseOwner;
    private OffsetDateTime leaseUntil;
    private OffsetDateTime nextAttemptAt;
    private String runNo;
    private String lastError;
    private OffsetDateTime pausedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
