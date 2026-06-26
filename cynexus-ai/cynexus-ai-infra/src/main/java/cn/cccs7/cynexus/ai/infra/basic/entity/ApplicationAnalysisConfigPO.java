package cn.cccs7.cynexus.ai.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "application_analysis_configs", autoResultMap = true)
public class ApplicationAnalysisConfigPO extends BasePO {

    private String configKey;
    private String status;
    private Integer horizonDays;
    private Integer horizonSegments;
    private Integer monthlyKeep;
    private Integer weeklyKeep;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
