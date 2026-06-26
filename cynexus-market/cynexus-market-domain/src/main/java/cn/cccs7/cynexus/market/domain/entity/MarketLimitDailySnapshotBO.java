package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketLimitDailySnapshotBO {

    private Long id;
    private LocalDate tradeDate;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer touchedCount;
    private Integer brokenCount;
    private BigDecimal breakBoardRate;
    private Integer maxStreakHeight;
    private BigDecimal promotionOverallRate;
    private String sentimentLevel;
    private String sentimentText;
    private Integer stockCount;
    private String marketStatus;
    private String dataStatus;
    private String source;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
