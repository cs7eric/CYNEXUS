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
@TableName(value = "ths_industry_constituent_snapshot", autoResultMap = true)
public class ThsIndustryConstituentSnapshotPO extends BasePO {

    private Long batchId;
    private LocalDate snapshotDate;
    private String industryCode;
    private String stockCode;
    private String stockName;
    private Integer rank;
    private BigDecimal price;
    private BigDecimal changePct;
    private BigDecimal change;
    private BigDecimal speedPct;
    private BigDecimal turnoverPct;
    private BigDecimal volumeRatio;
    private BigDecimal amplitudePct;
    private String amountText;
    private String floatSharesText;
    private String floatMarketCapText;
    private BigDecimal pe;
    private String source;
    private String sourceType;
    private OffsetDateTime capturedAt;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extra;
}
