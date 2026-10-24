package cn.cccs7.cynexus.ai.domain.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Canonicalizes common provider deviations before the strict nutrition schema is applied. */
public final class FoodNutritionLabelOutputPolicy {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");

    private FoodNutritionLabelOutputPolicy() { }

    public static String normalize(String content) {
        final JsonNode source;
        try {
            source = MAPPER.readTree(content);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Food nutrition output is not valid JSON", exception);
        }
        if (source == null || !source.isObject()) {
            throw new IllegalArgumentException("Food nutrition output must be a JSON object");
        }

        JsonNode nutritionSource = source.path("nutrition").isObject() ? source.path("nutrition") : source;
        BigDecimal calories = number(first(nutritionSource, "caloriesKcal", "calories", "energyKcal", "calorie", "热量"));
        if (calories == null) calories = number(first(nutritionSource, "energy", "能量"));
        BigDecimal carbs = number(first(nutritionSource, "carbsG", "carbohydratesG", "carbohydrates", "carbs", "碳水化合物"));
        BigDecimal protein = number(first(nutritionSource, "proteinG", "protein", "蛋白质"));
        BigDecimal fat = number(first(nutritionSource, "fatG", "fat", "脂肪"));

        ObjectNode result = MAPPER.createObjectNode();
        boolean recognized = booleanValue(first(source, "recognized", "isRecognized"), false);
        if (!recognized && (calories != null || carbs != null || protein != null || fat != null)) recognized = true;
        result.put("recognized", recognized);
        textOrNull(result, "foodName", first(source, "foodName", "food_name", "productName", "product_name", "name", "品名"));
        textOrNull(result, "brand", first(source, "brand", "brandName", "brand_name", "manufacturer", "品牌"));
        result.put("basis", basis(first(source, "basis", "nutritionBasis", "nutrition_basis", "basisType")));
        BigDecimal servingSize = number(first(source, "servingSizeGrams", "serving_size_grams", "servingSize", "serving_size", "每份"));
        if (servingSize == null) result.putNull("servingSizeGrams"); else result.put("servingSizeGrams", servingSize);

        ObjectNode normalizedNutrition = result.putObject("nutrition");
        decimalOrNull(normalizedNutrition, "caloriesKcal", calories);
        decimalOrNull(normalizedNutrition, "carbsG", carbs);
        decimalOrNull(normalizedNutrition, "proteinG", protein);
        decimalOrNull(normalizedNutrition, "fatG", fat);
        result.put("confidence", confidence(first(source, "confidence", "置信度")));
        return result.toString();
    }

    private static JsonNode first(JsonNode source, String... names) {
        if (source == null || !source.isObject()) return null;
        for (String name : names) {
            JsonNode value = source.get(name);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private static void textOrNull(ObjectNode target, String field, JsonNode value) {
        String text = value == null || value.isNull() ? null : value.asText(null);
        if (text == null || text.isBlank()) target.putNull(field);
        else target.put(field, text.trim());
    }

    private static void decimalOrNull(ObjectNode target, String field, BigDecimal value) {
        if (value == null) target.putNull(field); else target.put(field, value);
    }

    private static BigDecimal number(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isObject()) return number(value.get("value"));
        if (value.isNumber()) return finiteNonNegative(value.decimalValue());
        if (!value.isTextual()) return null;
        Matcher matcher = NUMBER.matcher(value.textValue().replace(",", ""));
        if (!matcher.find()) return null;
        try { return finiteNonNegative(new BigDecimal(matcher.group())); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static BigDecimal finiteNonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0 && value.scale() < 100 ? value : null;
    }

    private static boolean booleanValue(JsonNode value, boolean fallback) {
        if (value == null || value.isNull()) return fallback;
        if (value.isBoolean()) return value.booleanValue();
        if (value.isTextual()) return Boolean.parseBoolean(value.textValue().trim());
        return fallback;
    }

    private static String basis(JsonNode value) {
        if (value == null || value.isNull()) return "unknown";
        String normalized = value.asText("").trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "_");
        if (normalized.equals("per_100g") || normalized.equals("per100g") || normalized.contains("100g")) return "per_100g";
        if (normalized.equals("per_serving") || normalized.equals("perserving") || normalized.contains("serving") || normalized.contains("每份")) return "per_serving";
        return "unknown";
    }

    private static String confidence(JsonNode value) {
        if (value == null || value.isNull()) return "low";
        if (value.isNumber()) {
            double score = value.doubleValue();
            return score >= 0.8 ? "high" : score >= 0.5 ? "medium" : "low";
        }
        String normalized = value.asText("").trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("high") || normalized.contains("高")) return "high";
        if (normalized.contains("medium") || normalized.contains("中")) return "medium";
        return "low";
    }
}
