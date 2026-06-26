package cn.cccs7.cynexus.ai.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapability {

    private boolean toolCallSupported;
    private boolean structuredOutputSupported;
    private boolean visionSupported;
}
