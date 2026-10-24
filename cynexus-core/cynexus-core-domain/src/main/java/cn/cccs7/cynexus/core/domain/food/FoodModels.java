package cn.cccs7.cynexus.core.domain.food;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Private diary values. Unknown nutrition is always null, never an observed zero. */
public final class FoodModels {
    private FoodModels() {}
    public record Nutrients(BigDecimal caloriesKcal, BigDecimal carbsG, BigDecimal proteinG, BigDecimal fatG) {
        public static Nutrients unknown() { return new Nutrients(null, null, null, null); }
        public List<BigDecimal> values() { return java.util.Arrays.asList(caloriesKcal, carbsG, proteinG, fatG); }
        public Nutrients scale(BigDecimal grams) {
            return new Nutrients(scale(caloriesKcal, grams), scale(carbsG, grams), scale(proteinG, grams), scale(fatG, grams));
        }
        private static BigDecimal scale(BigDecimal value, BigDecimal grams) {
            return value == null ? null : value.multiply(grams).movePointLeft(2);
        }
    }
    public record FoodAsset(String catalogKey, String catalogFoodId, String assetKey, String assetUrl,
                            String sourceAtlas, Integer row, Integer column, Integer printedLabel) {}
    public record Food(String id, String kind, String name, String brand, Nutrients nutrients,
                       String source, String license, String usageScope, boolean isComplete, String favoriteId,
                       String sourceSnapshot, String assetKey, String assetUrl) {
        public Food(String id, String kind, String name, String brand, Nutrients nutrients,
                    String source, String license, String usageScope, boolean isComplete, String favoriteId,
                    String sourceSnapshot, String assetKey) {
            this(id, kind, name, brand, nutrients, source, license, usageScope, isComplete, favoriteId,
                    sourceSnapshot, assetKey, null);
        }
    }
    public record Item(String id, String mealType, String entryKind, String foodId, String foodName,
                       String brand, BigDecimal quantity, String unit, BigDecimal gramsPerUnit,
                       BigDecimal consumedGrams, Nutrients per100g, Nutrients nutrients, String assetKey,
                       String sourceFridgeItemId, String assetUrl) {
        public Item(String id, String mealType, String entryKind, String foodId, String foodName,
                    String brand, BigDecimal quantity, String unit, BigDecimal gramsPerUnit,
                    BigDecimal consumedGrams, Nutrients per100g, Nutrients nutrients, String assetKey) {
            this(id, mealType, entryKind, foodId, foodName, brand, quantity, unit, gramsPerUnit,
                    consumedGrams, per100g, nutrients, assetKey, null, null);
        }
        public Item(String id, String mealType, String entryKind, String foodId, String foodName,
                    String brand, BigDecimal quantity, String unit, BigDecimal gramsPerUnit,
                    BigDecimal consumedGrams, Nutrients per100g, Nutrients nutrients, String assetKey,
                    String sourceFridgeItemId) {
            this(id, mealType, entryKind, foodId, foodName, brand, quantity, unit, gramsPerUnit,
                    consumedGrams, per100g, nutrients, assetKey, sourceFridgeItemId, null);
        }
    }
    public record Day(LocalDate date, String status, String timezone, Nutrients targets,
                      Nutrients totals, Nutrients knownTotals, Nutrients missingCounts, List<Item> items) {}
    public record DayState(String id, LocalDate date, String status, String timezone, Nutrients targets) {}
    public record Goal(String id, LocalDate effectiveFrom, Nutrients nutrients) {}
    public record EntryCommand(String idempotencyKey, String timezone, String mealType, String entryKind,
                               String foodId, String foodName, BigDecimal quantity, String unit,
                               BigDecimal gramsPerUnit, Nutrients nutrients) {}
    public record EditCommand(BigDecimal quantity, String unit, BigDecimal gramsPerUnit, Nutrients nutrients, String mealType) {
        public EditCommand(BigDecimal quantity, String unit, BigDecimal gramsPerUnit, Nutrients nutrients) {
            this(quantity, unit, gramsPerUnit, nutrients, null);
        }
    }
    public record StoredRequest(String id, String fingerprint, boolean deleted) {}
}
