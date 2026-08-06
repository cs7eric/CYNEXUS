package cn.cccs7.cynexus.market.infra.basic.entity;

import cn.cccs7.cynexus.common.db.entity.BasePO;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("market_indicator_snapshots")
public class MarketIndicatorSnapshotPO extends BasePO {
    private String code; private String name; private String industry; private String market;
    private BigDecimal lastPrice; private BigDecimal prevClose; private BigDecimal openPrice;
    private BigDecimal highPrice; private BigDecimal lowPrice; private BigDecimal changePct;
    private BigDecimal amount; private BigDecimal volume; private BigDecimal turnoverRate;
    private BigDecimal limitUpPrice; private BigDecimal limitDownPrice;
    private String limitStatus; private String source; private OffsetDateTime capturedAt;
}
