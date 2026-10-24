package cn.cccs7.cynexus.core.domain.food.recognition;

import cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients;
import cn.cccs7.cynexus.core.domain.food.FoodPolicy;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FoodRecognitionPolicy {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_IMAGE_BYTES = 12 * 1024 * 1024;
    private static final Pattern IMAGE_NAME = Pattern.compile("(?i)^(?:food-photo(?:-cutout)?|nutrition-label|image|img)[-_]?\\d*\\.(?:jpe?g|png|webp)$");
    private FoodRecognitionPolicy() { }

    public static Command validate(Command value) {
        FoodPolicy.require(value != null, "Food recognition request is required");
        String key = FoodPolicy.key(value.idempotencyKey());
        Image label = image(value.nutritionLabelImage(), true);
        Image food = image(value.foodImage(), false);
        FoodPolicy.require(label.dataUrl().length() + (food == null ? 0 : food.dataUrl().length()) < 32 * 1024 * 1024,
                "Food recognition images are too large");
        Nutrients manual = value.nutrients() == null ? Nutrients.unknown() : value.nutrients();
        if (manual.values().stream().anyMatch(v -> v != null)) FoodPolicy.nutrition(manual);
        FoodPolicy.require(manual.values().stream().anyMatch(v -> v == null), "Nutrition is already complete; save the food directly");
        String assetKey = optional(value.assetKey(), 160, "artwork mapping");
        return new Command(key, food, label, optional(value.name(), 200, "name"), optional(value.brand(), 200, "brand"), assetKey, manual);
    }

    public static String fingerprint(Command command) {
        return FoodPolicy.fingerprint(command);
    }

    public static Prepared parse(String content, Command command) {
        try {
            FoodPolicy.require(content != null && !content.isBlank() && content.length() <= 64 * 1024,
                    "Nutrition analysis returned no usable result");
            JsonNode root = JSON.readTree(content);
            FoodPolicy.require(root != null && root.isObject() && root.path("recognized").isBoolean()
                    && root.path("recognized").booleanValue(), "The nutrition label could not be recognized");
            String basis = root.path("basis").asText("");
            FoodPolicy.require(Set.of("per_100g", "per_serving").contains(basis), "The nutrition measurement basis is unclear");
            FoodPolicy.require(Set.of("high", "medium", "low").contains(root.path("confidence").asText("")),
                    "Nutrition analysis returned invalid confidence");
            FoodPolicy.require(!"low".equals(root.path("confidence").asText()),
                    "Nutrition analysis confidence is low; review the photos before retrying");
            BigDecimal serving = nullableNumber(root.get("servingSizeGrams"));
            FoodPolicy.require(!"per_serving".equals(basis) || serving != null && serving.signum() > 0,
                    "The serving size in grams is required");
            BigDecimal factor = "per_serving".equals(basis)
                    ? new BigDecimal("100").divide(serving, 16, RoundingMode.HALF_UP) : BigDecimal.ONE;
            JsonNode nutrition = root.get("nutrition");
            FoodPolicy.require(nutrition != null && nutrition.isObject(), "Nutrition analysis returned invalid nutrients");
            List<String> fields = List.of("caloriesKcal", "carbsG", "proteinG", "fatG");
            BigDecimal[] result = new BigDecimal[4];
            for (int i = 0; i < fields.size(); i++) {
                BigDecimal recognized = nullableNumber(nutrition.get(fields.get(i)));
                BigDecimal manual = command.nutrients().values().get(i);
                result[i] = manual != null ? manual : recognized == null ? null : recognized.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
            FoodPolicy.require(java.util.Arrays.stream(result).allMatch(v -> v != null),
                    "Nutrition analysis is incomplete; all four nutrition values are required before automatic saving");
            Nutrients values = new Nutrients(result[0], result[1], result[2], result[3]);
            FoodPolicy.nutrition(values);
            String name = command.name() != null ? command.name() : cleanText(root.get("foodName"));
            String brand = command.brand() != null ? command.brand() : cleanText(root.get("brand"));
            FoodPolicy.require(name != null && !name.isBlank(), "The food name could not be recognized");
            return new Prepared(name, brand, command.assetKey(), values, null);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Nutrition analysis returned invalid details", exception);
        }
    }

    public static byte[] imageBytes(Image image) {
        return Base64.getDecoder().decode(image.dataUrl().substring(image.dataUrl().indexOf(',') + 1));
    }

    private static Image image(Image value, boolean required) {
        if (value == null) {
            FoodPolicy.require(!required, "A nutrition label image is required");
            return null;
        }
        FoodPolicy.require(Set.of("image/jpeg", "image/png", "image/webp").contains(value.contentType() == null ? "" : value.contentType()),
                "Use a JPEG, PNG or WebP image");
        String prefix = "data:" + value.contentType() + ";base64,";
        FoodPolicy.require(value.dataUrl() != null && value.dataUrl().startsWith(prefix)
                && value.dataUrl().length() <= 16 * 1024 * 1024 + 100, "Invalid image data");
        byte[] bytes;
        try { bytes = imageBytes(value); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid image data", exception); }
        FoodPolicy.require(bytes.length > 0 && bytes.length <= MAX_IMAGE_BYTES, "Choose an image smaller than 12 MB");
        return new Image(optional(value.fileName(), 200, "image file name"), value.contentType(), value.dataUrl());
    }

    private static BigDecimal nullableNumber(JsonNode value) {
        FoodPolicy.require(value != null && (value.isNull() || value.isNumber()), "Nutrition analysis returned invalid numeric values");
        if (value.isNull()) return null;
        BigDecimal number = value.decimalValue();
        FoodPolicy.require(number.signum() >= 0 && number.compareTo(new BigDecimal("10000000")) <= 0,
                "Nutrition analysis returned invalid numeric values");
        return number;
    }

    private static String cleanText(JsonNode value) {
        FoodPolicy.require(value != null && (value.isNull() || value.isTextual()), "Nutrition analysis returned invalid product details");
        if (value.isNull()) return null;
        String text = value.asText().replaceAll("\\s+", " ").trim()
                .replaceFirst("(?i)^(?:food\\s*name|product\\s*name|brand)\\s*:\\s*", "").trim();
        return text.isBlank() || IMAGE_NAME.matcher(text).matches() ? null : text.substring(0, Math.min(200, text.length()));
    }

    private static String optional(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        return FoodPolicy.text(value, max, field);
    }
}
