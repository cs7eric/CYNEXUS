package cn.cccs7.cynexus.market.infra.stockselection;

import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionBacktestBO;
import cn.cccs7.cynexus.market.domain.stockselection.StockSelectionBacktestMarketDataRepository;
import cn.cccs7.cynexus.market.infra.client.ClickHouseStockChartClient;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class StockSelectionBacktestMarketDataRepositoryAdapter implements StockSelectionBacktestMarketDataRepository {

    private final ClickHouseStockChartClient client;

    public StockSelectionBacktestMarketDataRepositoryAdapter(ClickHouseStockChartClient client) { this.client = client; }

    @Override
    public List<LocalDate> listTradingDatesOnOrBefore(LocalDate requestedAsOfDate, int limit) {
        return client.listTradingDatesOnOrBefore(requestedAsOfDate, limit);
    }

    @Override
    public List<StockSelectionBacktestBO.DailyBar> listDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate) {
        return client.listDailyBars(symbols.stream().map(item -> new ClickHouseStockChartClient.MarketSymbol(item.market(), item.code())).toList(), startDate, endDate)
                .stream().map(item -> new StockSelectionBacktestBO.DailyBar(item.market(), item.symbol(), item.tradeDate(), item.close())).toList();
    }

    @Override
    public List<StockSelectionBacktestBO.DailyBar> listLatestDailyBars(List<StockSelectionBacktestBO.MarketSymbol> symbols) {
        return client.listLatestDailyQuotes(symbols.stream().map(item -> new ClickHouseStockChartClient.MarketSymbol(item.market(), item.code())).toList())
                .stream().map(item -> new StockSelectionBacktestBO.DailyBar(item.getMarket(), item.getSymbol(), item.getTradeDate(), item.getClose())).toList();
    }
}
