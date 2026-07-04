package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarginAccountStatsBO;
import cn.cccs7.cynexus.market.domain.entity.MarginIndustrySummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginMarketSummaryBO;
import cn.cccs7.cynexus.market.domain.entity.MarginStockDetailBO;
import cn.cccs7.cynexus.market.domain.repository.MarginTradingRepository;
import cn.cccs7.cynexus.market.infra.basic.service.MarginTradingBasicService;
import cn.cccs7.cynexus.market.infra.convert.MarginAccountStatsPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginIndustrySummaryPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginMarketSummaryPOConverter;
import cn.cccs7.cynexus.market.infra.convert.MarginStockDetailPOConverter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MarginTradingRepositoryImpl implements MarginTradingRepository {

    private final MarginTradingBasicService marginTradingBasicService;

    public MarginTradingRepositoryImpl(MarginTradingBasicService marginTradingBasicService) {
        this.marginTradingBasicService = marginTradingBasicService;
    }

    @Override
    public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
        return marginTradingBasicService.findLatestTradeDate(endDate);
    }

    @Override
    public List<MarginMarketSummaryBO> listMarketSummaryByTradeDate(LocalDate tradeDate) {
        return MarginMarketSummaryPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listMarketSummaryByTradeDate(tradeDate)
        );
    }

    @Override
    public Optional<MarginAccountStatsBO> findAccountStatsByTradeDate(LocalDate tradeDate) {
        return marginTradingBasicService.findAccountStatsByTradeDate(tradeDate)
                .map(MarginAccountStatsPOConverter.INSTANCE::toTarget);
    }

    @Override
    public List<MarginMarketSummaryBO> listMarketSummaryHistory(LocalDate startDate, LocalDate endDate) {
        return MarginMarketSummaryPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listMarketSummaryHistory(startDate, endDate)
        );
    }

    @Override
    public List<MarginStockDetailBO> listStocks(LocalDate tradeDate, String exchange, String industryCode, String sort, String sortDir, int offset, int limit) {
        return MarginStockDetailPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listStocks(tradeDate, exchange, industryCode, sort, sortDir, offset, limit)
        );
    }

    @Override
    public int countStocks(LocalDate tradeDate, String exchange, String industryCode) {
        return marginTradingBasicService.countStocks(tradeDate, exchange, industryCode);
    }

    @Override
    public List<MarginIndustrySummaryBO> listIndustrySummaries(LocalDate tradeDate, String sort, String sortDir, int offset, int limit) {
        return MarginIndustrySummaryPOConverter.INSTANCE.toTargetList(
                marginTradingBasicService.listIndustrySummaries(tradeDate, sort, sortDir, offset, limit)
        );
    }

    @Override
    public int countIndustrySummaries(LocalDate tradeDate) {
        return marginTradingBasicService.countIndustrySummaries(tradeDate);
    }
}
