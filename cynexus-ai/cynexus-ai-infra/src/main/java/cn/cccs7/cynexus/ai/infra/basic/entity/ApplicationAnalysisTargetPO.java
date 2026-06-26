package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "application_analysis_targets", autoResultMap = true)
public class ApplicationAnalysisTargetPO extends BasePO {

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
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tags;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
