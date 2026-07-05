package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_prompt", autoResultMap = true)
public class AiPromptPO extends BasePO {

    private String promptKey;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String scenarioCode;
    private String promptVersion;
    private String status;
    private String systemPrompt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String userPromptTemplate;
    @TableField(typeHandler = JsonbTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private String outputSchema;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String variables;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tags;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
}
