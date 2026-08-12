package cn.cccs7.cynexus.market.domain.service.stock;

import cn.cccs7.cynexus.market.domain.entity.StockChartBO;
import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.stock.StockAuctionDomainPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Domain entry point for StockChart query use cases.
 *
 * <p>The application layer should only adapt these domain results to API DTOs.
 * Normalization, source selection, date fallback, empty-result semantics and
 * auction interpretation belong to this service.</p>
 */
public interface StockChartQueryDomainService {

    List<StockChartBO.SearchItem> search(String keyword);

    KlineResult klines(String targetType, String symbol, String period, String adjust);

    StockChartBO.StockMeta stockMeta(String symbol);

    StockChartBO.StockSectors stockSectors(String symbol, String tradeDate);

    StockChartBO.FundFlowSnapshot fundFlow(String symbol);

    StockDailyFeatureBO dailyFeature(String symbol, String tradeDate);

    StockIntradayFeatureBO intradayFeature(String symbol, String tradeDate);

    StockChartBO.SectorStocks sectorStocks(String sectorId, Integer categoryRaw,
                                           String topicId, String name, int limit);

    IntradayResult intraday(String targetType, String symbol, String name, String adjust,
                            String tradeDate, List<String> periods);

    default IntradayResult intraday(String targetType, String symbol, String name, String adjust,
                                    String tradeDate, List<String> periods, boolean strictDate) {
        return intraday(targetType, symbol, name, adjust, tradeDate, periods);
    }

    AuctionResult auction(String symbol, String tradeDate);

    List<AuctionResult> auctionHistory(String symbol, int limit);

    StockChartBO.F10Valuation valuation(String symbol, String reqId);

    StockChartBO.F10FinanceReport financeReport(String symbol, String reportType);

    StockChartBO.F10BusinessComposition businessComposition(String symbol, String reportDate);

    StockChartBO.F10ProfitForecast profitForecast(String symbol);

    StockChartBO.F10Announcements announcements(String symbol);

    StockChartBO.F10News news(String symbol);

    StockChartBO.F10Roadshows roadshows(String symbol);

    StockChartBO.F10CompanyNews companyNews(String symbol, String section);

    record KlineResult(String symbol, String targetType, String period, String adjust,
                       String source, List<StockChartBO.Bar> items) {
    }

    record IntradayResult(String symbol, String targetType, String name, String adjust,
                          String effectiveAdjust, String requestedAdjust, String source,
                          LocalDate tradeDate, LocalDate requestedTradeDate,
                          BigDecimal preClose,
                          List<StockChartBO.IntradayPoint> timeshare,
                          Map<String, List<StockChartBO.Bar>> minuteBars,
                          String error) {
    }

    record AuctionResult(String symbol, LocalDate tradeDate, String source, String error,
                         StockAuctionDomainPolicy.AuctionPhaseFacts opening,
                         StockAuctionDomainPolicy.AuctionPhaseFacts closing,
                         Map<String, Object> auction0925,
                         List<StockAuctionDomainPolicy.AuctionPointFacts> openingPoints,
                         List<StockAuctionDomainPolicy.AuctionPointFacts> closingPoints,
                         List<StockAuctionDomainPolicy.AuctionPointFacts> allPoints) {
    }
}
