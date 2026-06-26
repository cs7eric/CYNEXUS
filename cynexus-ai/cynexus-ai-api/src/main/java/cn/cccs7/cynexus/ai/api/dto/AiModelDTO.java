package cn.cccs7.cynexus.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelDTO {

    private String modelCode;
    private String providerCode;
    private String modelName;
    private boolean toolCallSupported;
    private boolean structuredOutputSupported;
    private boolean enabled;
}
