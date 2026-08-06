package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("market_indicator_events")
public class MarketIndicatorEventPO extends BasePO {
    private LocalDate tradeDate; private String code; private String name; private String market;
    private String eventType; private OffsetDateTime eventAt; private BigDecimal signalValue;
    private BigDecimal lastPrice; private BigDecimal changePct; private String limitStatus; private String source;
}
