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
@TableName(value = "stock_fund_flow_snapshot", autoResultMap = true)
public class StockFundFlowSnapshotPO extends BasePO {

    private Long batchId;
    private LocalDate tradeDate;
    private String code;
    private String symbol;
    private String name;
    private BigDecimal lastPrice;
    private BigDecimal preClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal volumeLots;
    private BigDecimal amountWan;
    private BigDecimal turnoverRate;
    private BigDecimal outerDiscLots;
    private BigDecimal insideDishLots;
    private BigDecimal activeNetLots;
    private BigDecimal activeNetAmountWan;
    private BigDecimal activeBuyRatio;
    private BigDecimal activeSellRatio;
    private BigDecimal buyBigRatio;
    private BigDecimal buySmallRatio;
    private BigDecimal sellBigRatio;
    private BigDecimal sellSmallRatio;
    private BigDecimal pe;
    private BigDecimal pb;
    private BigDecimal amplitude;
    private BigDecimal floatMarketCapYi;
    private BigDecimal totalMarketCapYi;
    private String source;
    private String sourceType;
    private OffsetDateTime capturedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
