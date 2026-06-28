package cn.cccs7.cynexus.market.api.dto;

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
public class StockTurnoverSeriesSnapshotDTO {

    private Long id;
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
    private String payload;
    private OffsetDateTime generatedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
