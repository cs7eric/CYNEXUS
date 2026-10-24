package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.FridgeModels.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FridgeDomainServiceTest {
    @Test
    void replaceMealRestoresThenConsumesTheRequestedGramsFromCurrentLots() {
        List<String> calls = new ArrayList<>();
        AtomicReference<MealSource> consumed = new AtomicReference<>();
        cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients per100g =
                new cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients(
                        new BigDecimal("140"), new BigDecimal("2"), new BigDecimal("29"), new BigDecimal("3"));
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> { calls.add("lock"); yield null; }
                    case "restoreMeal" -> { calls.add("restore"); yield null; }
                    case "mealStock" -> {
                        calls.add("read-stock");
                        yield new MealStock("fridge-1", "food-1", "catalog", "Chicken", null,
                                List.of(new MealLot(12, new BigDecimal("500"), "g", null, null, per100g)));
                    }
                    case "consumeMeal" -> { calls.add("consume"); consumed.set((MealSource) args[1]); yield null; }
                    default -> throw new AssertionError(method.getName());
                });

        MealSource replacement = new FridgeDomainService(repository).replaceMeal(
                "owner-a", "fridge-1", "meal-1", new BigDecimal("80"), "operation-1");

        assertThat(calls).containsSubsequence("lock", "restore", "read-stock", "consume");
        assertThat(replacement.consumedGrams()).isEqualByComparingTo("80");
        assertThat(replacement.allocations()).singleElement().satisfies(allocation ->
                assertThat(allocation.quantity()).isEqualByComparingTo("80"));
        assertThat(consumed.get()).isEqualTo(replacement);
    }

    @Test
    void addAcceptsMeasuredUnitsAndDelegatesToTheOwnerScopedRepository() {
        AtomicReference<AddCommand> saved = new AtomicReference<>();
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
            case "lockOwner" -> null;
            case "findConfirmedMapping" -> null;
            case "searchMapping" -> List.of();
            case "add" -> { saved.set((AddCommand) args[1]); yield new Inventory(1, false, List.of(), List.of()); }
            default -> throw new AssertionError(method.getName());
        });
        FridgeDomainService service = new FridgeDomainService(repository);
        service.add("owner-a", new AddCommand(null, "request-1", "Chicken breast", null, new BigDecimal("450"), "g", LocalDate.now().plusDays(3), null, null, null, null));
        assertThat(saved.get().unit()).isEqualTo("g");
        assertThat(saved.get().quantity()).isEqualByComparingTo("450");
    }

    @Test
    void addAutomaticallyUsesAUniqueExactCatalogAliasAndKeepsAmbiguousMatchesForReview() {
        AtomicReference<AddCommand> saved = new AtomicReference<>();
        AtomicReference<String> searchedName = new AtomicReference<>();
        List<MappingCandidate> exactAlias = List.of(new MappingCandidate(
                "food-17", "catalog", "Rolled oats", null, BigDecimal.ONE,
                new Nutrients(
                        new BigDecimal("389"), new BigDecimal("66"), new BigDecimal("17"), new BigDecimal("7"))));
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "findConfirmedMapping" -> null;
                    case "searchMapping" -> {
                        searchedName.set((String) args[1]);
                        yield exactAlias;
                    }
                    case "add" -> { saved.set((AddCommand) args[1]); yield new Inventory(1, false, List.of(), List.of()); }
                    default -> throw new AssertionError(method.getName());
                });

        new FridgeDomainService(repository).add("owner-a", new AddCommand(null, "request-2", "Oats",
                null, new BigDecimal("300"), "g", LocalDate.now().plusDays(10), null, null, null, null));

        assertThat(searchedName.get()).isEqualTo("Oats");
        assertThat(saved.get().mappingResolution()).satisfies(resolution -> {
            assertThat(resolution.catalogFoodId()).isEqualTo("food-17");
            assertThat(resolution.status()).isEqualTo("matched");
            assertThat(resolution.candidates()).isEmpty();
        });
    }

    @Test
    void addDoesNotAutoSelectAmbiguousExactCandidates() {
        AtomicReference<AddCommand> saved = new AtomicReference<>();
        List<MappingCandidate> candidates = List.of(
                new MappingCandidate("food-1", "catalog", "Milk", null, BigDecimal.ONE, null),
                new MappingCandidate("food-2", "catalog", "Milk", null, BigDecimal.ONE, null));
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "findConfirmedMapping" -> null;
                    case "searchMapping" -> candidates;
                    case "add" -> { saved.set((AddCommand) args[1]); yield new Inventory(1, false, List.of(), List.of()); }
                    default -> throw new AssertionError(method.getName());
                });

        new FridgeDomainService(repository).add("owner-a", new AddCommand(null, "request-3", "Milk",
                null, new BigDecimal("1"), "bottle", LocalDate.now().plusDays(10), null, null, null, null));

        assertThat(saved.get().mappingResolution()).satisfies(resolution -> {
            assertThat(resolution.status()).isEqualTo("pending");
            assertThat(resolution.catalogFoodId()).isNull();
            assertThat(resolution.candidates()).hasSize(2);
        });
    }

    @Test
    void addReusesTheOwnersPreviouslyConfirmedNameAndBrandMapping() {
        AtomicReference<AddCommand> saved = new AtomicReference<>();
        AtomicReference<Boolean> searchedCatalog = new AtomicReference<>(false);
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "findConfirmedMapping" -> new MappingCandidate("food-31", "catalog", "Oatmeal", null,
                            BigDecimal.ONE, null);
                    case "searchMapping" -> { searchedCatalog.set(true); yield List.of(); }
                    case "add" -> { saved.set((AddCommand) args[1]); yield new Inventory(1, false, List.of(), List.of()); }
                    default -> throw new AssertionError(method.getName());
                });

        new FridgeDomainService(repository).add("owner-a", new AddCommand(null, "request-4", "My oatmeal",
                null, new BigDecimal("300"), "g", LocalDate.now().plusDays(10), null, null, null, null));

        assertThat(searchedCatalog.get()).isFalse();
        assertThat(saved.get().mappingResolution()).satisfies(resolution -> {
            assertThat(resolution.catalogFoodId()).isEqualTo("food-31");
            assertThat(resolution.status()).isEqualTo("user_confirmed");
        });
    }

    @Test
    void rejectsVolumeWithoutAnExplicitConversionAtTheDomainBoundary() {
        FridgeDomainService service = new FridgeDomainService((FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> null));
        assertThatThrownBy(() -> service.add("owner-a", new AddCommand(null, "request-1", "Milk", null, BigDecimal.ONE, "ml", LocalDate.now().plusDays(3), null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aiPreviewNormalizesCandidatesWithoutWritingInventory() {
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> {
            throw new AssertionError("AI preview must not access the inventory repository: " + method.getName());
        });
        FridgeInventoryAiClient ai = (input, mode) -> new FridgeInventoryAiClient.Completion(
                "{\"items\":[{\"name\":\"Milk\",\"quantity\":2,\"unit\":\"bottles\",\"bestBefore\":null,\"confidence\":1.2,\"notes\":[\"check date\"]}],\"warnings\":[]}",
                "xtyoo", "gpt-6.1-sol");
        FridgeDomainService service = new FridgeDomainService(repository, ai);
        AiPreview preview = service.previewAi(new FridgeDomainService.PreviewCommand("two bottles of milk", "voice"));
        assertThat(preview.provider()).isEqualTo("xtyoo");
        assertThat(preview.model()).isEqualTo("gpt-6.1-sol");
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("Milk");
            assertThat(item.quantity()).isEqualByComparingTo("2");
            assertThat(item.unit()).isEqualTo("bottle");
            assertThat(item.confidence()).isEqualByComparingTo("1");
            assertThat(item.bestBefore()).isNull();
        });
        assertThat(preview.warnings()).anyMatch(value -> value.contains("best-before"));
    }

    @Test
    void aiPreviewPreservesMeasuredUnitsForVolumeAndWeight() {
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> {
                    throw new AssertionError("AI preview must not access the inventory repository: " + method.getName());
                });
        FridgeInventoryAiClient ai = (input, mode) -> new FridgeInventoryAiClient.Completion(
                "{\"items\":[{\"name\":\"Milk\",\"quantity\":500,\"unit\":\"ml\",\"confidence\":0.9,\"notes\":[]},{\"name\":\"Oats\",\"quantity\":300,\"unit\":\"g\",\"confidence\":0.9,\"notes\":[]}],\"warnings\":[]}",
                "xtyoo", "gpt-6.1-sol");

        AiPreview preview = new FridgeDomainService(repository, ai).previewAi(
                new FridgeDomainService.PreviewCommand("500 ml milk and 300 g oats", "text"));

        assertThat(preview.items()).extracting(AiItem::unit).containsExactly("ml", "g");
        assertThat(preview.items()).extracting(AiItem::quantity)
                .containsExactly(new BigDecimal("500"), new BigDecimal("300"));
    }

    @Test
    void aiPreviewKeepsUncertainQuantitiesAndUnitsUnknownForReview() {
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> {
                    throw new AssertionError("AI preview must not access the inventory repository: " + method.getName());
                });
        FridgeInventoryAiClient ai = (input, mode) -> new FridgeInventoryAiClient.Completion(
                "{\"items\":[{\"name\":\"Unmeasured milk\",\"quantity\":null,\"unit\":null,\"confidence\":0.8,\"notes\":[]},{\"name\":\"Unknown spice\",\"quantity\":-1,\"unit\":\"oz\",\"confidence\":0.7,\"notes\":[]}],\"warnings\":[]}",
                "xtyoo", "gpt-6.1-sol");

        AiPreview preview = new FridgeDomainService(repository, ai).previewAi(
                new FridgeDomainService.PreviewCommand("some milk and spice", "text"));

        assertThat(preview.items()).extracting(AiItem::quantity).containsExactly(null, null);
        assertThat(preview.items()).extracting(AiItem::unit).containsExactly(null, null);
        assertThat(preview.warnings()).anyMatch(value -> value.contains("quantity needs review"));
        assertThat(preview.warnings()).anyMatch(value -> value.contains("unit needs review"));
        assertThat(preview.warnings()).anyMatch(value -> value.contains("unsupported unit needs review"));
    }

    @Test
    void nutritionPreviewUsesMaintainedEnglishAliasAndRejectsModelInventedCandidateIds() {
        List<String> searches = new ArrayList<>();
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "searchMapping" -> {
                        searches.add((String) args[1]);
                        if ("Chicken breast".equals(args[1])) yield List.of(new MappingCandidate("food-052", "catalog",
                                "Chicken breast", null, new BigDecimal("0.65"), new Nutrients(
                                new BigDecimal("165"), BigDecimal.ZERO, new BigDecimal("31"), new BigDecimal("3.6"))));
                        yield List.of();
                    }
                    default -> throw new AssertionError("Unexpected repository call " + method.getName());
                });
        FridgeInventoryAiClient ai = new FridgeInventoryAiClient() {
            @Override public Completion preview(String input, String mode) { throw new AssertionError(); }
            @Override public Completion matchNutrition(String context, String tenant, String principal) {
                assertThat(context).contains("food-052").contains("Chicken breast");
                return new Completion("{\"matches\":[{\"itemIndex\":0,\"itemName\":\"鸡胸肉\",\"selectedId\":\"invented-id\",\"confidence\":0.99,\"rankedIds\":[\"food-052\",\"invented-id\"]}]}", "xtyoo", "gpt-6.1-sol");
            }
        };

        NutritionMatchPreview preview = new FridgeDomainService(repository, ai, new FridgeAssetMappingResolver() {
            @Override public List<AssetMapping> assetMappings() { return List.of(new AssetMapping("food-052", "鸡胸肉", "Chicken breast", List.of(), "art", "/art", "meat")); }
            @Override public AssetMapping assetMapping(String key) { return null; }
        }).previewNutritionMatches("owner", List.of(new NutritionMatchInput("鸡胸肉", null)), "tenant", "user");

        assertThat(searches).contains("鸡胸肉", "Chicken breast");
        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("needs_review");
            assertThat(item.selectedId()).isNull();
            assertThat(item.rankedIds()).containsExactly("food-052");
            assertThat(item.candidates()).singleElement().satisfies(candidate -> assertThat(candidate.id()).isEqualTo("food-052"));
            assertThat(item.requiresConfirmation()).isTrue();
        });
    }

    @Test
    void nutritionPreviewKeepsUniqueExactMatchDeterministicWithoutCallingAi() {
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "searchMapping" -> List.of(new MappingCandidate("food-1", "catalog", "Chicken breast", null,
                            BigDecimal.ONE, null));
                    default -> throw new AssertionError("Unexpected repository call " + method.getName());
                });
        FridgeInventoryAiClient ai = new FridgeInventoryAiClient() {
            @Override public Completion preview(String input, String mode) { throw new AssertionError(); }
            @Override public Completion matchNutrition(String input, String tenant, String principal) { throw new AssertionError("Exact match must bypass AI"); }
        };
        FridgeDomainService service = new FridgeDomainService(repository, ai, new FridgeAssetMappingResolver() {
            @Override public List<AssetMapping> assetMappings() { return List.of(); }
            @Override public AssetMapping assetMapping(String key) { return null; }
        });
        NutritionMatchPreview result = service.previewNutritionMatches("owner",
                List.of(new NutritionMatchInput("Chicken breast", null)), "tenant", "user");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("exact_match");
            assertThat(item.selectedId()).isEqualTo("food-1");
            assertThat(item.requiresConfirmation()).isTrue();
        });
    }

    @Test
    void confirmsOnlySupportedNutritionMappingKindsAfterOwnerLock() {
        List<String> calls = new ArrayList<>();
        FridgeRepository repository = (FridgeRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FridgeRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> { calls.add("lock:" + args[0]); yield null; }
                    case "confirmMapping" -> {
                        calls.add("confirm:" + args[1] + ":" + args[2] + ":" + args[3]);
                        yield new Inventory(1, false, List.of(), List.of());
                    }
                    default -> throw new AssertionError(method.getName());
                });
        FridgeDomainService service = new FridgeDomainService(repository);

        service.confirmMapping("owner-a", "fridge-1", new MappingCommand("food-1", "catalog"));

        assertThat(calls).containsExactly("lock:owner-a", "confirm:fridge-1:food-1:catalog");
        assertThatThrownBy(() -> service.confirmMapping("owner-a", "fridge-1",
                new MappingCommand("food-1", "other"))).isInstanceOf(IllegalArgumentException.class);
    }
}
