package cn.cccs7.cynexus.market.domain.factorresearch;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorReplayBO;
import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** A bounded event replay; all source quality remains visible to callers. */
public final class EventReplayBO {
    private EventReplayBO() {
    }

    public record Query(String code, LocalDate tradeDate, OffsetDateTime anchorAt, int minuteLimit) {
    }

    public record Result(String code, LocalDate tradeDate, OffsetDateTime anchorAt, String status,
                         String source, OffsetDateTime dataCutoffAt, Coverage coverage,
                         List<String> warnings, List<StockChartBO.Bar> stockMinuteBars,
                         MarketIndicatorReplayBO marketReplay) {
        public Result {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            stockMinuteBars = stockMinuteBars == null ? List.of() : List.copyOf(stockMinuteBars);
        }
    }

    /** Source-level coverage is explicit so callers never infer completeness from a ready status. */
    public record Coverage(int stockBarsRead, int stockBarsIncluded, boolean marketAvailable,
                           boolean industryAvailable, boolean stockAvailable) {
    }
}
