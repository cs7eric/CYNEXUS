package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sec_trade_calendar")
public class TradeCalendarPO extends BasePO {

    private LocalDate tradeDate;
    @TableField("is_trading_day")
    private Boolean tradingDay;
    private String reason;
    private String source;
    private OffsetDateTime ingestedAt;
}
