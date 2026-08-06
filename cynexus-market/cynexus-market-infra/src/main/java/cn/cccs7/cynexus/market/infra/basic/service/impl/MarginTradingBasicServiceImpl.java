package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.market.infra.basic.entity.MarginAccountStatsPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustryHistoryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginIndustryConstituentPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginMarketSummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarginStockDetailPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarginTradingMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarginTradingBasicService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarginTradingBasicServiceImpl implements MarginTradingBasicService {

    private final MarginTradingMapper marginTradingMapper;

    public MarginTradingBasicServiceImpl(MarginTradingMapper marginTradingMapper) {
        this.marginTradingMapper = marginTradingMapper;
    }

    @Override
    public Optional<LocalDate> findLatestTradeDate(LocalDate endDate) {
        return Optional.ofNullable(marginTradingMapper.findLatestTradeDate(endDate));
    }

    @Override
    public List<MarginMarketSummaryPO> listMarketSummaryByTradeDate(LocalDate tradeDate) {
        return marginTradingMapper.listMarketSummaryByTradeDate(tradeDate);
    }

    @Override
    public Optional<MarginAccountStatsPO> findAccountStatsByTradeDate(LocalDate tradeDate) {
        return Optional.ofNullable(marginTradingMapper.findAccountStatsByTradeDate(tradeDate));
    }

    @Override
    public List<MarginMarketSummaryPO> listMarketSummaryHistory(LocalDate startDate, LocalDate endDate) {
        return marginTradingMapper.listMarketSummaryHistory(startDate, endDate);
    }

    @Override
    public List<MarginStockDetailPO> listStocks(LocalDate tradeDate, String exchange, String industryCode, String sort, String sortDir, int offset, int limit) {
        return marginTradingMapper.listStocks(tradeDate, exchange, industryCode, sort, sortDir, offset, limit);
    }

    @Override
    public int countStocks(LocalDate tradeDate, String exchange, String industryCode) {
        return marginTradingMapper.countStocks(tradeDate, exchange, industryCode);
    }

    @Override
    public List<MarginStockDetailPO> listStockHistory(String stockCode, int limit) {
        return marginTradingMapper.listStockHistory(stockCode, limit);
    }

    @Override
    public List<MarginIndustrySummaryPO> listIndustrySummaries(LocalDate tradeDate, String sort, String sortDir, int offset, int limit) {
        return marginTradingMapper.listIndustrySummaries(tradeDate, sort, sortDir, offset, limit);
    }

    @Override
    public int countIndustrySummaries(LocalDate tradeDate) {
        return marginTradingMapper.countIndustrySummaries(tradeDate);
    }

    @Override
    public List<MarginIndustryConstituentPO> listIndustryConstituents(LocalDate tradeDate, List<String> industryCodes) {
        return marginTradingMapper.listIndustryConstituents(tradeDate, industryCodes);
    }

    @Override
    public List<MarginIndustryHistoryPO> listIndustryHistory(LocalDate tradeDate, String industryCode, int limit) {
        return marginTradingMapper.listIndustryHistory(tradeDate, industryCode, limit);
    }
}
