package cn.cccs7.cynexus.ai.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class KnowledgeBaseAnswerDTO {
    String content;
    List<KnowledgeBaseSearchResultDTO> sources;
}
