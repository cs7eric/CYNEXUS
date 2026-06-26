package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_job_category_mappings")
public class SchedulerJobCategoryMappingPO extends BasePO {

    private Long jobId;
    private Long categoryId;
    private String remark;
    private Integer sortOrder;
}
