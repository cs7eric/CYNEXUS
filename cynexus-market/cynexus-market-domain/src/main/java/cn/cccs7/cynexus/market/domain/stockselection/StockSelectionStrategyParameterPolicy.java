package cn.cccs7.cynexus.market.domain.stockselection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Validates the same bounded criterion vocabulary used by the governed Screener. */
@Component
public class StockSelectionStrategyParameterPolicy {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> TEXT_KEYS = Set.of("universe", "industry", "tdxIndustryCode", "thsIndustryCode",
            "conceptSectorId", "styleSectorId", "board", "sort", "sortDir");
    private static final Set<String> BOOLEAN_KEYS = Set.of("excludeSt", "excludeChiNext", "excludeStar", "requiresTdxIndustry");
    private static final Set<String> NUMERIC_KEYS = Set.of("minPrice", "maxPrice", "minChangePct", "maxChangePct",
            "minAmount", "minMarketCap", "maxMarketCap", "minAmplitudePct", "maxAmplitudePct", "minReturnPct5d",
            "maxReturnPct5d", "minReturnPct10d", "maxReturnPct10d", "minReturnPct30d", "maxReturnPct30d",
            "minReturnPct60d", "maxReturnPct60d", "minReturnPct252d", "maxReturnPct252d", "minMainNet",
            "minMainNet5d", "minTurnoverPct", "maxTurnoverPct", "minVolumeRatio", "maxVolumeRatio", "minPe",
            "maxPe", "minBreakoutVolumeMultiple", "maxBreakoutVolumeMultiple", "minBullishBodyPct", "minCloseAboveMa20Pct",
            "minMa5AboveMa20Pct", "maxAtrPct20", "minVolumeVsMa5Multiple", "maxVolumeVsMa5Multiple");
    private static final Set<String> ALL_KEYS = Set.of("universe", "industry", "tdxIndustryCode", "thsIndustryCode",
            "conceptSectorId", "styleSectorId", "board", "sort", "sortDir", "candlestickPattern", "excludeSt", "excludeChiNext", "excludeStar", "requiresTdxIndustry",
            "minPrice", "maxPrice", "minChangePct", "maxChangePct", "minAmount", "minMarketCap", "maxMarketCap",
            "minAmplitudePct", "maxAmplitudePct", "minReturnPct5d", "maxReturnPct5d", "minReturnPct10d",
            "maxReturnPct10d", "minReturnPct30d", "maxReturnPct30d", "minReturnPct60d", "maxReturnPct60d",
            "minReturnPct252d", "maxReturnPct252d", "minMainNet", "minMainNet5d", "minTurnoverPct", "maxTurnoverPct",
            "minVolumeRatio", "maxVolumeRatio", "minPe", "maxPe", "volumeContractionDays",
            "minBreakoutVolumeMultiple", "maxBreakoutVolumeMultiple", "minBullishBodyPct", "minCloseAboveMa20Pct",
            "minMa5AboveMa20Pct", "breakoutHighDays", "maxAtrPct20", "minConsecutiveBullishDays", "minVolumeVsMa5Multiple", "maxVolumeVsMa5Multiple");

