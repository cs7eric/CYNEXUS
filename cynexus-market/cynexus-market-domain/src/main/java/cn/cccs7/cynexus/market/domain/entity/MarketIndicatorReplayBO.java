package cn.cccs7.cynexus.market.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bounded, read-only evidence packet for the indicator replay workbench.
 * Missing minute data is represented explicitly instead of silently falling
 * back to another trading day.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicatorReplayBO {
    private Boolean ok;
    private String error;
    private LocalDate tradeDate;
    private LocalDate dataTradeDate;
    private String dataStatus;
    private String source;
    private OffsetDateTime requestedFrom;
    private OffsetDateTime requestedTo;
    private List<IndexMinuteBarBO> indexPoints;
    private List<MarketIndicatorEventBO> events;
    private List<MarketIndicatorIndustrySnapshotBO> industryPoints;
    private String message;
}
