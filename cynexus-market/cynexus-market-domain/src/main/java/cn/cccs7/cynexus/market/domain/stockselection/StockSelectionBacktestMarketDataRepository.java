package cn.cccs7.cynexus.market.domain.stockselection;

import java.time.LocalDate;
import java.util.List;

public interface StockSelectionBacktestMarketDataRepository {

    List<LocalDate> listTradingDatesOnOrBefore(LocalDate requestedAsOfDate, int limit);

    List<StockSelectionBacktestBO.DailyBar> listDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols,
                                                           LocalDate startDate,
                                                           LocalDate endDate);

    List<StockSelectionBacktestBO.DailyBar> listLatestDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols);
}
