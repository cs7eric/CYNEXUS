package cn.cccs7.cynexus.core.domain.food;

import cn.cccs7.cynexus.core.domain.food.FoodModels.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import cn.cccs7.cynexus.core.domain.food.FridgeModels.MealSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FoodDomainServiceTest {
    @Test void fridgeQuantityEditStoresTheReplacementLotNutritionSnapshot() {
        LocalDate date = LocalDate.parse("2026-10-21");
        DayState state = new DayState("day-1", date, "open", "Asia/Shanghai", Nutrients.unknown());
        Nutrients oldNutrition = new Nutrients(new BigDecimal("120"), new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("2"));
        Item existing = new Item("meal-1", "lunch", "catalog", "food-1", "Chicken", null,
                new BigDecimal("100"), "g", BigDecimal.ONE, new BigDecimal("100"), oldNutrition,
                oldNutrition, null, "fridge-1");
        AtomicReference<Item> saved = new AtomicReference<>();
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "day" -> state;
                    case "item" -> existing;
                    case "updateItem" -> { saved.set((Item) args[2]); yield null; }
                    case "items" -> List.of(saved.get() == null ? existing : saved.get());
                    default -> throw new AssertionError(method.getName());
                });
        FoodDomainService service = new FoodDomainService(repository);
        Nutrients replacementNutrition = new Nutrients(new BigDecimal("150"), new BigDecimal("0"), new BigDecimal("31"), new BigDecimal("3"));
        MealSource replacement = new MealSource("fridge-1", "food-1", "catalog", "Chicken", null,
                new BigDecimal("90"), "g", new BigDecimal("90"), replacementNutrition, List.of());

        service.updateFridge("owner-a", date, "meal-1",
                new EditCommand(new BigDecimal("90"), "g", BigDecimal.ONE, null, "dinner"), replacement);

        assertThat(saved.get().mealType()).isEqualTo("dinner");
        assertThat(saved.get().quantity()).isEqualByComparingTo("90");
        assertThat(saved.get().sourceFridgeItemId()).isEqualTo("fridge-1");
        assertThat(saved.get().per100g()).isEqualTo(replacementNutrition);
        assertThat(saved.get().nutrients().caloriesKcal()).isEqualByComparingTo("135");
        assertThat(saved.get().nutrients().proteinG()).isEqualByComparingTo("27.9");
    }

    @Test void fridgeEntryDeleteIsAvailableOnlyThroughTheRestoreAwareDomainOperation() {
        LocalDate date = LocalDate.parse("2026-10-21");
        DayState state = new DayState("day-1", date, "open", "Asia/Shanghai", Nutrients.unknown());
        Item existing = new Item("meal-1", "lunch", "catalog", "food-1", "Chicken", null,
                new BigDecimal("100"), "g", BigDecimal.ONE, new BigDecimal("100"),
                new Nutrients(new BigDecimal("120"), null, null, null),
                new Nutrients(new BigDecimal("120"), null, null, null), null, "fridge-1");
        AtomicInteger deletes = new AtomicInteger();
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "day" -> state;
                    case "item" -> existing;
                    case "deleteItem" -> { deletes.incrementAndGet(); yield null; }
                    case "items" -> List.of();
                    default -> throw new AssertionError(method.getName());
                });

        new FoodDomainService(repository).deleteFridge("owner-a", date, "meal-1");

        assertThat(deletes).hasValue(1);
        assertThatThrownBy(() -> new FoodDomainService(repository).delete("owner-a", date, "meal-1"))
                .hasMessageContaining("cannot be deleted");
    }

    @Test void customRetryReturnsSameIdAndRejectsDifferentPayloadWithoutWriting() {
        AtomicInteger writes=new AtomicInteger();
        StoredRequest[] request={null};
        Nutrients nutrition=new Nutrients(new BigDecimal("100"),null,null,null);
        Food value=new Food("42","custom","Soup",null,nutrition,"Owner entered",null,null,false,null,"{}",null);
        FoodRepository repository=(FoodRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{FoodRepository.class},(proxy,method,args)->{
            assertThat(args[0]).isEqualTo("owner-a");
            return switch(method.getName()) {
                case "lockOwner" -> null;
                case "request" -> request[0];
                case "customFoods" -> List.of();
                case "saveCustom" -> { writes.incrementAndGet(); request[0]=new StoredRequest("42",(String)args[5],false); yield "42"; }
                case "food" -> { assertThat(args[2]).isEqualTo("42"); yield value; }
                default -> throw new AssertionError(method.getName());
            };
        });
        FoodDomainService service=new FoodDomainService(repository);
        assertThat(service.saveCustom("owner-a","Soup",null,nutrition,"key").id()).isEqualTo("42");
        assertThat(service.saveCustom("owner-a","Soup",null,nutrition,"key").id()).isEqualTo("42");
        assertThat(writes.get()).isEqualTo(1);
        assertThatThrownBy(() -> service.saveCustom("owner-a","Other",null,nutrition,"key")).hasMessageContaining("different request");
    }

    @Test void customSaveResolvesAndPersistsTheSelectedArtworkMapping() {
        AtomicReference<String> savedAsset = new AtomicReference<>();
        Nutrients nutrition = new Nutrients(new BigDecimal("100"), null, null, null);
        Food value = new Food("42", "custom", "Soup", null, nutrition, "Owner entered", null, null, false, null, "{}", "food-201");
        FoodAsset asset = new FoodAsset("food-201", null, "food-201", "/mobile/fridge/foods/food-201.webp", null, null, null, 201);
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
            case "lockOwner" -> null;
            case "asset" -> { assertThat(args[0]).isEqualTo("food-201"); yield asset; }
            case "request" -> null;
            case "customFoods" -> List.of();
            case "saveCustom" -> { savedAsset.set((String) args[6]); yield "42"; }
            case "food" -> value;
            default -> throw new AssertionError(method.getName());
        });

        Food saved = new FoodDomainService(repository).saveCustom("owner-a", "Soup", null, nutrition, "key", "food-201");

        assertThat(saved.id()).isEqualTo("42");
        assertThat(savedAsset).hasValue("food-201");
    }

    @Test void customSaveRejectsUnknownArtworkMapping() {
        Nutrients nutrition = new Nutrients(new BigDecimal("100"), null, null, null);
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
            case "lockOwner" -> null;
            case "asset" -> null;
            default -> throw new AssertionError(method.getName());
        });

        assertThatThrownBy(() -> new FoodDomainService(repository).saveCustom("owner-a", "Soup", null, nutrition, "key", "missing"))
                .hasMessageContaining("artwork mapping was not found");
    }

    @Test void customFoodImageMustBePngAndImageDigestParticipatesInIdempotency() {
        byte[] pngA = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
        byte[] pngB = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 2};
        String imageA = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(pngA);
        String imageB = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(pngB);
        AtomicReference<StoredRequest> request = new AtomicReference<>();
        AtomicReference<byte[]> persisted = new AtomicReference<>();
        Nutrients nutrition = new Nutrients(new BigDecimal("100"), null, null, null);
        Food value = new Food("42", "custom", "Soup", null, nutrition, "Owner entered", null, null, false, null, "{}", null);
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "request" -> request.get();
                    case "customFoods" -> List.of();
                    case "saveCustom" -> {
                        persisted.set((byte[]) args[7]);
                        request.set(new StoredRequest("42", (String) args[5], false));
                        yield "42";
                    }
                    case "food" -> value;
                    default -> throw new AssertionError(method.getName());
                });
        FoodDomainService service = new FoodDomainService(repository);

        service.saveCustom("owner-a", "Soup", null, nutrition, "key", null, imageA);

        assertThat(persisted.get()).containsExactly(pngA);
        assertThatThrownBy(() -> service.saveCustom("owner-a", "Soup", null, nutrition, "key", null, imageB))
                .hasMessageContaining("different request");
        assertThatThrownBy(() -> service.saveCustom("owner-a", "Soup", null, nutrition, "bad", null,
                "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(new byte[16])))
                .hasMessageContaining("must be a PNG");
    }

    @Test void customFoodImageRejectsOversizedPng() {
        byte[] oversized = new byte[8 * 1024 * 1024 + 1];
        byte[] signature = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(signature, 0, oversized, 0, signature.length);
        String dataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(oversized);
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    default -> throw new AssertionError(method.getName());
                });

        assertThatThrownBy(() -> new FoodDomainService(repository).saveCustom("owner-a", "Soup", null,
                new Nutrients(new BigDecimal("100"), null, null, null), "key", null, dataUrl))
                .hasMessageContaining("no larger than 8 MB");
    }

    @Test void loggingAFridgeFoodPreservesTheResolvedArtworkInTheDiarySnapshot() {
        LocalDate date = LocalDate.parse("2026-10-21");
        DayState state = new DayState("day-1", date, "open", "Asia/Shanghai", Nutrients.unknown());
        Nutrients nutrition = new Nutrients(new BigDecimal("120"), new BigDecimal("25"),
                new BigDecimal("2.5"), BigDecimal.ZERO);
        Food food = new Food("1234", "catalog", "Oats", null, nutrition, "Measured foods",
                "CC0", "unrestricted", true, null,
                "{\"assetKey\":\"food-201\",\"assetUrl\":\"/mobile/fridge/foods/food-201.webp\"}",
                "food-201", "/mobile/fridge/foods/food-201.webp");
        AtomicReference<String> savedSnapshot = new AtomicReference<>();
        AtomicReference<Item> savedItem = new AtomicReference<>();
        AtomicInteger requestCount = new AtomicInteger();
        FoodRepository repository = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{FoodRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "request" -> requestCount.incrementAndGet() == 1
                            ? null : new StoredRequest("meal-1", "fingerprint", false);
                    case "day" -> state;
                    case "items" -> List.of();
                    case "food" -> food;
                    case "insertItem" -> {
                        savedItem.set((Item) args[2]);
                        savedSnapshot.set((String) args[5]);
                        yield null;
                    }
                    default -> throw new AssertionError(method.getName());
                });
        MealSource source = new MealSource("fridge-7", "1234", "catalog", "Oats", null,
                new BigDecimal("40"), "g", new BigDecimal("40"), nutrition, List.of());
        EntryCommand command = new EntryCommand("request-key", "Asia/Shanghai", "breakfast",
                "catalog", "1234", null, null, null, null, null);

        new FoodDomainService(repository).addFromFridge("owner-a", date, command, source);

        assertThat(savedItem.get().assetKey()).isEqualTo("food-201");
        assertThat(savedItem.get().assetUrl()).isEqualTo("/mobile/fridge/foods/food-201.webp");
        assertThat(savedSnapshot.get()).contains("\"fridgeItemId\":\"fridge-7\"")
                .contains("\"assetKey\":\"food-201\"")
                .contains("\"assetUrl\":\"/mobile/fridge/foods/food-201.webp\"");
    }
}
