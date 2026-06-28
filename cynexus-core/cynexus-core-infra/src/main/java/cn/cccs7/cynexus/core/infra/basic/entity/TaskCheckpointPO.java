package cn.cccs7.cynexus.core.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task_checkpoint", autoResultMap = true)
public class TaskCheckpointPO extends BasePO {

    private String checkpointKey;
    private String jobKey;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String checkpointValue;
}
