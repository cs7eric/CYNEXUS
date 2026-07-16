package cn.cccs7.cynexus.market.domain.information;

import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;

public interface StockInformationContentExtractor {

    StockInformationBO.Content extract(StockInformationBO.SourceRecord record);
}
