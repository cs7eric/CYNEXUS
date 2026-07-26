package cn.cccs7.cynexus.ai.domain.entity.agent;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentMessageBO {
    private Long id;
    private String conversationNo;
    private String runNo;
    private Integer sequenceNo;
    private String role;
    private String content;
    private String toolCallId;
    private String toolName;
    private String toolCallsJson;
    private String runtimeContext;
    private String providerOutputJson;
    private OffsetDateTime createdAt;
}
