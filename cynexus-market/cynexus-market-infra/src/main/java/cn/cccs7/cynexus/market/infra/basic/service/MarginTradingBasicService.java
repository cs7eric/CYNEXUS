package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.MarginAccountStatsPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginMarketSummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginStockDetailPO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarginTradingBasicService {

    Optional<LocalDate> findLatestTradeDate(LocalDate endDate);

    List<MarginMarketSummaryPO> listMarketSummaryByTradeDate(LocalDate tradeDate);

    Optional<MarginAccountStatsPO> findAccountStatsByTradeDate(LocalDate tradeDate);

    List<MarginMarketSummaryPO> listMarketSummaryHistory(LocalDate startDate, LocalDate endDate);

    List<MarginStockDetailPO> listStocks(LocalDate tradeDate, String exchange, String industryCode, String sort, String sortDir, int offset, int limit);

    int countStocks(LocalDate tradeDate, String exchange, String industryCode);

    List<MarginIndustrySummaryPO> listIndustrySummaries(LocalDate tradeDate, String sort, String sortDir, int offset, int limit);

    int countIndustrySummaries(LocalDate tradeDate);
}
