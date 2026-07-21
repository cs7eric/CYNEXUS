package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentMessageDTO {
    private Integer sequenceNo;
    private String role;
    private String content;
    private String toolCallId;
    private String toolName;
    private String toolCallsJson;
    private OffsetDateTime createdAt;
}
