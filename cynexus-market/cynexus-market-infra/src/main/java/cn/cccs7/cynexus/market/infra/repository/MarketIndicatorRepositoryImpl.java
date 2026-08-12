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
import cn.cccs7.cynexus.market.infra.cache.RedisTradeReviewMarketStateCache;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class MarketIndicatorRepositoryImpl implements MarketIndicatorRepository {
    private final MarketIndicatorQueryBasicService service;
    private final RedisMarketIndicatorHotCache hotCache;
    private final RedisTradeReviewMarketStateCache tradeReviewCache;
    private final ObjectMapper objectMapper;
    public MarketIndicatorRepositoryImpl(MarketIndicatorQueryBasicService service, RedisMarketIndicatorHotCache hotCache,
                                         RedisTradeReviewMarketStateCache tradeReviewCache, ObjectMapper objectMapper) {
        this.service = service;
        this.hotCache = hotCache;
        this.tradeReviewCache = tradeReviewCache;
        this.objectMapper = objectMapper;
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
        List<Map<String, Object>> cached = hotCache.transmissionEpisodes(date, limit).orElseGet(List::of);
        if (!cached.isEmpty()) return cached;
        return markFullConfirmation(service.listTransmissionEpisodes(date, limit));
    }
    @Override public List<Map<String, Object>> listTransmissionAdvice(LocalDate date, int limit) {
        List<Map<String, Object>> cached = hotCache.transmissionAdvice(date, limit).orElseGet(List::of);
        return cached.isEmpty() ? service.listTransmissionAdvice(date, limit) : cached;
    }
    @Override public List<Map<String, Object>> listTransmissionOutcomes(LocalDate date, int limit) {
        List<Map<String, Object>> cached = hotCache.transmissionOutcomes(date, limit).orElseGet(List::of);
        return cached.isEmpty() ? service.listTransmissionOutcomes(date, limit) : cached;
    }
    @Override public Map<String, Object> transmissionRuntime(LocalDate date) {
        return hotCache.transmissionRuntime(date).orElseGet(Map::of);
    }
    @Override public List<MarketIndicatorEventBO> listReplayEvents(LocalDate date, OffsetDateTime from, OffsetDateTime to,
                                                                     String type, String industryCode, int limit) {
        return service.listReplayEvents(date, from, to, type, industryCode, limit).stream().map(this::event).toList();
    }
    @Override public List<MarketIndicatorIndustrySnapshotBO> listReplayIndustries(LocalDate date, OffsetDateTime from,
                                                                                    OffsetDateTime to, String industryCode, int limit) {
        return service.listReplayIndustries(date, from, to, industryCode, limit).stream().map(this::industry).toList();
    }
    @Override public Map<String, Object> tradeReviewMarketState(LocalDate date, OffsetDateTime asOf, String membership,
                                                                 String industryCode, int windowDays) {
        // The Python projection publishes a complete immutable day bundle.
        // Never compensate a cache miss by contacting a market provider: the
        // durable materialized tables are the sole fallback for replay.
        var cached = tradeReviewCache.state(date, asOf, membership, industryCode);
        if (cached.isPresent()) return cached.get();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("manifest", normalize(service.getTradeReviewManifest(date), "quality"));
        result.put("market", normalize(asOf == null ? service.getTradeReviewMarketDaily(date) : service.getTradeReviewMarketMinute(date, asOf), "ladder", "source"));
        result.put("industries", normalizeRows(service.listTradeReviewIndustries(date, asOf, membership, industryCode), "ladder", "core_pool", "factor"));
        result.put("pools", normalizeRows(service.listTradeReviewPools(date, asOf, membership, industryCode), "stocks"));
        result.put("events", asOf == null ? List.of() : normalizeRows(service.listTradeReviewEvents(date, asOf), "payload"));
        result.put("history", normalizeRows(service.listTradeReviewDailyStates(date, windowDays), "ladder", "source"));
        result.put("trend", normalize(service.getTradeReviewPhase(date, windowDays), "factor"));
        result.put("industryTrends", normalizeRows(service.listTradeReviewIndustryPhases(date, membership, industryCode, windowDays), "factor"));
        return result;
    }
    @Override public Map<String, Object> tradeReviewMarketStateManifest(LocalDate date) {
        // Entry-page status deliberately reads only the small durable manifest.
        // It must not inflate the full market/industry replay bundle just to
        // decide whether the user can start a replay.
        return service.getTradeReviewManifest(date);
    }
    @Override public List<Map<String, Object>> listLimitBoardSessionSummaries(LocalDate date, String tradeSession, String direction, int limit) {
        return service.listLimitBoardSessionSummaries(date, tradeSession, direction, limit);
    }
    private List<Map<String, Object>> markFullConfirmation(List<Map<String, Object>> rows) {
        rows.forEach(row -> {
            row.put("scope", "full");
            row.put("universeMode", "full_market");
            row.put("fullConfirmationRequired", false);
        });
        return rows;
    }

    /** Normalizes MyBatis JSONB text so the durable fallback equals Redis. */
    private List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows, String... jsonFields) {
        if (rows == null || rows.isEmpty()) return List.of();
        return rows.stream().map(row -> normalize(row, jsonFields)).toList();
    }

    private Map<String, Object> normalize(Map<String, Object> row, String... jsonFields) {
        if (row == null || row.isEmpty()) return Map.of();
        Map<String, Object> normalized = new LinkedHashMap<>(row);
        for (String field : jsonFields) {
            Object raw = normalized.get(field);
            if (!(raw instanceof String text) || text.isBlank()) continue;
            try {
                normalized.put(field, objectMapper.readValue(text, Object.class));
            } catch (Exception ignored) {
                // Keep the source value available to the tolerant UI parser;
                // replay must not fail because optional JSONB is malformed.
            }
        }
        return normalized;
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
