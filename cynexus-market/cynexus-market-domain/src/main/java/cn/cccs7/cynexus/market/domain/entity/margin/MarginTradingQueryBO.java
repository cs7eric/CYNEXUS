package cn.cccs7.cynexus.market.domain.entity.margin;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustryHistoryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class MarginTradingQueryBO {

    private MarginTradingQueryBO() {
    }

    public record Page(int number, int size, int offset) {
    }

    public record Latest(boolean ok, LocalDate requestedTradeDate, LocalDate tradeDate, boolean fallbackTradeDate,
                         String source, OffsetDateTime updatedAt, List<MarginMarketSummaryBO> summaries,
                         MarginAccountStatsBO account, List<MarginStockDetailBO> topStocks, int stockCount,
                         String error) {
    }

    public record History(LocalDate start, LocalDate end, List<MarginMarketSummaryBO> items) {
    }

    public record Stocks(boolean ok, LocalDate requestedTradeDate, LocalDate tradeDate, String exchange,
                         String industryCode, String sort, String sortDir, Page page, int count,
                         List<MarginStockDetailBO> items, String error) {
    }

    public record StockHistory(String stockCode, List<MarginStockDetailBO> items) {
    }

    public record Industries(boolean ok, LocalDate requestedTradeDate, LocalDate tradeDate,
                             LocalDate constituentSnapshotDate, String sort, String sortDir, Page page,
                             int count, List<MarginIndustrySummaryBO> items, String error) {
    }

    public record IndustryHistory(boolean ok, String industryCode, LocalDate tradeDate,
                                  List<MarginIndustryHistoryBO> items, String error) {
    }
}
