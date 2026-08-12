package cn.cccs7.cynexus.market.infra.basic.mapper;

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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketIndicatorQueryMapper {
    List<MarketIndicatorSnapshotPO> listLatestSnapshots(@Param("tradeDate") LocalDate tradeDate,
                                                         @Param("status") String status,
                                                         @Param("limit") int limit);
    List<MarketIndicatorEventPO> listRecentEvents(@Param("tradeDate") LocalDate tradeDate,
                                                   @Param("eventType") String eventType,
                                                   @Param("limit") int limit);
    MarketIndicatorBreadthPO getBreadth(@Param("tradeDate") LocalDate tradeDate);
    List<MarketIndicatorIndustrySnapshotPO> listLatestIndustries(@Param("tradeDate") LocalDate tradeDate,
                                                                  @Param("limit") int limit);
    List<MarketIndicatorSnapshotPO> listIndustryConstituents(@Param("tradeDate") LocalDate tradeDate,
                                                              @Param("industryCode") String industryCode,
                                                              @Param("offset") int offset,
                                                              @Param("limit") int limit);
    MarketIndicatorIndustrySummaryPO getIndustrySummary(@Param("tradeDate") LocalDate tradeDate,
                                                         @Param("industryCode") String industryCode);
    List<MarketIndicatorIndustryRelationPO> listIndustryRelations(@Param("tradeDate") LocalDate tradeDate,
                                                                  @Param("industryCode") String industryCode,
                                                                  @Param("limit") int limit);
    List<Map<String, Object>> listTransmissionEpisodes(@Param("tradeDate") LocalDate tradeDate, @Param("limit") int limit);
    List<Map<String, Object>> listTransmissionAdvice(@Param("tradeDate") LocalDate tradeDate, @Param("limit") int limit);
    List<Map<String, Object>> listTransmissionOutcomes(@Param("tradeDate") LocalDate tradeDate, @Param("limit") int limit);
    List<MarketIndicatorEventPO> listReplayEvents(@Param("tradeDate") LocalDate tradeDate,
                                                   @Param("from") OffsetDateTime from,
                                                   @Param("to") OffsetDateTime to,
                                                   @Param("eventType") String eventType,
                                                   @Param("industryCode") String industryCode,
                                                   @Param("limit") int limit);
    List<MarketIndicatorIndustrySnapshotPO> listReplayIndustries(@Param("tradeDate") LocalDate tradeDate,
                                                                  @Param("from") OffsetDateTime from,
                                                                  @Param("to") OffsetDateTime to,
                                                                  @Param("industryCode") String industryCode,
                                                                  @Param("limit") int limit);
    Map<String, Object> getTradeReviewManifest(@Param("tradeDate") LocalDate tradeDate);
    List<Map<String, Object>> listLimitBoardSessionSummaries(@Param("tradeDate") LocalDate tradeDate,
                                                              @Param("tradeSession") String tradeSession,
                                                              @Param("direction") String direction,
                                                              @Param("limit") int limit);
    Map<String, Object> getTradeReviewMarketMinute(@Param("tradeDate") LocalDate tradeDate, @Param("asOf") OffsetDateTime asOf);
    Map<String, Object> getTradeReviewMarketDaily(@Param("tradeDate") LocalDate tradeDate);
    List<Map<String, Object>> listTradeReviewIndustries(@Param("tradeDate") LocalDate tradeDate, @Param("asOf") OffsetDateTime asOf,
                                                         @Param("membership") String membership, @Param("industryCode") String industryCode);
    List<Map<String, Object>> listTradeReviewPools(@Param("tradeDate") LocalDate tradeDate, @Param("asOf") OffsetDateTime asOf,
                                                    @Param("membership") String membership, @Param("industryCode") String industryCode);
    List<Map<String, Object>> listTradeReviewEvents(@Param("tradeDate") LocalDate tradeDate, @Param("asOf") OffsetDateTime asOf);
    List<Map<String, Object>> listTradeReviewDailyStates(@Param("tradeDate") LocalDate tradeDate, @Param("windowDays") int windowDays);
    Map<String, Object> getTradeReviewPhase(@Param("tradeDate") LocalDate tradeDate, @Param("windowDays") int windowDays);
    List<Map<String, Object>> listTradeReviewIndustryPhases(@Param("tradeDate") LocalDate tradeDate, @Param("membership") String membership,
                                                             @Param("industryCode") String industryCode, @Param("windowDays") int windowDays);
}
