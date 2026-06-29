package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "msi_style_risk_daily", autoResultMap = true)
public class StyleRiskDailyPO extends BasePO {

    private LocalDate tradeDate;
    private Integer windowDays;
    private BigDecimal hs300Return;
    private BigDecimal csi1000Return;
    private BigDecimal spread;
    private BigDecimal score;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
