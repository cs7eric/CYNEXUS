package cn.cccs7.cynexus.market.domain.pulse;

import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailySnapshotBO;
import cn.cccs7.cynexus.market.domain.entity.MarketLimitDailyStockBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Normalizes persisted market-pulse payloads before they cross into the API layer. */
public final class MarketPulsePayloadDomainPolicy {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode normalizeHeatmap(String payload, String kind, String source) {
        JsonNode parsed = parse(payload);
        ObjectNode value = parsed != null && parsed.isObject() ? (ObjectNode) parsed : mapper.createObjectNode();
        value.put("ok", true);
        if (blank(value, "kind")) {
            value.put("kind", kind);
        }
        if (blank(value, "source")) {
            value.put("source", source == null ? "java-pg" : source);
        }
        ArrayNode items = array(value, "items");
        for (JsonNode itemNode : items) {
            if (!itemNode.isObject()) {
                continue;
            }
            ObjectNode item = (ObjectNode) itemNode;
            copyIfMissing(item, "changePercent", "change_pct", "changePct");
            copyIfMissing(item, "changePct", "changePercent", "change_pct");
            copyIfMissing(item, "stockCount", "count");
            copyIfMissing(item, "count", "stockCount");
            ensureArray(item, "conceptTags");
            ArrayNode children = array(item, "children");
            for (JsonNode childNode : children) {
                if (childNode.isObject()) {
                    ObjectNode child = (ObjectNode) childNode;
                    ensureArray(child, "conceptTags");
                    if (!child.has("isLimitUp") || child.get("isLimitUp").isNull()) {
                        child.put("isLimitUp", false);
                    }
                }
            }
        }
        value.put("count", items.size());
        return value;
    }

    public JsonNode parsePayload(String payload) {
        return parse(payload);
    }

    public JsonNode normalizeLimitEmotion(String payload,
                                          MarketLimitDailySnapshotBO snapshot,
                                          List<MarketLimitDailyStockBO> limitUpStocks,
                                          List<MarketLimitDailyStockBO> limitDownStocks,
                                          List<MarketLimitDailyStockBO> brokenStocks) {
        JsonNode parsed = parse(payload);
        ObjectNode value = parsed != null && parsed.isObject() ? (ObjectNode) parsed : mapper.createObjectNode();
        JsonNode extra = parse(snapshot.getExtra());
        if (extra != null && extra.isObject()) {
            ObjectNode extraObject = (ObjectNode) extra;
            copyIfMissing(value, "above5PctCount", extraObject, "above5PctCount", "above_5_pct_count", "above5_count");
            copyIfMissing(value, "medianChangePct", extraObject, "medianChangePct", "median_change_pct", "medianReturnPct");
            copyIfMissing(value, "advancingCount", extraObject, "advancingCount", "risingCount", "rising_count");
            copyIfMissing(value, "decliningCount", extraObject, "decliningCount", "fallingCount", "falling_count");
            copyIfMissing(value, "flatCount", extraObject, "flatCount", "flat_count");
        }
        value.put("ok", true);
        if (!value.hasNonNull("tradeDate")) {
            value.put("tradeDate", snapshot.getTradeDate() == null ? null : snapshot.getTradeDate().toString());
        }
        if (blank(value, "updateTime") && snapshot.getUpdatedAt() != null) {
            value.put("updateTime", snapshot.getUpdatedAt().toString());
        }
        if (blank(value, "marketStatus")) {
            value.put("marketStatus", snapshot.getMarketStatus());
        }
        if (blank(value, "dataStatus")) {
            value.put("dataStatus", snapshot.getDataStatus() == null ? "normal" : snapshot.getDataStatus());
        }
        if (!value.hasNonNull("limitUp")) {
            value.set("limitUp", limitBlock("涨停", snapshot.getLimitUpCount(), limitUpStocks));
        } else {
            normalizeLimitBlock((ObjectNode) value.get("limitUp"), snapshot.getLimitUpCount());
        }
        mergeStockNames(array((ObjectNode) value.get("limitUp"), "stocks"), limitUpStocks);
        if (!value.hasNonNull("limitDown")) {
            value.set("limitDown", limitBlock("跌停", snapshot.getLimitDownCount(), limitDownStocks));
        } else {
            normalizeLimitBlock((ObjectNode) value.get("limitDown"), snapshot.getLimitDownCount());
        }
        mergeStockNames(array((ObjectNode) value.get("limitDown"), "stocks"), limitDownStocks);
        if (!value.hasNonNull("breakBoard")) {
            value.set("breakBoard", breakBoard(snapshot, brokenStocks));
        } else {
            ensureArray((ObjectNode) value.get("breakBoard"), "brokenStocks");
        }
        mergeStockNames(array((ObjectNode) value.get("breakBoard"), "brokenStocks"), brokenStocks);
        ObjectNode streak = value.hasNonNull("streak") && value.get("streak").isObject()
                ? (ObjectNode) value.get("streak") : mapper.createObjectNode();
        value.set("streak", streak);
        if (blank(streak, "label")) {
            streak.put("label", "连板高度");
        }
        if (!streak.hasNonNull("maxHeight")) {
            putNullable(streak, "maxHeight", snapshot.getMaxStreakHeight());
        }
        ensureArray(streak, "leaders");
        mergeStockNames(array(streak, "leaders"), limitUpStocks);
        ArrayNode distribution = array(streak, "distribution");
        for (JsonNode rowNode : distribution) {
            if (rowNode.isObject()) {
                ObjectNode row = (ObjectNode) rowNode;
                copyIfMissing(row, "streak", "height");
                ensureArray(row, "stocks");
                mergeStockNames(array(row, "stocks"), limitUpStocks);
            }
        }
        fillDistributionStocks(streak, value.path("limitUp").path("stocks"));
        ObjectNode promotion = streak.hasNonNull("promotion") && streak.get("promotion").isObject()
                ? (ObjectNode) streak.get("promotion") : mapper.createObjectNode();
        streak.set("promotion", promotion);
        ensureArray(promotion, "levels");
        if (!promotion.hasNonNull("overallRate")) {
            putNullable(promotion, "overallRate", snapshot.getPromotionOverallRate());
        }
        fillPromotionLevels(promotion, value.path("limitUp").path("stocks"), streak.path("broken").path("stocks"));
        ObjectNode broken = streak.hasNonNull("broken") && streak.get("broken").isObject()
                ? (ObjectNode) streak.get("broken") : mapper.createObjectNode();
        streak.set("broken", broken);
        if (!broken.hasNonNull("count")) {
            putNullable(broken, "count", snapshot.getBrokenCount());
        }
        if (!broken.hasNonNull("highStreakBrokenCount")) {
            broken.put("highStreakBrokenCount", 0);
        }
        ensureArray(broken, "stocks");
        mergeStockNames(array(broken, "stocks"), brokenStocks);
        ObjectNode sentiment = streak.hasNonNull("sentiment") && streak.get("sentiment").isObject()
                ? (ObjectNode) streak.get("sentiment") : mapper.createObjectNode();
        streak.set("sentiment", sentiment);
        if (blank(sentiment, "level")) {
            sentiment.put("level", snapshot.getSentimentLevel() == null ? "normal" : snapshot.getSentimentLevel());
        }
        if (sentiment.path("text").isMissingNode() || sentiment.path("text").isNull()) {
            sentiment.put("text", snapshot.getSentimentText() == null ? "" : snapshot.getSentimentText());
        }
        return value;
    }

