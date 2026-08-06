package cn.cccs7.cynexus.market.infra.repository;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorEventBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryDetailBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryRelationBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorSnapshotBO;
import cn.cccs7.cynexus.market.domain.repository.MarketIndicatorRepository;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorEventPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorBreadthPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustryRelationPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorIndustrySummaryPO;
import cn.cccs7.cynexus.market.infra.basic.entity.MarketIndicatorSnapshotPO;
import cn.cccs7.cynexus.market.infra.basic.service.MarketIndicatorQueryBasicService;
import cn.cccs7.cynexus.market.infra.cache.RedisMarketIndicatorHotCache;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MarketIndicatorRepositoryImpl implements MarketIndicatorRepository {
    private final MarketIndicatorQueryBasicService service;
    private final RedisMarketIndicatorHotCache hotCache;
    public MarketIndicatorRepositoryImpl(MarketIndicatorQueryBasicService service, RedisMarketIndicatorHotCache hotCache) {
        this.service = service;
        this.hotCache = hotCache;
    }
    @Override public List<MarketIndicatorSnapshotBO> listLatestSnapshots(LocalDate date, String status, int limit) {
        return hotCache.latestSnapshots(date, status, limit).orElseGet(() -> service.listLatestSnapshots(date, status, limit)).stream().map(this::snapshot).toList();
    }
    @Override public List<MarketIndicatorEventBO> listRecentEvents(LocalDate date, String type, int limit) {
        return hotCache.recentEvents(date, type, limit).orElseGet(() -> service.listRecentEvents(date, type, limit)).stream().map(this::event).toList();
    }
    @Override public MarketIndicatorBreadthBO getBreadth(LocalDate date) {
        MarketIndicatorBreadthPO p = hotCache.breadth(date).orElseGet(() -> service.getBreadth(date));
        return p == null ? MarketIndicatorBreadthBO.builder().build() : MarketIndicatorBreadthBO.builder()
                .totalCount(p.getTotalCount()).advancingCount(p.getAdvancingCount()).decliningCount(p.getDecliningCount())
                .flatCount(p.getFlatCount()).limitUpCount(p.getLimitUpCount()).limitUpTouchCount(p.getLimitUpTouchCount())
                .limitDownCount(p.getLimitDownCount()).limitDownTouchCount(p.getLimitDownTouchCount())
                .rapidRiseCount(p.getRapidRiseCount()).rapidFallCount(p.getRapidFallCount()).capturedAt(p.getCapturedAt()).build();
    }
    @Override public List<MarketIndicatorIndustrySnapshotBO> listLatestIndustries(LocalDate date, int limit) {
        return hotCache.latestIndustries(date, limit).orElseGet(() -> service.listLatestIndustries(date, limit)).stream().map(this::industry).toList();
    }
    @Override public MarketIndicatorIndustryDetailBO getIndustryDetail(LocalDate date, String industryCode, int page, int pageSize) {
        MarketIndicatorIndustrySummaryPO summary = service.getIndustrySummary(date, industryCode);
        if (summary == null) {
            return MarketIndicatorIndustryDetailBO.builder().industryCode(industryCode).page(page).pageSize(pageSize)
                    .constituents(List.of()).relations(List.of()).build();
        }
        return MarketIndicatorIndustryDetailBO.builder()
                .industryCode(summary.getIndustryCode()).industryName(summary.getIndustryName()).categoryLabel(summary.getCategoryLabel())
                .totalCount(summary.getTotalCount()).advancingCount(summary.getAdvancingCount()).decliningCount(summary.getDecliningCount())
                .flatCount(summary.getFlatCount()).averageChangePct(summary.getAverageChangePct()).capturedAt(summary.getCapturedAt())
                .page(page).pageSize(pageSize)
                .constituents(service.listIndustryConstituents(date, industryCode, page * pageSize, pageSize).stream().map(this::snapshot).toList())
                .relations(service.listIndustryRelations(date, industryCode, 12).stream().map(this::relation).toList())
                .build();
    }
    @Override public List<Map<String, Object>> listTransmissionEpisodes(LocalDate date, int limit) {
        return hotCache.transmissionEpisodes(date, limit).orElseGet(() -> service.listTransmissionEpisodes(date, limit));
    }
    @Override public List<Map<String, Object>> listTransmissionAdvice(LocalDate date, int limit) {
        return hotCache.transmissionAdvice(date, limit).orElseGet(() -> service.listTransmissionAdvice(date, limit));
    }
    @Override public List<Map<String, Object>> listTransmissionOutcomes(LocalDate date, int limit) {
        return hotCache.transmissionOutcomes(date, limit).orElseGet(() -> service.listTransmissionOutcomes(date, limit));
    }
    private MarketIndicatorIndustryRelationBO relation(MarketIndicatorIndustryRelationPO p) {
        return MarketIndicatorIndustryRelationBO.builder().industryCode(p.getIndustryCode()).industryName(p.getIndustryName())
                .categoryLabel(p.getCategoryLabel()).overlapCount(p.getOverlapCount()).sourceStockCount(p.getSourceStockCount())
                .targetStockCount(p.getTargetStockCount()).advancingCount(p.getAdvancingCount()).decliningCount(p.getDecliningCount())
                .flatCount(p.getFlatCount()).averageChangePct(p.getAverageChangePct()).averageRelationLevel(p.getAverageRelationLevel()).build();
    }
    private MarketIndicatorSnapshotBO snapshot(MarketIndicatorSnapshotPO p) {
        return MarketIndicatorSnapshotBO.builder().id(p.getId()).code(p.getCode()).name(p.getName()).industry(p.getIndustry()).market(p.getMarket())
                .lastPrice(p.getLastPrice()).prevClose(p.getPrevClose()).openPrice(p.getOpenPrice()).highPrice(p.getHighPrice())
                .lowPrice(p.getLowPrice()).changePct(p.getChangePct()).amount(p.getAmount()).volume(p.getVolume())
                .turnoverRate(p.getTurnoverRate()).limitUpPrice(p.getLimitUpPrice()).limitDownPrice(p.getLimitDownPrice())
                .limitStatus(p.getLimitStatus()).source(p.getSource()).capturedAt(p.getCapturedAt()).build();
    }
    private MarketIndicatorEventBO event(MarketIndicatorEventPO p) {
        return MarketIndicatorEventBO.builder().id(p.getId()).tradeDate(p.getTradeDate()).code(p.getCode()).name(p.getName())
                .market(p.getMarket()).eventType(p.getEventType()).eventAt(p.getEventAt()).signalValue(p.getSignalValue())
                .lastPrice(p.getLastPrice()).changePct(p.getChangePct()).limitStatus(p.getLimitStatus()).source(p.getSource()).build();
    }
    private MarketIndicatorIndustrySnapshotBO industry(MarketIndicatorIndustrySnapshotPO p) {
        return MarketIndicatorIndustrySnapshotBO.builder().id(p.getId()).tradeDate(p.getTradeDate()).capturedAt(p.getCapturedAt())
                .industryCode(p.getIndustryCode()).industryName(p.getIndustryName()).stockCount(p.getStockCount())
                .advancingCount(p.getAdvancingCount()).decliningCount(p.getDecliningCount()).flatCount(p.getFlatCount())
                .limitUpCount(p.getLimitUpCount()).limitUpTouchCount(p.getLimitUpTouchCount())
                .limitDownCount(p.getLimitDownCount()).limitDownTouchCount(p.getLimitDownTouchCount())
                .rapidRiseCount(p.getRapidRiseCount()).rapidFallCount(p.getRapidFallCount())
                .firstLimitUpAt(p.getFirstLimitUpAt()).firstLimitUpCode(p.getFirstLimitUpCode()).firstLimitUpName(p.getFirstLimitUpName())
                .firstLimitUpChangePct(p.getFirstLimitUpChangePct()).firstEventAt(p.getFirstEventAt()).firstEventCode(p.getFirstEventCode())
                .firstEventName(p.getFirstEventName()).firstEventType(p.getFirstEventType()).firstEventSignalValue(p.getFirstEventSignalValue())
                .source(p.getSource()).build();
    }
}
