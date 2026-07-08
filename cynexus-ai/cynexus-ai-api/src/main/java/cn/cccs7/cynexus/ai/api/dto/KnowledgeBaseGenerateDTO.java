package cn.cccs7.cynexus.ai.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseGenerateDTO {

    private String title;
    private String description;
    private List<String> tags;
    private List<String> categories;
    private String content;
}
