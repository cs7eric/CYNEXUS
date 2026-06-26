package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scheduler_jobs")
public class SchedulerJobPO extends BasePO {

    private String code;
    private String name;
    private String description;
    private String serviceModule;
    private String serviceClass;
    private String configFile;
    @TableField("is_enabled")
    private Boolean enabled;
    private OffsetDateTime registeredAt;
    private String extra;
    private String remark;
}
