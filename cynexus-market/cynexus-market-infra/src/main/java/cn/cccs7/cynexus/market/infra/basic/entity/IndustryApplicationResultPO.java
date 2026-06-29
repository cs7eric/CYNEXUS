package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mkt_industry_application_result", autoResultMap = true)
public class IndustryApplicationResultPO extends BasePO {

    private String targetKey;
    private String targetType;
    private String symbol;
    private String name;
    private OffsetDateTime updatedAtResult;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String kline;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String indicators;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String meta;
    private String source;
    private String remark;
}
