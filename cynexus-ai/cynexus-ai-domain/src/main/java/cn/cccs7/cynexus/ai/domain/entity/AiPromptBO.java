package cn.cccs7.cynexus.ai.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptBO {

    private Long id;
    private String promptKey;
    private String name;
    private String scenarioCode;
    private String promptVersion;
    private String status;
    private String systemPrompt;
    private String userPromptTemplate;
    private String outputSchema;
    private String variables;
    private String tags;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
