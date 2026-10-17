package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class KnowledgeBaseAnswerRequest {
    private String tenantKey;
    private String principalKey;
    private String question;
    private Integer topK;
    private String preferredModelCode;
    private String permissionScope;
}
