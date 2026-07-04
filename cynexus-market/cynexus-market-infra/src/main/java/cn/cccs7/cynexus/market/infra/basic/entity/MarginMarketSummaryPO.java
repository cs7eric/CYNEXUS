package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "margin_market_summary", autoResultMap = true)
public class MarginMarketSummaryPO extends BasePO {

    private LocalDate tradeDate;
    private String exchange;
    private String source;
    private BigDecimal financingBalance;
    private BigDecimal financingBuyAmount;
    private BigDecimal financingRepayAmount;
    private BigDecimal securitiesLendingBalance;
    private BigDecimal securitiesLendingBalanceAmount;
    private BigDecimal securitiesLendingSellVolume;
    private BigDecimal securitiesLendingRepayVolume;
    private BigDecimal marginBalance;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
