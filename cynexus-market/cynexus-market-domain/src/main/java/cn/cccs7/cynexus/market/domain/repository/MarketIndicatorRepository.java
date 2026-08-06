package cn.cccs7.cynexus.market.domain.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorEventBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryDetailBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorSnapshotBO;
import java.time.LocalDate;
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
}
