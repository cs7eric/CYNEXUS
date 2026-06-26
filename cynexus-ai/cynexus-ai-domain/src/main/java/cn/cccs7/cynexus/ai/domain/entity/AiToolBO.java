package cn.cccs7.cynexus.ai.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiToolBO {

    private String toolCode;
    private String toolName;
    private String description;
    private boolean enabled;
}
