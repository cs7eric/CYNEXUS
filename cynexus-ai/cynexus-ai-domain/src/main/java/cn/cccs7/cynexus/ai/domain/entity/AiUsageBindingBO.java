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
public class AiUsageBindingBO {

    private Long id;
    private String capability;
    private String label;
    private Long providerId;
    private Long promptId;
    private String modelOverride;
    private Boolean enabled;
    private String params;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