    private ObjectNode limitBlock(String label, Integer count, List<MarketLimitDailyStockBO> stocks) {
        ObjectNode value = mapper.createObjectNode();
        putNullable(value, "count", count);
        value.put("label", label);
        ArrayNode items = value.putArray("stocks");
        if (stocks != null) {
            stocks.forEach(stock -> items.add(stockNode(stock)));
        }
        return value;
    }

    private ObjectNode breakBoard(MarketLimitDailySnapshotBO snapshot, List<MarketLimitDailyStockBO> stocks) {
        ObjectNode value = mapper.createObjectNode();
        putNullable(value, "touchedCount", snapshot.getTouchedCount());
        putNullable(value, "brokenCount", snapshot.getBrokenCount());
        putNullable(value, "rate", snapshot.getBreakBoardRate());
        value.put("status", "ready");
        value.put("label", "炸板率");
        ArrayNode items = value.putArray("brokenStocks");
        if (stocks != null) {
            stocks.forEach(stock -> items.add(stockNode(stock)));
        }
        return value;
    }

    private ObjectNode stockNode(MarketLimitDailyStockBO stock) {
        ObjectNode value = mapper.createObjectNode();
        value.put("code", stock.getCode());
        value.put("name", stock.getName());
        putNullable(value, "streak", stock.getStreak());
        putNullable(value, "changePct", stock.getChangePct());
        putNullable(value, "limitUpPrice", stock.getLimitUpPrice());
        putNullable(value, "limitDownPrice", stock.getLimitDownPrice());
        return value;
    }

    private void normalizeLimitBlock(ObjectNode block, Integer count) {
        if (!block.hasNonNull("count")) {
            putNullable(block, "count", count);
        }
        ensureArray(block, "stocks");
    }

