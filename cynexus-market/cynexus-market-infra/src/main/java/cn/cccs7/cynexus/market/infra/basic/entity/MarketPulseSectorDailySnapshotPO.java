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
@TableName(value = "market_pulse_sector_daily_snapshots", autoResultMap = true)
public class MarketPulseSectorDailySnapshotPO extends BasePO {

    private Long batchId;
    private LocalDate tradeDate;
    private String sectorName;
    private String sectorIndex;
    private Integer rankByChange;
    private BigDecimal changePct;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal mainNet;
    private Integer stockCount;
    private String leadingStock;
    private BigDecimal leadingChangePct;
    private BigDecimal leadingPrice;
    private String sourceKind;
    private String sourceName;
    private OffsetDateTime capturedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
