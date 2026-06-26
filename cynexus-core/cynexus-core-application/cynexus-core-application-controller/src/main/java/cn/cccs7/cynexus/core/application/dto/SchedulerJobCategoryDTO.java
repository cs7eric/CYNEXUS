package cn.cccs7.cynexus.core.application.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobCategoryDTO {

    private Long id;
    private String code;
    private String label;
    private String iconHint;
    private Integer sortOrder;
    private String description;
    private String status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
