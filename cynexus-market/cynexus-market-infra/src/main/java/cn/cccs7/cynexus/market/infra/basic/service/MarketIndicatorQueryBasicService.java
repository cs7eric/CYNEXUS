package cn.cccs7.cynexus.market.infra.basic.service;

import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorEventPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorBreadthPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustryRelationPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorSnapshotPO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    List<MarketIndicatorEventPO> listReplayEvents(LocalDate tradeDate, OffsetDateTime from, OffsetDateTime to,
                                                   String eventType, String industryCode, int limit);
    List<MarketIndicatorIndustrySnapshotPO> listReplayIndustries(LocalDate tradeDate, OffsetDateTime from,
                                                                  OffsetDateTime to, String industryCode, int limit);
    Map<String, Object> getTradeReviewManifest(LocalDate tradeDate);
    List<Map<String, Object>> listLimitBoardSessionSummaries(LocalDate tradeDate, String tradeSession,
                                                              String direction, int limit);
    Map<String, Object> getTradeReviewMarketMinute(LocalDate tradeDate, OffsetDateTime asOf);
    Map<String, Object> getTradeReviewMarketDaily(LocalDate tradeDate);
    List<Map<String, Object>> listTradeReviewIndustries(LocalDate tradeDate, OffsetDateTime asOf, String membership, String industryCode);
    List<Map<String, Object>> listTradeReviewPools(LocalDate tradeDate, OffsetDateTime asOf, String membership, String industryCode);
    List<Map<String, Object>> listTradeReviewEvents(LocalDate tradeDate, OffsetDateTime asOf);
    List<Map<String, Object>> listTradeReviewDailyStates(LocalDate tradeDate, int windowDays);
    Map<String, Object> getTradeReviewPhase(LocalDate tradeDate, int windowDays);
    List<Map<String, Object>> listTradeReviewIndustryPhases(LocalDate tradeDate, String membership, String industryCode, int windowDays);
}
