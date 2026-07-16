package cn.cccs7.cynexus.market.domain.information;

import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;

public interface StockInformationContentDomainService {

    StockInformationBO.Content content(String symbol, String kind, String source, String itemId, boolean fetchIfMissing);

    StockInformationBO.Content fetchContent(String symbol, String kind, String source, String itemId);

    StockInformationBO.Analysis analysis(String symbol, String kind, String source, String itemId);

    StockInformationBO.Analysis runAnalysis(String symbol, String kind, String source, String itemId);
}
