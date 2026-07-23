package cn.cccs7.cynexus.market.domain.service.stock;

import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;

public interface StockInformationQueryDomainService {

    StockInformationBO.Feed feed(String symbol, int limit);
}
