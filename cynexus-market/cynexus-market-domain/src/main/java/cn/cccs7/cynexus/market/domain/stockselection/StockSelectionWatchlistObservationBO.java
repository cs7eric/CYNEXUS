package cn.cccs7.cynexus.market.domain.stockselection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StockSelectionWatchlistObservationBO {
    private StockSelectionWatchlistObservationBO() { }

    public record Item(long watchlistItemId,
                       BigDecimal latestClose,
                       LocalDate latestTradeDate,
                       BigDecimal returnSinceEntryPct,
                       BigDecimal benchmarkReturnPct,
                       BigDecimal excessReturnPct,
                       String dataSource,
                       List<String> warnings) { }

    public record Result(List<Item> items, String dataSource, List<String> warnings) { }

    public record History(long watchlistItemId,
                          LocalDate entryTradeDate,
                          BigDecimal entryPrice,
                          List<Point> points,
                          String dataSource,
                          List<String> warnings) { }

    public record Point(LocalDate tradeDate,
                        BigDecimal close,
                        BigDecimal returnSinceEntryPct,
                        BigDecimal benchmarkReturnPct,
                        BigDecimal excessReturnPct,
                        List<String> warnings) { }
}
