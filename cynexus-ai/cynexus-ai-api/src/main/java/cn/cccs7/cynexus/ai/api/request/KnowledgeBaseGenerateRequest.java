package cn.cccs7.cynexus.ai.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseGenerateRequest {

    private String sourceText;
    private String preferredModelCode;
    private String tenantCode;
    private String routingQuality;
    private String maxCostTier;
}
