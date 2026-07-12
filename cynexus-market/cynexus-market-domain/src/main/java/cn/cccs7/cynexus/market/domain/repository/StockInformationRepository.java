package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.StockInformationBO;

public interface StockInformationRepository {

    StockInformationBO.Feed feed(String symbol, int limit);

    StockInformationBO.SourceRecord findSourceRecord(String symbol, String kind, String source, String itemId);

    StockInformationBO.Content findContent(StockInformationBO.SourceRecord sourceRecord);

    StockInformationBO.Content saveContent(StockInformationBO.Content content);

    StockInformationBO.Analysis findLatestAnalysis(Long contentId, String contentHash, String schemaVersion, String promptVersion);

    StockInformationBO.Analysis saveAnalysis(StockInformationBO.Analysis analysis);
}
