package cn.cccs7.cynexus.ai.api.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KnowledgeBaseSearchResultDTO {
    String documentId;
    String content;
    double score;
    Map<String, String> metadata;
}
