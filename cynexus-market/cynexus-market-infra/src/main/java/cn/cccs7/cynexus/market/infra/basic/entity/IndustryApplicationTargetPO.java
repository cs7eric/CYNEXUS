package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mkt_industry_application_target", autoResultMap = true)
public class IndustryApplicationTargetPO extends BasePO {

    private String targetKey;
    private String targetType;
    private String symbol;
    private String name;
    private Boolean enabled;
    private Integer intervalMinutes;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tags;
    private Integer displayOrder;
    private String source;
    private String remark;
}
