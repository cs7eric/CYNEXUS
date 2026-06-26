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
@TableName(value = "sector_fund_flow_daily_snapshots", autoResultMap = true)
public class SectorFundFlowDailySnapshotPO extends BasePO {

    private Long batchId;
    private String scope;
    private LocalDate tradeDate;
    private String sectorCode;
    private String sectorName;
    private Integer rank;
    private BigDecimal changePct;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal net;
    private Integer companyCount;
    private String leaderStock;
    private BigDecimal leaderChange;
    private BigDecimal leaderPrice;
    private String source;
    private String sourceType;
    private OffsetDateTime capturedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
    private String remark;
}
