package cn.cccs7.cynexus.market.infra.basic.service.impl;

import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorEventPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorBreadthPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustryRelationPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.mapper.MarketIndicatorQueryMapper;
import cn.cccs7.cynexus.market.infra.basic.service.MarketIndicatorQueryBasicService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MarketIndicatorQueryBasicServiceImpl implements MarketIndicatorQueryBasicService {
    private final MarketIndicatorQueryMapper mapper;

    public MarketIndicatorQueryBasicServiceImpl(MarketIndicatorQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override public List<MarketIndicatorSnapshotPO> listLatestSnapshots(LocalDate date, String status, int limit) {
        return mapper.listLatestSnapshots(date, status, limit);
    }
    @Override public List<MarketIndicatorEventPO> listRecentEvents(LocalDate date, String type, int limit) {
        return mapper.listRecentEvents(date, type, limit);
    }
    @Override public MarketIndicatorBreadthPO getBreadth(LocalDate date) { return mapper.getBreadth(date); }
    @Override public List<MarketIndicatorIndustrySnapshotPO> listLatestIndustries(LocalDate date, int limit) {
        return mapper.listLatestIndustries(date, limit);
    }
    @Override public List<MarketIndicatorSnapshotPO> listIndustryConstituents(LocalDate date, String industryCode, int offset, int limit) {
        return mapper.listIndustryConstituents(date, industryCode, offset, limit);
    }
    @Override public MarketIndicatorIndustrySummaryPO getIndustrySummary(LocalDate date, String industryCode) {
        return mapper.getIndustrySummary(date, industryCode);
    }
    @Override public List<MarketIndicatorIndustryRelationPO> listIndustryRelations(LocalDate date, String industryCode, int limit) {
        return mapper.listIndustryRelations(date, industryCode, limit);
    }
    @Override public List<Map<String, Object>> listTransmissionEpisodes(LocalDate date, int limit) {
        return mapper.listTransmissionEpisodes(date, limit);
    }
    @Override public List<Map<String, Object>> listTransmissionAdvice(LocalDate date, int limit) {
        return mapper.listTransmissionAdvice(date, limit);
    }
    @Override public List<Map<String, Object>> listTransmissionOutcomes(LocalDate date, int limit) {
        return mapper.listTransmissionOutcomes(date, limit);
    }
    @Override public List<MarketIndicatorEventPO> listReplayEvents(LocalDate date, OffsetDateTime from, OffsetDateTime to,
                                                                     String eventType, String industryCode, int limit) {
        return mapper.listReplayEvents(date, from, to, eventType, industryCode, limit);
    }
    @Override public List<MarketIndicatorIndustrySnapshotPO> listReplayIndustries(LocalDate date, OffsetDateTime from,
                                                                                    OffsetDateTime to, String industryCode, int limit) {
        return mapper.listReplayIndustries(date, from, to, industryCode, limit);
    }
    @Override public Map<String, Object> getTradeReviewManifest(LocalDate date) { return mapper.getTradeReviewManifest(date); }
    @Override public List<Map<String, Object>> listLimitBoardSessionSummaries(LocalDate date, String tradeSession, String direction, int limit) {
        return mapper.listLimitBoardSessionSummaries(date, tradeSession, direction, limit);
    }
    @Override public Map<String, Object> getTradeReviewMarketMinute(LocalDate date, OffsetDateTime asOf) { return mapper.getTradeReviewMarketMinute(date, asOf); }
    @Override public Map<String, Object> getTradeReviewMarketDaily(LocalDate date) { return mapper.getTradeReviewMarketDaily(date); }
    @Override public List<Map<String, Object>> listTradeReviewIndustries(LocalDate date, OffsetDateTime asOf, String membership, String industryCode) { return mapper.listTradeReviewIndustries(date, asOf, membership, industryCode); }
    @Override public List<Map<String, Object>> listTradeReviewPools(LocalDate date, OffsetDateTime asOf, String membership, String industryCode) { return mapper.listTradeReviewPools(date, asOf, membership, industryCode); }
    @Override public List<Map<String, Object>> listTradeReviewEvents(LocalDate date, OffsetDateTime asOf) { return mapper.listTradeReviewEvents(date, asOf); }
    @Override public List<Map<String, Object>> listTradeReviewDailyStates(LocalDate date, int windowDays) { return mapper.listTradeReviewDailyStates(date, windowDays); }
    @Override public Map<String, Object> getTradeReviewPhase(LocalDate date, int windowDays) { return mapper.getTradeReviewPhase(date, windowDays); }
    @Override public List<Map<String, Object>> listTradeReviewIndustryPhases(LocalDate date, String membership, String industryCode, int windowDays) { return mapper.listTradeReviewIndustryPhases(date, membership, industryCode, windowDays); }
}
