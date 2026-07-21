package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentConversationDTO {
    private String conversationNo;
    private String title;
    private String status;
    private String lastRunNo;
    private String lastMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
