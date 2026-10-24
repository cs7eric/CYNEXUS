package cn.cccs7.cynexus.core.api.dto.food;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contracts for the user's refrigerator inventory. Unknown nutrition remains null. */
public final class FridgeDTO {
    private FridgeDTO() {}

    public record Nutrients(BigDecimal caloriesKcal, BigDecimal carbsG, BigDecimal proteinG, BigDecimal fatG) {}

    public record MappingCandidate(String id, String kind, String name, String brand,
                                   BigDecimal confidence, Nutrients nutrition) {}

    public record Item(String id, String catalogFoodId, String customFoodId, String name,
                       BigDecimal quantity, String unit, LocalDate bestBefore,
                       Instant createdAt, Instant updatedAt, String mappingStatus,
                       List<MappingCandidate> mappingCandidates, BigDecimal mappingConfidence,
                       BigDecimal grams, BigDecimal gramsPerUnit, Nutrients nutrition,
                       String nutritionStatus, int stackCount, BigDecimal overflowCount,
                       String assetCatalogKey, String assetKey, String assetUrl) {
        public Item(String id, String catalogFoodId, String customFoodId, String name,
                    BigDecimal quantity, String unit, LocalDate bestBefore,
                    Instant createdAt, Instant updatedAt, String mappingStatus,
                    List<MappingCandidate> mappingCandidates, BigDecimal mappingConfidence,
                    BigDecimal grams, BigDecimal gramsPerUnit, Nutrients nutrition,
                    String nutritionStatus, int stackCount, BigDecimal overflowCount) {
            this(id, catalogFoodId, customFoodId, name, quantity, unit, bestBefore, createdAt,
                    updatedAt, mappingStatus, mappingCandidates, mappingConfidence, grams,
                    gramsPerUnit, nutrition, nutritionStatus, stackCount, overflowCount,
                    null, null, null);
        }
    }

    public record Operation(String id, String itemId, String itemName, String kind,
                            BigDecimal quantityDelta, BigDecimal quantityAfter, Instant at) {}

    public record Inventory(int version, boolean isSample, List<Item> items, List<Operation> operations) {}

    public record AddRequest(String id, String idempotencyKey, String name, String brand, BigDecimal quantity,
                             String unit, LocalDate bestBefore, BigDecimal gramsPerUnit,
                             String catalogFoodId, String customFoodId, String mappingId,
                             String catalogKey) {
        public AddRequest(String id, String idempotencyKey, String name, String brand, BigDecimal quantity,
                          String unit, LocalDate bestBefore, BigDecimal gramsPerUnit,
                          String catalogFoodId, String customFoodId, String mappingId) {
            this(id, idempotencyKey, name, brand, quantity, unit, bestBefore, gramsPerUnit,
                    catalogFoodId, customFoodId, mappingId, null);
        }
    }

    public record AdjustRequest(BigDecimal quantityDelta, String unit, String idempotencyKey,
                                String source, String mealItemId) {}

    public record MappingSearch(String query, List<MappingCandidate> candidates) {}
    public record MappingRequest(String candidateId, String kind) {}

    public record AiPreviewRequest(String input, String inputMode) {}

    public record AiPreview(String provider, String model, String inputMode,
                             List<AiItem> items, List<String> warnings) {}

    public record NutritionMatchRequest(List<NutritionMatchInput> items) {}
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
