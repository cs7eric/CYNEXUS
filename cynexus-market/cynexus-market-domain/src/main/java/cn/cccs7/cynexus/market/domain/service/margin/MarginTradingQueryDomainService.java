package cn.cccs7.cynexus.market.domain.service.margin;

import cn.cccs7.cynexus.market.domain.entity.margin.MarginTradingQueryBO;

public interface MarginTradingQueryDomainService {

    MarginTradingQueryBO.Latest latest(String tradeDate, int top);

    MarginTradingQueryBO.History summaryHistory(String start, String end);

    MarginTradingQueryBO.Stocks stocks(String tradeDate, String exchange, String industryCode,
                                       String sort, String sortDir, Integer page, Integer pageSize, int limit);

    MarginTradingQueryBO.StockHistory stockHistory(String stockCode, int limit);

    MarginTradingQueryBO.Industries industries(String tradeDate, String sort, String sortDir,
                                               Integer page, Integer pageSize);

    MarginTradingQueryBO.IndustryHistory industryHistory(String industryCode, String tradeDate, int limit);
}
