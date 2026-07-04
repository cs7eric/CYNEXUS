package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarginTradingRepository {

    Optional<LocalDate> findLatestTradeDate(LocalDate endDate);

    List<MarginMarketSummaryBO> listMarketSummaryByTradeDate(LocalDate tradeDate);

    Optional<MarginAccountStatsBO> findAccountStatsByTradeDate(LocalDate tradeDate);

    List<MarginMarketSummaryBO> listMarketSummaryHistory(LocalDate startDate, LocalDate endDate);

    List<MarginStockDetailBO> listStocks(LocalDate tradeDate, String exchange, String industryCode, String sort, String sortDir, int offset, int limit);

    int countStocks(LocalDate tradeDate, String exchange, String industryCode);

    List<MarginIndustrySummaryBO> listIndustrySummaries(LocalDate tradeDate, String sort, String sortDir, int offset, int limit);

    int countIndustrySummaries(LocalDate tradeDate);
}
