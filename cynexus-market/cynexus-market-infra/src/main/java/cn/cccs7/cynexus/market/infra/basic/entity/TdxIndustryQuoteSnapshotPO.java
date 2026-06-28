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
@TableName("tdx_industry_quote_snapshot")
public class TdxIndustryQuoteSnapshotPO extends BasePO {

    private Long batchId;
    private LocalDate snapshotDate;
    private String code6;
    private String name;
    private String fullCode;
    private String exchange;
    private BigDecimal lastPrice;
    private BigDecimal preClosePrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal amount;
    private BigDecimal totalHand;
    private BigDecimal currentHand;
    private String source;
    private OffsetDateTime fetchedAt;
}
