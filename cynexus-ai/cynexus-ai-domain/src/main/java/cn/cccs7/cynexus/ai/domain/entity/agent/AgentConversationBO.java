package cn.cccs7.cynexus.ai.domain.agent;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentConversationBO {

    private Long id;
    private String conversationNo;
    private String ownerKey;
    private String title;
    private String status;
    private String lastRunNo;
    private String lastMessage;
    private String metadataJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
