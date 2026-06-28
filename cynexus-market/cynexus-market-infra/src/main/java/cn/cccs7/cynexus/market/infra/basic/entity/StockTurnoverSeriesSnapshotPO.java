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
@TableName(value = "stock_turnover_series_snapshot", autoResultMap = true)
public class StockTurnoverSeriesSnapshotPO extends BasePO {

    private String symbol;
    private String targetType;
    private String period;
    private String adjust;
    private BigDecimal circulatingShares;
    private BigDecimal totalShares;
    private LocalDate latestTradeDate;
    private BigDecimal latestTurnoverRate;
    private Integer entryCount;
    private String source;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private OffsetDateTime generatedAt;
}
