package cn.cccs7.cynexus.market.domain.service.indicator;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorEventBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryDetailBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryRelationBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorReplayBO;
import cn.cccs7.cynexus.market.domain.entity.IndexMinuteBarBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import cn.cccs7.cynexus.market.domain.repository.MarketIndicatorRepository;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public final class MarketIndicatorQueryDomainService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 2500;
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private final MarketIndicatorRepository repository;
    private final MarketPulseQueryRepository pulseRepository;

    public MarketIndicatorQueryDomainService(MarketIndicatorRepository repository) {
        this(repository, null);
    }

    public MarketIndicatorQueryDomainService(MarketIndicatorRepository repository, MarketPulseQueryRepository pulseRepository) {
        this.repository = repository;
        this.pulseRepository = pulseRepository;
    }

    public List<MarketIndicatorSnapshotBO> latestSnapshots(LocalDate requestedDate, String status, int limit) {
        return repository.listLatestSnapshots(requestedDate, normalizeStatus(status), normalizeLimit(limit));
    }

    public List<MarketIndicatorEventBO> recentEvents(LocalDate requestedDate, String eventType, int limit) {
        return repository.listRecentEvents(requestedDate, normalizeEventType(eventType), normalizeLimit(limit));
    }
    public MarketIndicatorBreadthBO breadth(LocalDate requestedDate) {
        return repository.getBreadth(requestedDate);
    }

    public List<Map<String, Object>> limitBoardSessionSummaries(LocalDate requestedDate, String tradeSession,
                                                                 String direction, int limit) {
        String normalizedSession = tradeSession == null || tradeSession.isBlank() ? "all" : tradeSession.trim().toLowerCase();
        if (!normalizedSession.equals("all") && !normalizedSession.equals("morning") && !normalizedSession.equals("afternoon")) normalizedSession = "all";
        String normalizedDirection = direction == null || direction.isBlank() ? "all" : direction.trim().toLowerCase();
        if (!normalizedDirection.equals("all") && !normalizedDirection.equals("up") && !normalizedDirection.equals("down")) normalizedDirection = "all";
        return repository.listLimitBoardSessionSummaries(requestedDate, normalizedSession, normalizedDirection,
                Math.min(240, Math.max(1, limit <= 0 ? 120 : limit)));
    }
    public List<MarketIndicatorIndustrySnapshotBO> latestIndustries(LocalDate requestedDate, int limit) {
        return repository.listLatestIndustries(requestedDate, Math.min(300, normalizeLimit(limit)));
    }

    public MarketIndicatorIndustryDetailBO industryDetail(LocalDate requestedDate, String industryCode, int page, int pageSize) {
        if (industryCode == null || industryCode.isBlank()) {
            return MarketIndicatorIndustryDetailBO.builder().page(0).pageSize(50).constituents(List.of()).relations(List.of()).build();
        }
        int normalizedPage = Math.max(0, page);
        int normalizedPageSize = Math.max(10, Math.min(100, pageSize <= 0 ? 50 : pageSize));
        MarketIndicatorIndustryDetailBO detail = repository.getIndustryDetail(
                requestedDate, industryCode.trim(), normalizedPage, normalizedPageSize);
        if (detail == null || detail.getRelations() == null) {
            return detail;
        }
        BigDecimal sourceChange = detail.getAverageChangePct();
        detail.getRelations().forEach(relation -> enrichRelation(relation, sourceChange));
        return detail;
    }

    public List<Map<String, Object>> transmissionEpisodes(LocalDate requestedDate, int limit) {
        return repository.listTransmissionEpisodes(requestedDate, normalizeLimit(limit));
    }

    public List<Map<String, Object>> transmissionAdvice(LocalDate requestedDate, int limit) {
        return repository.listTransmissionAdvice(requestedDate, normalizeLimit(limit));
    }

    public List<Map<String, Object>> transmissionOutcomes(LocalDate requestedDate, int limit) {
        return repository.listTransmissionOutcomes(requestedDate, normalizeLimit(limit));
    }

    public Map<String, Object> transmissionRuntime(LocalDate requestedDate) {
        return repository.transmissionRuntime(requestedDate);
    }

    public Map<String, Object> radarState(LocalDate requestedDate, OffsetDateTime asOf) {
        LocalDate tradeDate = requestedDate == null ? LocalDate.now(MARKET_ZONE) : requestedDate;
        MarketIndicatorBreadthBO breadth = repository.getBreadth(tradeDate);
        List<MarketIndicatorEventBO> events = repository.listRecentEvents(tradeDate, null, 50).stream()
                .filter(event -> asOf == null || event.getEventAt() == null || !event.getEventAt().isAfter(asOf))
                .toList();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("tradeDate", tradeDate);
        state.put("asOf", asOf);
        state.put("breadth", breadth);
        state.put("eventCount", events.size());
        state.put("rapidRiseCount", value(breadth == null ? null : breadth.getRapidRiseCount()));
        state.put("rapidFallCount", value(breadth == null ? null : breadth.getRapidFallCount()));
        state.put("marketState", classifyMarketState(breadth));
        state.put("stateReason", stateReason(breadth));
        state.put("source", "postgres.market_indicator");
        state.put("coverage", breadth == null ? null : breadth.getTotalCount());
        state.put("capturedAt", breadth == null ? null : breadth.getCapturedAt());
        return state;
    }

    public List<MarketIndicatorEventBO> radarEvents(LocalDate requestedDate, OffsetDateTime asOf, String eventType, int limit) {
        return repository.listRecentEvents(requestedDate, normalizeEventType(eventType), Math.min(100, normalizeLimit(limit))).stream()
                .filter(event -> asOf == null || event.getEventAt() == null || !event.getEventAt().isAfter(asOf))
                .sorted(Comparator.comparing(MarketIndicatorEventBO::getEventAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<MarketIndicatorSnapshotBO> radarLimitLadder(LocalDate requestedDate, String board, int limit) {
        String status = switch (board == null ? "all" : board.trim().toLowerCase()) {
            case "limit_up", "first_board", "sealed" -> "limit_up_sealed";
            case "limit_down" -> "limit_down_sealed";
            case "touch" -> "limit_up_touch";
            default -> null;
        };
        return repository.listLatestSnapshots(requestedDate, status, Math.min(100, normalizeLimit(limit))).stream()
                .sorted(Comparator.comparing(MarketIndicatorSnapshotBO::getChangePct, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Map<String, Object> tradeReviewMarketState(LocalDate requestedDate, OffsetDateTime asOf, String membership,
                                                       String industryCode, int windowDays) {
        LocalDate tradeDate = requestedDate == null ? LocalDate.now(MARKET_ZONE) : requestedDate;
        String normalizedMembership = membership == null ? "union" : membership.trim().toLowerCase();
        if (!List.of("ths", "tdx", "union").contains(normalizedMembership)) normalizedMembership = "union";
        int window = Math.max(5, Math.min(240, windowDays <= 0 ? 60 : windowDays));
        Map<String, Object> result = new LinkedHashMap<>(repository.tradeReviewMarketState(tradeDate, asOf,
                normalizedMembership, industryCode == null || industryCode.isBlank() ? null : industryCode.trim(), window));
        Map<String, Object> manifest = result.get("manifest") instanceof Map<?, ?> map ? castMap(map) : Map.of();
        String status = text(manifest.get("status"), "UNAVAILABLE").toLowerCase();
        result.put("tradeDate", tradeDate);
        result.put("asOf", asOf);
        result.put("membership", normalizedMembership);
        result.put("windowDays", window);
        result.put("dataStatus", "ready".equals(status) ? "ready" : "partial".equals(status) ? "partial" : "unavailable");
        List<String> missingReasons = new ArrayList<>();
        if (manifest.isEmpty()) missingReasons.add("market_state_not_materialized");
        else if ("unavailable".equals(status)) missingReasons.add("minute_1m_unavailable");
        else if ("partial".equals(status)) missingReasons.add("minute_1m_coverage_partial");
        if (result.get("market") == null) missingReasons.add(asOf == null ? "daily_state_unavailable" : "cursor_minute_unavailable");
        // ``qualityReasons`` is the stable daily replay contract. Keep the
        // legacy name during the UI rollout so existing Panel builds do not
        // lose their degradation message.
        result.put("status", "ready".equals(status) ? "ready" : "partial".equals(status) ? "partial" : "unavailable");
        result.put("qualityReasons", List.copyOf(missingReasons));
        result.put("missingReasons", missingReasons);
        result.put("source", "trade_review_materialized");
        return result;
    }

    /**
     * Returns the compact materialization manifest used before a replay panel
     * is opened. This intentionally avoids loading pools, industry rows and
     * historical trend data.
     */
    public Map<String, Object> tradeReviewMarketStateStatus(LocalDate requestedDate) {
        LocalDate tradeDate = requestedDate == null ? LocalDate.now(MARKET_ZONE) : requestedDate;
        Map<String, Object> manifest = repository.tradeReviewMarketStateManifest(tradeDate);
        String status = text(manifest == null ? null : manifest.get("status"), "UNAVAILABLE").toLowerCase();
        List<String> missingReasons = new ArrayList<>();
        if (manifest == null || manifest.isEmpty()) missingReasons.add("market_state_not_materialized");
        else if ("unavailable".equals(status)) missingReasons.add("minute_1m_unavailable");
        else if ("partial".equals(status)) missingReasons.add("minute_1m_coverage_partial");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tradeDate", tradeDate);
        result.put("manifest", manifest == null ? Map.of() : manifest);
        result.put("dataStatus", "ready".equals(status) ? "ready" : "partial".equals(status) ? "partial" : "unavailable");
        result.put("status", result.get("dataStatus"));
        result.put("qualityReasons", List.copyOf(missingReasons));
        result.put("missingReasons", missingReasons);
        result.put("source", "trade_review_materialized_manifest");
        return result;
    }

    public Map<String, Object> replaySummary(LocalDate requestedDate, int limit) {
        MarketIndicatorReplayBO replay = replayTimeline(requestedDate, LocalTime.of(9, 30), LocalTime.of(15, 0), null, null, Math.min(1000, limit));
        LocalDate tradeDate = replay.getTradeDate();
        List<Map<String, Object>> episodes = repository.listTransmissionEpisodes(tradeDate, Math.min(100, normalizeLimit(limit)));
        List<Map<String, Object>> outcomes = repository.listTransmissionOutcomes(tradeDate, Math.min(500, normalizeLimit(limit) * 4));
        Map<String, Integer> outcomeCounts = new LinkedHashMap<>();
        for (Map<String, Object> outcome : outcomes) {
            String key = text(outcome.get("outcome"), "insufficient_data");
            outcomeCounts.merge(key, 1, Integer::sum);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tradeDate", tradeDate);
        summary.put("dataStatus", replay.getDataStatus());
        summary.put("dataTradeDate", replay.getDataTradeDate());
        summary.put("indexPointCount", replay.getIndexPoints() == null ? 0 : replay.getIndexPoints().size());
        summary.put("eventCount", replay.getEvents() == null ? 0 : replay.getEvents().size());
        summary.put("industryPointCount", replay.getIndustryPoints() == null ? 0 : replay.getIndustryPoints().size());
        summary.put("episodeCount", episodes.size());
        summary.put("outcomeCount", outcomes.size());
        summary.put("outcomeCounts", outcomeCounts);
        summary.put("effectiveEpisodeCount", episodes.stream().filter(this::isEffectiveEpisode).count());
        summary.put("failedEpisodeCount", outcomes.stream().filter(item -> "failed".equalsIgnoreCase(text(item.get("outcome"), ""))).count());
        summary.put("firstBoardIndustryCount", replay.getIndustryPoints() == null ? 0 : replay.getIndustryPoints().stream()
                .filter(item -> item.getFirstLimitUpAt() != null && item.getFirstLimitUpCode() != null).map(MarketIndicatorIndustrySnapshotBO::getIndustryCode).distinct().count());
        summary.put("coverageWarning", replay.getMessage());
        summary.put("episodes", episodes);
        summary.put("outcomes", outcomes);
        summary.put("source", replay.getSource());
        return summary;
    }

    public Map<String, Object> replayEpisode(LocalDate requestedDate, String episodeId) {
        LocalDate tradeDate = requestedDate == null ? LocalDate.now(MARKET_ZONE) : requestedDate;
        Map<String, Object> match = repository.listTransmissionEpisodes(tradeDate, 2500).stream()
                .filter(item -> episodeId != null && episodeId.equals(String.valueOf(item.getOrDefault("episode_id", item.get("episodeId")))))
                .findFirst().orElse(null);
        if (match == null) {
            return Map.of("ok", false, "error", "episode not found", "tradeDate", tradeDate, "episodeId", episodeId);
        }
        String sectorCode = text(match.get("sector_code"), text(match.get("sectorCode"), null));
        MarketIndicatorReplayBO replay = replayTimeline(tradeDate, LocalTime.of(9, 30), LocalTime.of(15, 0), null, sectorCode, 3000);
        Map<String, Object> result = new LinkedHashMap<>(match);
        result.put("ok", true);
        result.put("tradeDate", tradeDate);
        result.put("timeline", replay);
        result.put("outcomes", repository.listTransmissionOutcomes(tradeDate, 500).stream()
                .filter(item -> episodeId.equals(String.valueOf(item.getOrDefault("episode_id", item.get("episodeId")))))
                .toList());
        result.put("explainability", explainEpisode(match));
        return result;
    }

    public Map<String, Object> explainReplay(Map<String, Object> request) {
        LocalDate date = request == null || request.get("tradeDate") == null ? LocalDate.now(MARKET_ZONE) : LocalDate.parse(String.valueOf(request.get("tradeDate")));
        String episodeId = request == null ? null : text(request.get("episodeId"), null);
        Map<String, Object> episode = episodeId == null ? null : replayEpisode(date, episodeId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", episode != null && Boolean.TRUE.equals(episode.get("ok")));
        result.put("capability", "market_indicator_replay_explanation");
        result.put("tradeDate", date);
        result.put("episodeId", episodeId);
        result.put("status", "deterministic_fallback");
        result.put("summary", episode == null ? "请选择一个已计算的板块 Episode 后生成解释。" : explainEpisode(episode));
        result.put("drivers", episode == null ? List.of() : drivers(episode));
        result.put("counterEvidence", episode == null ? List.of() : counterEvidence(episode));
        result.put("failureConditions", List.of("无跟随", "大盘逆风", "冲高回落", "封板不稳", "数据不足"));
        result.put("uncertainties", List.of("分钟数据覆盖和涨停封板确认以当前数据源为准", "AI provider 尚未执行，当前为确定性解释"));
        result.put("evidenceRefs", episode == null ? List.of() : List.of("market_indicator_sector_episodes", "market_indicator_episode_outcomes", "ch_dwd_kline_1m"));
        return result;
    }

    private String classifyMarketState(MarketIndicatorBreadthBO breadth) {
        if (breadth == null || breadth.getTotalCount() == null || breadth.getTotalCount() == 0) return "unknown";
        double advancing = value(breadth.getAdvancingCount()) / (double) breadth.getTotalCount();
        double declining = value(breadth.getDecliningCount()) / (double) breadth.getTotalCount();
        if (advancing > 0.62 && value(breadth.getLimitUpCount()) >= value(breadth.getLimitDownCount())) return "risk-on";
        if (declining > 0.62 || value(breadth.getLimitDownCount()) > value(breadth.getLimitUpCount()) * 2) return "risk-off";
        if (Math.abs(advancing - declining) < 0.12) return "range";
        return "rotation";
    }

    private String stateReason(MarketIndicatorBreadthBO breadth) {
        if (breadth == null) return "缺少市场宽度数据";
        return "上涨 " + value(breadth.getAdvancingCount()) + " / 下跌 " + value(breadth.getDecliningCount())
                + "；涨停 " + value(breadth.getLimitUpCount()) + " / 跌停 " + value(breadth.getLimitDownCount());
    }

    private boolean isEffectiveEpisode(Map<String, Object> episode) {
        String stage = text(episode.get("stage"), "");
        return "confirmed".equalsIgnoreCase(stage) || "spreading".equalsIgnoreCase(stage) || "crowded".equalsIgnoreCase(stage);
    }

    private String explainEpisode(Map<String, Object> episode) {
        String sector = text(episode.get("sector_name"), text(episode.get("sectorName"), "未知板块"));
        String stage = text(episode.get("stage"), "observation");
        String direction = text(episode.get("direction"), "unknown");
        String coverage = text(episode.get("coverage_peak"), text(episode.get("coveragePeak"), "-"));
        String excess = text(episode.get("benchmark_excess_pct"), text(episode.get("benchmarkExcessPct"), "-"));
        return sector + "当前为" + stage + "阶段，方向=" + direction + "，响应覆盖=" + coverage + "，相对大盘超额=" + excess + "。";
    }

    private List<String> drivers(Map<String, Object> episode) {
        List<String> values = new ArrayList<>();
        if (isEffectiveEpisode(episode)) values.add("达到板块响应证据门槛");
        if (episode.get("median_lag_seconds") != null) values.add("响应滞后=" + episode.get("median_lag_seconds") + "秒");
        if (episode.get("benchmark_excess_pct") != null) values.add("相对大盘超额=" + episode.get("benchmark_excess_pct") + "%");
        return values;
    }

    private List<String> counterEvidence(Map<String, Object> episode) {
        List<String> values = new ArrayList<>();
        if (episode.get("data_quality_grade") != null) values.add("数据质量=" + episode.get("data_quality_grade"));
        if (episode.get("ex_leader_return_pct") != null) values.add("非龙头收益=" + episode.get("ex_leader_return_pct") + "%");
        return values;
    }

    private String text(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> value) {
        return (Map<String, Object>) value;
    }

    public MarketIndicatorReplayBO replayTimeline(LocalDate requestedDate, LocalTime requestedFrom, LocalTime requestedTo,
                                                  String eventType, String industryCode, int limit) {
        LocalDate tradeDate = requestedDate;
        if (tradeDate == null) {
            MarketIndicatorBreadthBO breadth = repository.getBreadth(null);
            tradeDate = breadth == null || breadth.getCapturedAt() == null
                    ? LocalDate.now(MARKET_ZONE) : breadth.getCapturedAt().atZoneSameInstant(MARKET_ZONE).toLocalDate();
        }
        LocalTime from = requestedFrom == null ? LocalTime.of(9, 30) : requestedFrom;
        LocalTime to = requestedTo == null ? LocalTime.of(15, 0) : requestedTo;
        if (to.isBefore(from)) {
            LocalTime swap = from;
            from = to;
            to = swap;
        }
        OffsetDateTime fromAt = tradeDate.atTime(from).atZone(MARKET_ZONE).toOffsetDateTime();
        OffsetDateTime toAt = tradeDate.atTime(to).atZone(MARKET_ZONE).toOffsetDateTime();
        final LocalDate replayTradeDate = tradeDate;
        final LocalTime replayFrom = from;
        final LocalTime replayTo = to;
        final OffsetDateTime replayFromAt = fromAt;
        final OffsetDateTime replayToAt = toAt;
        int boundedLimit = Math.max(10, Math.min(10000, limit <= 0 ? 3000 : limit));
        String normalizedIndustryCode = industryCode == null || industryCode.isBlank()
                ? null : industryCode.trim();
        int eventLimit = normalizedIndustryCode == null ? Math.min(1000, boundedLimit) : boundedLimit;
        List<MarketIndicatorEventBO> events = repository.listReplayEvents(tradeDate, fromAt, toAt,
                normalizeEventType(eventType), normalizedIndustryCode, eventLimit);
        List<MarketIndicatorIndustrySnapshotBO> industries = repository.listReplayIndustries(tradeDate, fromAt, toAt,
                normalizedIndustryCode, boundedLimit);

        LocalDate dataTradeDate = null;
        List<IndexMinuteBarBO> indexPoints = List.of();
        if (pulseRepository != null) {
            dataTradeDate = pulseRepository.findLatestIndexMinuteDate("000001", tradeDate).orElse(null);
            if (tradeDate.equals(dataTradeDate)) {
                indexPoints = pulseRepository.listIndexMinuteBars("000001", tradeDate).stream()
                        .filter(point -> withinReplayWindow(point, replayTradeDate, replayFrom, replayTo, replayFromAt, replayToAt))
                        .toList();
            }
        }
        boolean ready = !indexPoints.isEmpty();
        boolean lagging = dataTradeDate != null && dataTradeDate.isBefore(tradeDate);
        String status = ready ? (industries.isEmpty() ? "partial" : "ready") : (lagging ? "pending" : "unavailable");
        String message = ready ? null : lagging
                ? "1m 数据尚未覆盖请求交易日，当前最新数据为 " + dataTradeDate
                : "当前交易日暂无可用 1m 数据；事件和行业快照仍可先行查看";
        return MarketIndicatorReplayBO.builder().ok(ready || !events.isEmpty() || !industries.isEmpty())
                .error(ready || !events.isEmpty() || !industries.isEmpty() ? null : "replay evidence is empty")
                .tradeDate(tradeDate).dataTradeDate(dataTradeDate).dataStatus(status).source("postgres.market_indicator+clickhouse.ch_dwd_kline_1m")
                .requestedFrom(fromAt).requestedTo(toAt).indexPoints(indexPoints).events(events).industryPoints(industries).message(message).build();
    }

    private boolean withinReplayWindow(IndexMinuteBarBO point, LocalDate tradeDate, LocalTime from, LocalTime to,
                                       OffsetDateTime fromAt, OffsetDateTime toAt) {
        if (point == null) {
            return false;
        }
        String time = point.getTime();
        if (time != null && !time.isBlank()) {
            try {
                LocalDateTime localTime = LocalDateTime.parse(time);
                return !localTime.isBefore(tradeDate.atTime(from)) && !localTime.isAfter(tradeDate.atTime(to));
            } catch (DateTimeParseException ignored) {
                // Fall back to the timestamp for legacy rows with no ISO local time.
            }
        }
        Long timestamp = point.getTimestamp();
        return timestamp == null || (timestamp >= fromAt.toInstant().toEpochMilli()
                && timestamp <= toAt.toInstant().toEpochMilli());
    }

    private void enrichRelation(MarketIndicatorIndustryRelationBO relation, BigDecimal sourceChange) {
        double alignment = directionAlignment(sourceChange, relation.getAverageChangePct());
        double mappingStrength = relation.getAverageRelationLevel() == null
                ? 0.5d
                : Math.max(0d, Math.min(1d, relation.getAverageRelationLevel().doubleValue()));
        double score = Math.min(100d, 100d * (0.60d * mappingStrength + 0.40d * alignment));
        relation.setRelationScore(BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP));
        relation.setRelationState(state(sourceChange, relation.getAverageChangePct()));
    }

    private double directionAlignment(BigDecimal source, BigDecimal target) {
        if (source == null || target == null || source.abs().compareTo(BigDecimal.valueOf(0.2)) < 0
                || target.abs().compareTo(BigDecimal.valueOf(0.2)) < 0) {
            return 0.5d;
        }
        return source.signum() == target.signum() ? 1d : 0d;
    }

    private String state(BigDecimal source, BigDecimal target) {
        if (source == null || target == null || source.abs().compareTo(BigDecimal.valueOf(0.2)) < 0
                || target.abs().compareTo(BigDecimal.valueOf(0.2)) < 0) {
            return "方向未明";
        }
        return source.signum() == target.signum() ? "同向联动" : "反向分化";
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(MAX_LIMIT, limit <= 0 ? DEFAULT_LIMIT : limit));
    }

    private String normalizeStatus(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value) ? null : value.trim().toLowerCase();
    }

    private String normalizeEventType(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value) ? null : value.trim().toLowerCase();
    }
}
