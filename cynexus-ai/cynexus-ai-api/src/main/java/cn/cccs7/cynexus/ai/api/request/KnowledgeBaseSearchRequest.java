package cn.cccs7.cynexus.ai.api.request;

import lombok.Data;

@Data
public class KnowledgeBaseSearchRequest {
    private String tenantKey;
    private String principalKey;
    private String query;
    private Integer topK;
    /** owner (default), tenant/shared, or public visibility. */
    private String permissionScope;
}
