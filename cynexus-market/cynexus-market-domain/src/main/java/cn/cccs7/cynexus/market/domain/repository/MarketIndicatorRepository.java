package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorEventBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryDetailBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorSnapshotBO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface MarketIndicatorRepository {
    List<MarketIndicatorSnapshotBO> listLatestSnapshots(LocalDate tradeDate, String status, int limit);

    List<MarketIndicatorEventBO> listRecentEvents(LocalDate tradeDate, String eventType, int limit);
    MarketIndicatorBreadthBO getBreadth(LocalDate tradeDate);
    List<MarketIndicatorIndustrySnapshotBO> listLatestIndustries(LocalDate tradeDate, int limit);

    MarketIndicatorIndustryDetailBO getIndustryDetail(LocalDate tradeDate, String industryCode, int page, int pageSize);

    List<Map<String, Object>> listTransmissionEpisodes(LocalDate tradeDate, int limit);
    List<Map<String, Object>> listTransmissionAdvice(LocalDate tradeDate, int limit);
    List<Map<String, Object>> listTransmissionOutcomes(LocalDate tradeDate, int limit);
    Map<String, Object> transmissionRuntime(LocalDate tradeDate);

    List<MarketIndicatorEventBO> listReplayEvents(LocalDate tradeDate, OffsetDateTime from, OffsetDateTime to,
                                                   String eventType, String industryCode, int limit);

    List<MarketIndicatorIndustrySnapshotBO> listReplayIndustries(LocalDate tradeDate, OffsetDateTime from,
                                                                  OffsetDateTime to, String industryCode, int limit);

    Map<String, Object> tradeReviewMarketState(LocalDate tradeDate, OffsetDateTime asOf, String membership,
                                               String industryCode, int windowDays);

    /** Lightweight day-manifest read for the Trade Review entry page. */
    Map<String, Object> tradeReviewMarketStateManifest(LocalDate tradeDate);
    List<Map<String, Object>> listLimitBoardSessionSummaries(LocalDate tradeDate, String tradeSession,
                                                              String direction, int limit);
}