    public Prepared prepare(Map<String, Object> input) {
        if (input == null || input.isEmpty()) throw new IllegalArgumentException("criteria must contain at least one supported condition");
        if (input.size() > ALL_KEYS.size()) throw new IllegalArgumentException("criteria contains too many fields");
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (!ALL_KEYS.contains(key)) throw new IllegalArgumentException("unsupported strategy criterion: " + key);
            Object value = entry.getValue();
            if (value == null || value instanceof String string && string.isBlank()) continue;
            if (TEXT_KEYS.contains(key) || "candlestickPattern".equals(key)) normalized.put(key, text(key, value));
            else if (BOOLEAN_KEYS.contains(key)) normalized.put(key, booleanValue(key, value));
            else if ("volumeContractionDays".equals(key)) normalized.put(key, contractionDays(value));
            else if (NUMERIC_KEYS.contains(key)) normalized.put(key, decimal(key, value));
        }
        normalizeEnums(normalized);
        validateRanges(normalized);
        if (!hasRule(normalized)) throw new IllegalArgumentException("criteria must contain at least one filtering condition");
        try {
            String schema = OBJECT_MAPPER.writeValueAsString(normalized);
            List<String> evidence = normalized.keySet().stream().filter(this::isEvidenceCriterion).toList();
            return new Prepared(Map.copyOf(normalized), schema, OBJECT_MAPPER.writeValueAsString(evidence));
        } catch (Exception exception) {
            throw new IllegalStateException("strategy criteria cannot be serialized", exception);
        }
    }

    private boolean hasRule(Map<String, Object> values) {
        return values.entrySet().stream().anyMatch(entry -> !Set.of("universe", "excludeSt", "excludeChiNext", "excludeStar", "sort", "sortDir").contains(entry.getKey()));
    }

    private boolean isEvidenceCriterion(String key) {
        return !Set.of("universe", "sort", "sortDir").contains(key);
    }

    private String text(String key, Object value) {
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || text.length() > 128) throw new IllegalArgumentException(key + " must be 1 to 128 characters");
        return text;
    }

    private boolean booleanValue(String key, Object value) {
        if (!(value instanceof Boolean result)) throw new IllegalArgumentException(key + " must be boolean");
        return result;
    }

    private int contractionDays(Object value) {
        BigDecimal number = decimal("volumeContractionDays", value);
        try {
            int days = number.intValueExact();
            if (days < 1 || days > 10) throw new IllegalArgumentException("volumeContractionDays must be between 1 and 10");
            return days;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("volumeContractionDays must be an integer", exception);
        }
    }

    private BigDecimal decimal(String key, Object value) {
        try {
            BigDecimal result = new BigDecimal(String.valueOf(value));
            if (result.precision() > 18 || result.scale() > 8) throw new IllegalArgumentException(key + " is out of supported precision");
            return result.stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be numeric", exception);
        }
    }

    private void normalizeEnums(Map<String, Object> values) {
        if (values.containsKey("universe") && !Set.of("ALL", "SH", "SZ", "BJ").contains(values.get("universe")))
            throw new IllegalArgumentException("universe is unsupported");
        if (values.containsKey("board") && !Set.of("main_sh", "main_sz", "chinext", "star", "bse", "unknown").contains(values.get("board")))
            throw new IllegalArgumentException("board is unsupported");
        if (values.containsKey("sort") && !Set.of("amount", "price", "change_pct", "amplitude_pct", "return_5d", "return_10d", "return_30d", "return_60d", "return_252d", "main_net", "main_net_5d", "turnover_pct", "volume_ratio", "pe", "market_cap", "code").contains(values.get("sort")))
            throw new IllegalArgumentException("sort is unsupported");
        if (values.containsKey("sortDir") && !Set.of("asc", "desc").contains(values.get("sortDir")))
            throw new IllegalArgumentException("sortDir is unsupported");
        if (values.containsKey("candlestickPattern") && !Set.of("bullish_engulfing", "bearish_engulfing", "hammer", "shooting_star", "doji", "gap_up", "gap_down", "inside_bar").contains(values.get("candlestickPattern")))
            throw new IllegalArgumentException("candlestickPattern is unsupported");
    }

    private void validateRanges(Map<String, Object> values) {
        boundedNonNegative(values, "minPrice"); boundedNonNegative(values, "maxPrice"); boundedNonNegative(values, "minAmount");
        boundedNonNegative(values, "minMarketCap"); boundedNonNegative(values, "maxMarketCap"); boundedNonNegative(values, "minTurnoverPct");
        boundedNonNegative(values, "maxTurnoverPct"); boundedNonNegative(values, "minVolumeRatio"); boundedNonNegative(values, "maxVolumeRatio");
        boundedNonNegative(values, "minBreakoutVolumeMultiple"); boundedNonNegative(values, "maxBreakoutVolumeMultiple");
        boundedNonNegative(values, "minBullishBodyPct"); boundedNonNegative(values, "minVolumeVsMa5Multiple"); boundedNonNegative(values, "maxVolumeVsMa5Multiple");
        minimumNotGreaterThanMaximum(values, "minPrice", "maxPrice");
        minimumNotGreaterThanMaximum(values, "minMarketCap", "maxMarketCap");
        minimumNotGreaterThanMaximum(values, "minChangePct", "maxChangePct");
        minimumNotGreaterThanMaximum(values, "minAmplitudePct", "maxAmplitudePct");
        minimumNotGreaterThanMaximum(values, "minReturnPct5d", "maxReturnPct5d");
        minimumNotGreaterThanMaximum(values, "minReturnPct10d", "maxReturnPct10d");
        minimumNotGreaterThanMaximum(values, "minReturnPct30d", "maxReturnPct30d");
        minimumNotGreaterThanMaximum(values, "minReturnPct60d", "maxReturnPct60d");
        minimumNotGreaterThanMaximum(values, "minReturnPct252d", "maxReturnPct252d");
        minimumNotGreaterThanMaximum(values, "minTurnoverPct", "maxTurnoverPct");
        minimumNotGreaterThanMaximum(values, "minVolumeRatio", "maxVolumeRatio");
        minimumNotGreaterThanMaximum(values, "minPe", "maxPe");
        minimumNotGreaterThanMaximum(values, "minBreakoutVolumeMultiple", "maxBreakoutVolumeMultiple");
        minimumNotGreaterThanMaximum(values, "minVolumeVsMa5Multiple", "maxVolumeVsMa5Multiple");
    }

    private void boundedNonNegative(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof BigDecimal decimal && decimal.signum() < 0) throw new IllegalArgumentException(key + " must not be negative");
    }

    private void minimumNotGreaterThanMaximum(Map<String, Object> values, String minimum, String maximum) {
        BigDecimal min = value(values, minimum); BigDecimal max = value(values, maximum);
        if (min != null && max != null && min.compareTo(max) > 0) throw new IllegalArgumentException(minimum + " must not exceed " + maximum);
    }

    private BigDecimal value(Map<String, Object> values, String key) { return values.get(key) instanceof BigDecimal value ? value : null; }

    public record Prepared(Map<String, Object> criteria, String parameterSchema, String evidenceSchema) {}
}
