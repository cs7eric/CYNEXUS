package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_usage_bindings", autoResultMap = true)
public class AiUsageBindingPO extends BasePO {

    private String capability;
    private String label;
    private Long providerId;
    private Long promptId;
    private String modelOverride;
    @TableField("is_enabled")
    private Boolean enabled;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String params;
    private String remark;
}
