package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("market_limit_daily_stocks")
public class MarketLimitDailyStockPO extends BasePO {

    private LocalDate tradeDate;
    private String code;
    private String name;
    private String category;
    private Integer streak;
    private BigDecimal changePct;
    private BigDecimal limitUpPrice;
    private BigDecimal limitDownPrice;
}