    private void mergeStockNames(ArrayNode target, List<MarketLimitDailyStockBO> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        Map<String, String> namesByCode = new LinkedHashMap<>();
        for (MarketLimitDailyStockBO stock : source) {
            if (stock == null
                    || stock.getCode() == null
                    || stock.getCode().isBlank()
                    || stock.getName() == null
                    || stock.getName().isBlank()) {
                continue;
            }
            namesByCode.put(stock.getCode().trim(), stock.getName().trim());
        }
        for (JsonNode node : target) {
            if (!node.isObject()) {
                continue;
            }
            ObjectNode stock = (ObjectNode) node;
            if (!blank(stock, "name")) {
                continue;
            }
            String name = namesByCode.get(stock.path("code").asText().trim());
            if (name != null) {
                stock.put("name", name);
            }
        }
    }

    private void fillDistributionStocks(ObjectNode streak, JsonNode limitUpStocks) {
        for (JsonNode rowNode : array(streak, "distribution")) {
            if (!rowNode.isObject()) {
                continue;
            }
            ObjectNode row = (ObjectNode) rowNode;
            if (!array(row, "stocks").isEmpty() || !row.hasNonNull("streak")) {
                continue;
            }
            ArrayNode matches = mapper.createArrayNode();
            for (JsonNode stock : limitUpStocks) {
                if (row.path("streak").asInt() == stock.path("streak").asInt()) {
                    matches.add(stock);
                }
            }
            row.set("stocks", matches);
        }
    }

    private void fillPromotionLevels(ObjectNode promotion, JsonNode limitUpStocks, JsonNode brokenStocks) {
        Map<Integer, Integer> denominator = new LinkedHashMap<>();
        Map<Integer, Integer> numerator = new LinkedHashMap<>();
        for (JsonNode stock : limitUpStocks) {
            int streak = stock.path("streak").asInt(0);
            if (streak > 1) {
                denominator.merge(streak - 1, 1, Integer::sum);
                numerator.merge(streak - 1, 1, Integer::sum);
            }
        }
        for (JsonNode stock : brokenStocks) {
            int previous = stock.path("previousStreak").asInt(0);
            if (previous > 0) {
                denominator.merge(previous, 1, Integer::sum);
            }
        }
        ArrayNode levels = mapper.createArrayNode();
        int totalDenominator = 0;
        int totalNumerator = 0;
        for (Map.Entry<Integer, Integer> entry : denominator.entrySet()) {
            int from = entry.getKey();
            int yesterday = entry.getValue();
            int promoted = numerator.getOrDefault(from, 0);
            totalDenominator += yesterday;
            totalNumerator += promoted;
            ObjectNode level = mapper.createObjectNode();
            level.put("from", from);
            level.put("to", from + 1);
            level.put("yesterdayCount", yesterday);
            level.put("todayPromotedCount", promoted);
            level.put("rate", BigDecimal.valueOf(promoted).divide(BigDecimal.valueOf(yesterday), 4, RoundingMode.HALF_UP));
            levels.add(level);
        }
        promotion.set("levels", levels);
        if (!promotion.hasNonNull("overallRate") && totalDenominator > 0) {
            promotion.put("overallRate", BigDecimal.valueOf(totalNumerator)
                    .divide(BigDecimal.valueOf(totalDenominator), 4, RoundingMode.HALF_UP));
        }
    }

    private JsonNode parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ArrayNode array(ObjectNode value, String field) {
        if (value.get(field) instanceof ArrayNode array) {
            return array;
        }
        return value.putArray(field);
    }

    private void ensureArray(ObjectNode value, String field) {
        array(value, field);
    }

    private boolean blank(ObjectNode value, String field) {
        return !value.hasNonNull(field) || value.path(field).asText().isBlank();
    }

    private void copyIfMissing(ObjectNode value, String target, String... sources) {
        if (value.hasNonNull(target)) {
            return;
        }
        for (String source : sources) {
            if (value.hasNonNull(source)) {
                value.set(target, value.get(source));
                return;
            }
        }
    }

    private void copyIfMissing(ObjectNode target, String targetField, ObjectNode source, String... sourceFields) {
        if (target.hasNonNull(targetField)) {
            return;
        }
        for (String sourceField : sourceFields) {
            if (source.hasNonNull(sourceField)) {
                target.set(targetField, source.get(sourceField));
                return;
            }
        }
    }

    private void putNullable(ObjectNode value, String field, Object data) {
        if (data == null) {
            value.putNull(field);
        } else if (data instanceof Integer integer) {
            value.put(field, integer);
        } else if (data instanceof BigDecimal decimal) {
            value.put(field, decimal);
        } else {
            value.put(field, data.toString());
        }
    }
}
