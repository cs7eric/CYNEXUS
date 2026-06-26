package cn.cccs7.cynexus.ai.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateBO {

    private String scenarioCode;
    private String promptVersion;
    private String systemPrompt;
    private String userPromptTemplate;
    private String outputSchemaJson;
    private boolean enabled;
}
