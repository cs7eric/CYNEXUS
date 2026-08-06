package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorEventPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorBreadthPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustryRelationPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorSnapshotPO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MarketIndicatorQueryBasicService {
    List<MarketIndicatorSnapshotPO> listLatestSnapshots(LocalDate tradeDate, String status, int limit);
    List<MarketIndicatorEventPO> listRecentEvents(LocalDate tradeDate, String eventType, int limit);
    MarketIndicatorBreadthPO getBreadth(LocalDate tradeDate);
    List<MarketIndicatorIndustrySnapshotPO> listLatestIndustries(LocalDate tradeDate, int limit);
    List<MarketIndicatorSnapshotPO> listIndustryConstituents(LocalDate tradeDate, String industryCode, int offset, int limit);
    MarketIndicatorIndustrySummaryPO getIndustrySummary(LocalDate tradeDate, String industryCode);
    List<MarketIndicatorIndustryRelationPO> listIndustryRelations(LocalDate tradeDate, String industryCode, int limit);
    List<Map<String, Object>> listTransmissionEpisodes(LocalDate tradeDate, int limit);
    List<Map<String, Object>> listTransmissionAdvice(LocalDate tradeDate, int limit);
    List<Map<String, Object>> listTransmissionOutcomes(LocalDate tradeDate, int limit);
}
