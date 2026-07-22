package cn.cccs7.cynexus.ai.domain.agent;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentWriteCommandBO {
    private Long id;
    private String commandNo;
    private String ownerKey;
    private String tenantKey;
    private String principalKey;
    private String conversationNo;
    private String runNo;
    private String toolCode;
    private String argumentsJson;
    private String argumentDigest;
    private String confirmationTokenHash;
    private String status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime executedAt;
    private String resultJson;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
