package cn.cccs7.cynexus.market.domain.service.screener;

import cn.cccs7.cynexus.market.domain.entity.screener.StockScreenerQueryBO;

public interface StockScreenerQueryDomainService {

    StockScreenerQueryBO.Result screen(StockScreenerQueryBO.QueryInput input);

    StockScreenerQueryBO.ClassificationOptions listClassificationOptions(String tradeDate);
}
