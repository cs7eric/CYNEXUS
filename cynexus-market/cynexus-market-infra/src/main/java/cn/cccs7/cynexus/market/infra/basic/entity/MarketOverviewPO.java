package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "market_overview_snapshots", autoResultMap = true)
public class MarketOverviewPO extends BasePO {

    private LocalDate tradeDate;
    private BigDecimal totalAmount;
    private BigDecimal totalVolume;
    private Integer risingCount;
    private Integer fallingCount;
    private Integer flatCount;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer stockCount;
    private BigDecimal mainNetInflow;
    private BigDecimal superLargeNetInflow;
    private BigDecimal largeNetInflow;
    private BigDecimal mediumNetInflow;
    private BigDecimal smallNetInflow;
    private BigDecimal mainNetInflowRatio;
    private BigDecimal superLargeNetRatio;
    private BigDecimal largeNetRatio;
    private BigDecimal mediumNetRatio;
    private BigDecimal smallNetRatio;
    private String source;
    @TableField("is_manual_override")
    private Boolean manualOverride;
    private OffsetDateTime manualUpdatedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
