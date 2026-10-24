package cn.cccs7.cynexus.core.domain.food;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FridgeModels {
    private FridgeModels() {}

    public record Nutrients(BigDecimal caloriesKcal, BigDecimal carbsG, BigDecimal proteinG, BigDecimal fatG) {
        public static Nutrients unknown() { return new Nutrients(null, null, null, null); }
        public List<BigDecimal> values() { return java.util.Arrays.asList(caloriesKcal, carbsG, proteinG, fatG); }
    }
    public record MappingCandidate(String id, String kind, String name, String brand,
                                   BigDecimal confidence, Nutrients nutrition) {}
    public record MappingResolution(String catalogFoodId, String customFoodId, String status,
                                    BigDecimal confidence, List<MappingCandidate> candidates) {}
    public record Item(String id, String catalogFoodId, String customFoodId, String name,
                       BigDecimal quantity, String unit, LocalDate bestBefore, Instant createdAt,
                       Instant updatedAt, String mappingStatus, List<MappingCandidate> mappingCandidates,
                       BigDecimal mappingConfidence, BigDecimal grams, BigDecimal gramsPerUnit,
                       Nutrients nutrition, String nutritionStatus, int stackCount,
                       BigDecimal overflowCount, String assetCatalogKey, String assetKey,
                       String assetUrl) {
        public Item(String id, String catalogFoodId, String customFoodId, String name,
                    BigDecimal quantity, String unit, LocalDate bestBefore, Instant createdAt,
                    Instant updatedAt, String mappingStatus, List<MappingCandidate> mappingCandidates,
                    BigDecimal mappingConfidence, BigDecimal grams, BigDecimal gramsPerUnit,
                    Nutrients nutrition, String nutritionStatus, int stackCount,
                    BigDecimal overflowCount) {
            this(id, catalogFoodId, customFoodId, name, quantity, unit, bestBefore, createdAt,
                    updatedAt, mappingStatus, mappingCandidates, mappingConfidence, grams,
                    gramsPerUnit, nutrition, nutritionStatus, stackCount, overflowCount,
                    null, null, null);
        }
    }
    public record Operation(String id, String itemId, String itemName, String kind,
                            BigDecimal quantityDelta, BigDecimal quantityAfter, Instant at) {}
    public record Inventory(int version, boolean isSample, List<Item> items, List<Operation> operations) {}
    public record MealAllocation(long lotId, BigDecimal quantity, String unit, BigDecimal consumedGrams) {}
    public record MealLot(long id, BigDecimal quantity, String unit, BigDecimal gramsPerUnit,
                          BigDecimal density, FoodModels.Nutrients nutrients) {}
    public record MealStock(String fridgeItemId, String foodId, String foodKind, String name, String brand,
                            List<MealLot> lots) {}
    public record MealSource(String fridgeItemId, String foodId, String foodKind, String name, String brand,
                             BigDecimal requestedQuantity, String requestedUnit, BigDecimal consumedGrams,
                             FoodModels.Nutrients per100g,
                             List<MealAllocation> allocations) {}
    public record AddCommand(String id, String idempotencyKey, String name, String brand, BigDecimal quantity,
                             String unit, LocalDate bestBefore, BigDecimal gramsPerUnit,
                             String catalogFoodId, String customFoodId, String mappingId,
                             String catalogKey, MappingResolution mappingResolution) {
        public AddCommand(String id, String idempotencyKey, String name, String brand, BigDecimal quantity,
                          String unit, LocalDate bestBefore, BigDecimal gramsPerUnit,
                          String catalogFoodId, String customFoodId, String mappingId, String catalogKey) {
            this(id, idempotencyKey, name, brand, quantity, unit, bestBefore, gramsPerUnit,
                    catalogFoodId, customFoodId, mappingId, catalogKey, null);
        }
        public AddCommand(String id, String idempotencyKey, String name, String brand, BigDecimal quantity,
                          String unit, LocalDate bestBefore, BigDecimal gramsPerUnit,
                          String catalogFoodId, String customFoodId, String mappingId) {
            this(id, idempotencyKey, name, brand, quantity, unit, bestBefore, gramsPerUnit,
                    catalogFoodId, customFoodId, mappingId, null, null);
        }
    }
    public record AdjustCommand(BigDecimal quantityDelta, String unit, String idempotencyKey,
                                String source, String mealItemId) {}
    public record MappingCommand(String candidateId, String kind) {}
    public record MappingSearch(String query, List<MappingCandidate> candidates) {}
    public record AssetMapping(String catalogKey, String canonicalName, String englishName,
                               List<String> aliases, String assetKey, String assetUrl, String category) {}
    public record AssetMappingDecision(String status, String method, BigDecimal confidence,
                                       AssetMapping mapping, List<AssetMapping> candidates) {}
    public record AiPreview(String provider, String model, String inputMode,
                            List<AiItem> items, List<String> warnings) {}
    public record NutritionMatchInput(String name, String brand) {}
    public record NutritionMatchCandidate(String id, String kind, String name, String brand,
                                          BigDecimal confidence, Nutrients nutrition) {}
    public record NutritionMatch(String name, String brand, String status, String selectedId,
                                 BigDecimal confidence, List<NutritionMatchCandidate> candidates,
                                 List<String> rankedIds, boolean requiresConfirmation) {}
    public record NutritionMatchPreview(String provider, String model, List<NutritionMatch> items) {}
    public record AiItem(String name, String brand, BigDecimal quantity, String unit,
                         LocalDate bestBefore, BigDecimal confidence, List<String> notes,
                         String catalogKey, String assetKey, String assetUrl,
                         String assetMappingStatus, BigDecimal assetMappingConfidence) {
        public AiItem(String name, String brand, BigDecimal quantity, String unit,
                      LocalDate bestBefore, BigDecimal confidence, List<String> notes) {
            this(name, brand, quantity, unit, bestBefore, confidence, notes,
                    null, null, null, "unavailable", null);
        }
    }
}
