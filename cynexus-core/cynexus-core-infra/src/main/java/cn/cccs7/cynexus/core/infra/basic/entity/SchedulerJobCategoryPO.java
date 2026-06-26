package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_job_categories")
public class SchedulerJobCategoryPO extends BasePO {

    private String code;
    private String label;
    private String iconHint;
    private Integer sortOrder;
    private String description;
    private String status;
    private String remark;
}
