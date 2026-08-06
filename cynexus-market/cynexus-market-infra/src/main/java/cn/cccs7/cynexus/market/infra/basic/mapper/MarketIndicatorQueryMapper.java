package cn.cccs7.cynexus.market.infra.basic.mapper;

import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorEventPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorBreadthPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustryRelationPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorSnapshotPO;
import java.time.LocalDate;
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
}
