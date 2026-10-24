package cn.cccs7.cynexus.core.api.dto.food;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Wire contracts, independent of persistence and domain models. Nutrition units are kcal and grams. */
public final class FoodDTO {
    private FoodDTO() {}
    public record Nutrients(BigDecimal caloriesKcal, BigDecimal carbsG, BigDecimal proteinG, BigDecimal fatG) {}
    public record FoodAsset(String catalogKey, String catalogFoodId, String assetKey, String assetUrl,
                            String sourceAtlas, Integer row, Integer column, Integer printedLabel) {}
    public record Food(String id, String kind, String name, String brand, Nutrients nutrients,
                       String source, String license, String usageScope, boolean isComplete, String favoriteId,
                       String assetKey, String assetUrl) {
        public Food(String id, String kind, String name, String brand, Nutrients nutrients,
                    String source, String license, String usageScope, boolean isComplete, String favoriteId,
                    String assetKey) {
            this(id, kind, name, brand, nutrients, source, license, usageScope, isComplete, favoriteId,
                    assetKey, null);
        }
    }
    public record Item(String id, String mealType, String entryKind, String foodId, String foodName,
                       String brand, BigDecimal quantity, String unit, BigDecimal gramsPerUnit,
                       BigDecimal consumedGrams, Nutrients per100g, Nutrients nutrients, String assetKey,
                       String sourceFridgeItemId, String assetUrl) {
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
    public record Goal(String id, LocalDate effectiveFrom, Nutrients nutrients) {}
    public record EntryRequest(String idempotencyKey, String timezone, String mealType, String entryKind,
                               String foodId, String foodName, BigDecimal quantity, String unit,
                               BigDecimal gramsPerUnit, Nutrients nutrients) {}
    /** A measured portion consumed from the authenticated owner's fridge stock. */
    public record FridgeMealRequest(String idempotencyKey, String timezone, String mealType,
                                    String fridgeItemId, BigDecimal quantity, String unit) {}
    public record EditRequest(BigDecimal quantity, String unit, BigDecimal gramsPerUnit, Nutrients nutrients, String mealType) {}
    public record GoalRequest(String idempotencyKey, LocalDate effectiveFrom, Nutrients nutrients) {}
    public record CustomRequest(String idempotencyKey, String name, String brand, Nutrients nutrients,
                                String assetKey, String assetImageDataUrl) {
        public CustomRequest(String idempotencyKey, String name, String brand, Nutrients nutrients,
                             String assetKey) {
            this(idempotencyKey, name, brand, nutrients, assetKey, null);
        }
        public CustomRequest(String idempotencyKey, String name, String brand, Nutrients nutrients) {
            this(idempotencyKey, name, brand, nutrients, null, null);
        }
    }
    public record FavoriteRequest(String idempotencyKey, String kind, String foodId) {}
    public record DayStatusRequest(String timezone) {}
}
