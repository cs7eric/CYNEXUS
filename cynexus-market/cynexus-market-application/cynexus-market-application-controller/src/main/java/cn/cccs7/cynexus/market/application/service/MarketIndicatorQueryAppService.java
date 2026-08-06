package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.api.dto.MarketIndicatorDTO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorEventBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorSnapshotBO;
import cn.cccs7.cynexus.market.domain.service.indicator.MarketIndicatorQueryDomainService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class MarketIndicatorQueryAppService {
    private final MarketIndicatorQueryDomainService domainService;

    public MarketIndicatorQueryAppService(MarketIndicatorQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public MarketIndicatorDTO.Overview latest(String tradeDate, String status, String eventType, int limit) {
        LocalDate date = tradeDate == null || tradeDate.isBlank() ? null : LocalDate.parse(tradeDate);
        List<MarketIndicatorSnapshotBO> snapshots = domainService.latestSnapshots(date, status, limit);
        List<MarketIndicatorSnapshotBO> limitUp = domainService.latestSnapshots(date, "limit_up_sealed", limit);
        List<MarketIndicatorSnapshotBO> limitDown = domainService.latestSnapshots(date, "limit_down_sealed", limit);
        List<MarketIndicatorEventBO> events = domainService.recentEvents(date, eventType, limit);
        MarketIndicatorBreadthBO breadth = domainService.breadth(date);
        List<MarketIndicatorIndustrySnapshotBO> industries = domainService.latestIndustries(date, limit);
        LocalDate resolved = snapshots.stream().findFirst().map(item -> item.getCapturedAt().toLocalDate())
                .orElseGet(() -> breadth.getCapturedAt() == null ? date : breadth.getCapturedAt().toLocalDate());
        boolean hasBreadth = breadth.getTotalCount() != null && breadth.getTotalCount() > 0;
        boolean hasData = hasBreadth || !snapshots.isEmpty() || !events.isEmpty() || !industries.isEmpty();
        return MarketIndicatorDTO.Overview.builder().ok(hasData)
                .error(hasData ? null : "market indicator data is empty")
                .tradeDate(resolved).capturedAt(breadth.getCapturedAt() != null ? breadth.getCapturedAt() : snapshots.stream().findFirst().map(MarketIndicatorSnapshotBO::getCapturedAt).orElse(null))
                .source(snapshots.stream().findFirst().map(MarketIndicatorSnapshotBO::getSource).orElse(null))
                .snapshotCount(breadth.getTotalCount() == null ? 0 : breadth.getTotalCount()).breadth(breadth(breadth))
                .snapshots(snapshots.stream().map(this::snapshot).toList())
                .limitUp(limitUp.stream().map(this::snapshot).toList())
                .limitDown(limitDown.stream().map(this::snapshot).toList())
                .industries(industries.stream().map(this::industry).toList())
                .events(events.stream().map(this::event).toList()).build();
    }

    public MarketIndicatorDTO.IndustryDetail industryDetail(String tradeDate, String industryCode, int page, int pageSize) {
        LocalDate date = tradeDate == null || tradeDate.isBlank() ? null : LocalDate.parse(tradeDate);
        var detail = domainService.industryDetail(date, industryCode, page, pageSize);
        return MarketIndicatorDTO.IndustryDetail.builder().industryCode(detail.getIndustryCode()).industryName(detail.getIndustryName())
                .categoryLabel(detail.getCategoryLabel()).totalCount(detail.getTotalCount()).page(detail.getPage()).pageSize(detail.getPageSize())
                .advancingCount(detail.getAdvancingCount()).decliningCount(detail.getDecliningCount()).flatCount(detail.getFlatCount())
                .averageChangePct(detail.getAverageChangePct()).capturedAt(detail.getCapturedAt())
                .constituents(detail.getConstituents() == null ? List.of() : detail.getConstituents().stream().map(this::snapshot).toList())
                .relations(detail.getRelations() == null ? List.of() : detail.getRelations().stream().map(this::relation).toList())
                .build();
    }

    public Map<String, Object> transmission(String tradeDate, int limit) {
        LocalDate date = tradeDate == null || tradeDate.isBlank() ? null : LocalDate.parse(tradeDate);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tradeDate", date);
        result.put("episodes", domainService.transmissionEpisodes(date, limit));
        result.put("advice", domainService.transmissionAdvice(date, limit));
        result.put("outcomes", domainService.transmissionOutcomes(date, limit));
        result.put("disclaimer", "仅供辅助参考，不构成无条件买卖指令，不执行自动交易");
        return result;
    }

    private MarketIndicatorDTO.Snapshot snapshot(MarketIndicatorSnapshotBO b) {
        return MarketIndicatorDTO.Snapshot.builder().code(b.getCode()).name(b.getName()).industry(b.getIndustry()).market(b.getMarket())
                .lastPrice(b.getLastPrice()).prevClose(b.getPrevClose()).openPrice(b.getOpenPrice()).highPrice(b.getHighPrice())
                .lowPrice(b.getLowPrice()).changePct(b.getChangePct()).amount(b.getAmount()).volume(b.getVolume())
                .turnoverRate(b.getTurnoverRate()).limitUpPrice(b.getLimitUpPrice()).limitDownPrice(b.getLimitDownPrice())
                .limitStatus(b.getLimitStatus()).source(b.getSource()).capturedAt(b.getCapturedAt()).build();
    }

    private MarketIndicatorDTO.Event event(MarketIndicatorEventBO b) {
        return MarketIndicatorDTO.Event.builder().id(b.getId()).tradeDate(b.getTradeDate()).code(b.getCode()).name(b.getName())
                .market(b.getMarket()).eventType(b.getEventType()).eventAt(b.getEventAt()).signalValue(b.getSignalValue())
                .lastPrice(b.getLastPrice()).changePct(b.getChangePct()).limitStatus(b.getLimitStatus()).source(b.getSource()).build();
    }

    private MarketIndicatorDTO.Breadth breadth(MarketIndicatorBreadthBO b) {
        return MarketIndicatorDTO.Breadth.builder().totalCount(b.getTotalCount()).advancingCount(b.getAdvancingCount())
                .decliningCount(b.getDecliningCount()).flatCount(b.getFlatCount()).limitUpCount(b.getLimitUpCount())
                .limitUpTouchCount(b.getLimitUpTouchCount()).limitDownCount(b.getLimitDownCount())
                .limitDownTouchCount(b.getLimitDownTouchCount()).rapidRiseCount(b.getRapidRiseCount())
                .rapidFallCount(b.getRapidFallCount()).capturedAt(b.getCapturedAt()).build();
    }

    private MarketIndicatorDTO.Industry industry(MarketIndicatorIndustrySnapshotBO b) {
        return MarketIndicatorDTO.Industry.builder().id(b.getId()).tradeDate(b.getTradeDate()).capturedAt(b.getCapturedAt())
                .industryCode(b.getIndustryCode()).industryName(b.getIndustryName()).stockCount(b.getStockCount())
                .advancingCount(b.getAdvancingCount()).decliningCount(b.getDecliningCount()).flatCount(b.getFlatCount())
                .limitUpCount(b.getLimitUpCount()).limitUpTouchCount(b.getLimitUpTouchCount())
                .limitDownCount(b.getLimitDownCount()).limitDownTouchCount(b.getLimitDownTouchCount())
                .rapidRiseCount(b.getRapidRiseCount()).rapidFallCount(b.getRapidFallCount())
                .firstLimitUpAt(b.getFirstLimitUpAt()).firstLimitUpCode(b.getFirstLimitUpCode()).firstLimitUpName(b.getFirstLimitUpName())
                .firstLimitUpChangePct(b.getFirstLimitUpChangePct()).firstEventAt(b.getFirstEventAt()).firstEventCode(b.getFirstEventCode())
                .firstEventName(b.getFirstEventName()).firstEventType(b.getFirstEventType()).firstEventSignalValue(b.getFirstEventSignalValue())
                .source(b.getSource()).build();
    }

    private MarketIndicatorDTO.IndustryRelation relation(cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryRelationBO b) {
        return MarketIndicatorDTO.IndustryRelation.builder().industryCode(b.getIndustryCode()).industryName(b.getIndustryName())
                .categoryLabel(b.getCategoryLabel()).overlapCount(b.getOverlapCount()).sourceStockCount(b.getSourceStockCount())
                .targetStockCount(b.getTargetStockCount()).advancingCount(b.getAdvancingCount()).decliningCount(b.getDecliningCount())
                .flatCount(b.getFlatCount()).averageChangePct(b.getAverageChangePct()).averageRelationLevel(b.getAverageRelationLevel())
                .relationScore(b.getRelationScore()).relationState(b.getRelationState()).build();
    }
}
