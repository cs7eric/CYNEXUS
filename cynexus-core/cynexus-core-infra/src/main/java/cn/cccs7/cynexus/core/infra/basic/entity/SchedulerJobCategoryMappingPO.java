package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import lombok.Data;

@Data
public class SchedulerJobCategoryMappingPO extends BasePO {

    private Long jobId;
    private Long categoryId;
    private String remark;
    private Integer sortOrder;
}
