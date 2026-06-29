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
@TableName(value = "msi_index_daily", autoResultMap = true)
public class MsiIndexDailyPO extends BasePO {

    private LocalDate tradeDate;
    private BigDecimal volScore;
    private BigDecimal turnoverScore;
    private BigDecimal priceStrengthScore;
    private BigDecimal riskAppetiteScore;
    private BigDecimal breadthScore;
    private BigDecimal limitEmotionScore;
    private BigDecimal profitEffectScore;
    private BigDecimal sectorBreadthScore;
    private BigDecimal styleRiskScore;
    private BigDecimal compositeScore;
    private Integer componentCount;
    private String level;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    private String extra;
    private String remark;
}
