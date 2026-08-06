package cn.cccs7.cynexus.market.domain.service.indicator;

import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorEventBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorBreadthBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustrySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorSnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryDetailBO;
import cn.cccs7.cynexus.market.domain.entity.MarketIndicatorIndustryRelationBO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import cn.cccs7.cynexus.market.domain.repository.MarketIndicatorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class MarketIndicatorQueryDomainService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 2500;
    private final MarketIndicatorRepository repository;

    public MarketIndicatorQueryDomainService(MarketIndicatorRepository repository) {
        this.repository = repository;
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
