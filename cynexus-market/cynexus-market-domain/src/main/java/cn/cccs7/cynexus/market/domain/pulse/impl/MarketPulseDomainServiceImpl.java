package cn.cccs7.cynexus.market.domain.pulse.impl;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketOverviewBO;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import cn.cccs7.cynexus.market.domain.pulse.MarketPulseDomainService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MarketPulseDomainServiceImpl implements MarketPulseDomainService {

    private static final List<String> DEFAULT_INDEX_CODES = List.of("000001", "399001", "399006");
    private final MarketPulseQueryRepository repository;

    public MarketPulseDomainServiceImpl(MarketPulseQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void applyServingFacts(MarketOverviewBO overview) {
        if (overview == null || overview.getTradeDate() == null) {
            return;
        }
        repository.findDailyBreadth(overview.getTradeDate()).ifPresent(breadth -> applyBreadth(overview, breadth));
        repository.findLimitEmotionByDate(overview.getTradeDate()).ifPresent(limit -> applyLimitCounts(overview, limit));
    }

    @Override
    public void applyServingFacts(List<MarketOverviewBO> overviews) {
        if (overviews == null || overviews.isEmpty()) {
            return;
        }
        LocalDate start = overviews.stream().map(MarketOverviewBO::getTradeDate).filter(value -> value != null)
                .min(LocalDate::compareTo).orElse(null);
        LocalDate end = overviews.stream().map(MarketOverviewBO::getTradeDate).filter(value -> value != null)
                .max(LocalDate::compareTo).orElse(null);
        Map<LocalDate, MarketOverviewBO> breadthByDate = start == null || end == null ? Map.of()
                : repository.listDailyBreadth(start, end).stream().filter(value -> value.getTradeDate() != null)
                .collect(Collectors.toMap(MarketOverviewBO::getTradeDate, Function.identity(), (left, right) -> right));
        for (MarketOverviewBO overview : overviews) {
            if (overview.getTradeDate() == null) {
                continue;
            }
            MarketOverviewBO breadth = breadthByDate.get(overview.getTradeDate());
            if (breadth != null) {
                applyBreadth(overview, breadth);
            }
            repository.findLimitEmotionByDate(overview.getTradeDate()).ifPresent(limit -> applyLimitCounts(overview, limit));
        }
    }

    @Override
    public int historyDays(String range) {
        return switch (range == null ? "60d" : range) {
            case "20d" -> 20;
            case "120d" -> 120;
            case "1y" -> 365;
            default -> 60;
        };
    }

    @Override
    public int safeHistoryDays(int days) {
        return Math.max(1, Math.min(days, 365));
    }

    @Override
    public String normalizeDate(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.length() == 8 && !normalized.contains("-")) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4, 6) + "-" + normalized.substring(6, 8);
        }
        return normalized;
    }

    @Override
    public String normalizeInterval(String value) {
        return value == null || value.isBlank() ? "1m" : value.trim().toLowerCase();
    }

    @Override
    public List<String> defaultIndexCodes() {
        return DEFAULT_INDEX_CODES;
    }

    @Override
    public String indexName(String code) {
        return switch (code == null ? "" : code) {
            case "000001" -> "上证指数";
            case "399001" -> "深证成指";
            case "399006" -> "创业板指";
            default -> code;
        };
    }

    @Override
    public String resolveServingMode(LocalDate tradeDate, String sourceKind) {
        return tradeDate != null && tradeDate.equals(LocalDate.now()) && "live_capture".equals(sourceKind)
                ? "intraday" : "close";
    }

    private void applyBreadth(MarketOverviewBO overview, MarketOverviewBO breadth) {
        overview.setRisingCount(breadth.getRisingCount());
        overview.setFallingCount(breadth.getFallingCount());
        overview.setFlatCount(breadth.getFlatCount());
        overview.setStockCount(breadth.getStockCount());
    }

    private void applyLimitCounts(MarketOverviewBO overview, MarketLimitDailySnapshotBO limit) {
        overview.setLimitUpCount(limit.getLimitUpCount());
        overview.setLimitDownCount(limit.getLimitDownCount());
    }
}
