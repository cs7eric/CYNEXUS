package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class AgentWritePreviewRequest {
    private String toolCode;
    private String conversationNo;
    private String runNo;
    private String argumentsJson;
}
