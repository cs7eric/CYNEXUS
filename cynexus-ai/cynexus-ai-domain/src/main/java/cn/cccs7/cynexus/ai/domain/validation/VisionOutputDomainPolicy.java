package cn.cccs7.cynexus.ai.domain.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Domain semantic checks for supported vision capabilities. */
public final class VisionOutputDomainPolicy {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private VisionOutputDomainPolicy() { }

    public static boolean isValid(String capability, String content) {
        if (content == null || content.isBlank()) return false;
        try {
            JsonNode root = MAPPER.readTree(content);
            if (root == null || !root.isObject()) return false;
            return switch (capability == null ? "" : capability) {
                case "accounting_receipt_parse" -> validReceipt(root);
                case "portfolio_trade_screenshot_parse" -> validTrade(root);
                case "portfolio_trade_screenshot_review" -> validReview(root);
                case "wechat_image_route" -> validRoute(root);
                case "food_nutrition_label_parse" -> validNutritionLabel(root);
                default -> true;
            };
        } catch (Exception ignored) { return false; }
    }

    private static boolean validReceipt(JsonNode root) {
        JsonNode items = root.get("items");
        if (items == null || !items.isArray()) return false;
        for (JsonNode item : items) {
            if (!item.isObject()) return false;
            JsonNode index = item.get("imageIndex");
            if (index != null && !index.isNull() && (!index.isIntegralNumber() || index.asInt() < 0)) return false;
            JsonNode type = item.get("transactionType");
            if (type != null && !type.isNull() && (!type.isTextual() || !("expense".equals(type.asText()) || "income".equals(type.asText())))) return false;
            JsonNode amount = item.get("amount");
            if (amount != null && !amount.isNull() && (!amount.isNumber() || amount.decimalValue().signum() <= 0)) return false;
        }
        return true;
    }

    private static boolean validTrade(JsonNode root) {
        JsonNode candidates = root.get("candidates");
        if (candidates == null || !candidates.isArray()) return false;
        for (JsonNode candidate : candidates) {
            if (!candidate.isObject()) return false;
            JsonNode side = candidate.get("side");
            if (side != null && !side.isNull() && (!side.isTextual() || !("BUY".equals(side.asText()) || "SELL".equals(side.asText())))) return false;
            JsonNode instrument = candidate.get("instrumentType");
            if (instrument != null && !instrument.isNull() && (!instrument.isTextual() || !java.util.Set.of("STOCK", "ETF", "LOF", "FUND").contains(instrument.asText()))) return false;
            JsonNode row = candidate.get("rowNo");
            if (row != null && !row.isNull() && (!row.isIntegralNumber() || row.asInt() < 1)) return false;
        }
        return true;
    }

    private static boolean validReview(JsonNode root) {
        return root.has("consistent") && root.get("consistent").isBoolean()
                && root.has("confidence") && root.get("confidence").isNumber()
                && root.get("confidence").asDouble() >= 0 && root.get("confidence").asDouble() <= 1
                && root.has("issues") && root.get("issues").isArray();
    }

    private static boolean validRoute(JsonNode root) {
        JsonNode route = root.get("route");
        return route != null && route.isTextual()
                && java.util.Set.of("ACCOUNTING_RECEIPT", "PORTFOLIO_TRADE", "UNKNOWN").contains(route.asText())
                && root.has("confidence") && root.get("confidence").isNumber()
                && root.get("confidence").asDouble() >= 0 && root.get("confidence").asDouble() <= 1;
    }

    private static boolean validNutritionLabel(JsonNode root) {
        JsonNode recognized = root.get("recognized");
        JsonNode name = root.get("foodName");
        JsonNode brand = root.get("brand");
        JsonNode basis = root.get("basis");
        JsonNode servingSize = root.get("servingSizeGrams");
        JsonNode nutrition = root.get("nutrition");
        JsonNode confidence = root.get("confidence");
        if (recognized == null || !recognized.isBoolean()
                || !nullableText(name, 200) || !nullableText(brand, 200)
                || basis == null || !basis.isTextual()
                || !java.util.Set.of("per_100g", "per_serving", "unknown").contains(basis.asText())
                || !nullablePositive(servingSize, 1_000_000)
                || nutrition == null || !nutrition.isObject()
                || !hasOnlyFields(root, "recognized", "foodName", "brand", "basis", "servingSizeGrams", "nutrition", "confidence")
                || !hasOnlyFields(nutrition, "caloriesKcal", "carbsG", "proteinG", "fatG")
                || confidence == null || !confidence.isTextual()
                || !java.util.Set.of("high", "medium", "low").contains(confidence.asText())) return false;

        String[] keys = {"caloriesKcal", "carbsG", "proteinG", "fatG"};
        boolean hasValue = false;
        for (String key : keys) {
            JsonNode value = nutrition.get(key);
            if (!nullableNonNegative(value, 10_000_000)) return false;
            hasValue |= value != null && !value.isNull();
        }
        return recognized.asBoolean() || !hasValue;
    }

    private static boolean hasOnlyFields(JsonNode value, String... names) {
        if (value.size() != names.length) return false;
        for (String name : names) if (!value.has(name)) return false;
        return true;
    }

    private static boolean nullableText(JsonNode value, int maxLength) {
        return value != null && (value.isNull() || value.isTextual() && value.asText().length() <= maxLength);
    }

    private static boolean nullablePositive(JsonNode value, int maxValue) {
        return value != null && (value.isNull() || value.isNumber()
                && value.decimalValue().signum() > 0
                && value.decimalValue().compareTo(java.math.BigDecimal.valueOf(maxValue)) <= 0);
    }

    private static boolean nullableNonNegative(JsonNode value, int maxValue) {
        return value != null && (value.isNull() || value.isNumber()
                && value.decimalValue().signum() >= 0
                && value.decimalValue().compareTo(java.math.BigDecimal.valueOf(maxValue)) <= 0);
    }
}
