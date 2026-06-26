package cn.cccs7.cynexus.ai.api.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnalysisTargetDTO {

    private Long id;
    private String targetKey;
    private String legacyKey;
    private Long selfSelectedItemId;
    private String symbol;
    private String market;
    private String name;
    private String targetType;
    private String adjust;
    private Boolean enabled;
    private Integer intervalMinutes;
    private String sourceType;
    private String status;
    private Integer sortOrder;
    private String tags;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
