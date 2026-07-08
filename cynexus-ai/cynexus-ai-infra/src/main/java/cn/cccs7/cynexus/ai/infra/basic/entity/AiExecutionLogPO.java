package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_execution_log", autoResultMap = true)
public class AiExecutionLogPO extends BasePO {

    private String executionNo;
    private String capability;
    private String promptKey;
    private String promptVersion;
    private String providerCode;
    private String modelCode;
    private String status;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Long latencyMs;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String requestJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String responseJson;
    private String errorMessage;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
