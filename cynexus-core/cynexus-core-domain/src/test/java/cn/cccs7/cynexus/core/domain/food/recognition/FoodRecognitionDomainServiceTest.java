package cn.cccs7.cynexus.core.domain.food.recognition;

import cn.cccs7.cynexus.core.domain.food.FoodDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingResult;
import cn.cccs7.cynexus.core.domain.food.FoodModels.*;
import cn.cccs7.cynexus.core.domain.food.FoodRepository;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FoodRecognitionDomainServiceTest {
    private static final Identity IDENTITY = new Identity("6:tenant4:user", "tenant", "user");
    private static final String COMPLETE = """
            {"recognized":true,"foodName":"Oats","brand":"Grain Co","basis":"per_serving","servingSizeGrams":25,
             "confidence":"high","nutrition":{"caloriesKcal":95,"carbsG":16,"proteinG":3,"fatG":2}}
            """;

    @Test void onlyNutritionPhotoWithMissingNutritionCanBeQueued() {
        assertThatThrownBy(() -> FoodRecognitionPolicy.validate(new Command("key", image("food"), null, null, null, null, null)))
                .hasMessageContaining("nutrition label image is required");
        assertThatThrownBy(() -> FoodRecognitionPolicy.validate(new Command("key", null, image("label"), null, null, null,
                new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))))
                .hasMessageContaining("already complete");
        assertThatThrownBy(() -> FoodRecognitionPolicy.validate(new Command("key", null,
                new Image("label", "image/jpeg", "data:image/jpeg;base64,bad!"), null, null, null, null)))
                .hasMessageContaining("Invalid image");
    }

    @Test void manualValuesOverrideRecognitionAndServingValuesBecomePer100g() {
        Command input = FoodRecognitionPolicy.validate(new Command("key", null, image("label"), "My oats", "My brand", null,
                new Nutrients(null, null, new BigDecimal("20"), null)));
        Prepared value = FoodRecognitionPolicy.parse(COMPLETE, input);
        assertThat(value.name()).isEqualTo("My oats");
        assertThat(value.brand()).isEqualTo("My brand");
        assertThat(value.nutrients().caloriesKcal()).isEqualByComparingTo("380");
        assertThat(value.nutrients().carbsG()).isEqualByComparingTo("64");
        assertThat(value.nutrients().proteinG()).isEqualByComparingTo("20");
        assertThat(value.nutrients().fatG()).isEqualByComparingTo("8");
    }

    @Test void incompleteOrUncertainResultsNeverAutomaticallyCreateFood() {
        Command command = command();
        assertThatThrownBy(() -> FoodRecognitionPolicy.parse(COMPLETE.replace("\"fatG\":2", "\"fatG\":null"), command))
                .hasMessageContaining("all four");
        assertThatThrownBy(() -> FoodRecognitionPolicy.parse(COMPLETE.replace("per_serving", "unknown"), command))
                .hasMessageContaining("basis is unclear");
        assertThatThrownBy(() -> FoodRecognitionPolicy.parse(COMPLETE.replace("\"servingSizeGrams\":25", "\"servingSizeGrams\":null"), command))
                .hasMessageContaining("serving size");
        assertThatThrownBy(() -> FoodRecognitionPolicy.parse(COMPLETE.replace("Oats", "nutrition-label.jpg"), command))
                .hasMessageContaining("name could not");
        assertThatThrownBy(() -> FoodRecognitionPolicy.parse(COMPLETE.replace("\"recognized\":true", "\"recognized\":false"), command))
                .hasMessageContaining("could not be recognized");
        assertThatThrownBy(() -> FoodRecognitionPolicy.parse(COMPLETE.replace("\"high\"", "\"low\""), command))
                .hasMessageContaining("confidence is low");
    }

    @Test void submitsBothOriginalsBeforeMattingAndCommitsValidatedFoodOnlyAfterCompletion() {
        MemoryRepository repository = new MemoryRepository();
        List<String> events = new ArrayList<>();
        AtomicInteger writes = new AtomicInteger();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
        FoodRecognitionAiPort ai = new FoodRecognitionAiPort() {
            @Override public String enqueue(Identity identity, String key, List<Image> images) {
                assertThat(identity).isEqualTo(IDENTITY);
                assertThat(key).isEqualTo("food-recognition:1:0");
                assertThat(images).containsExactly(command().foodImage(), command().nutritionLabelImage());
                events.add("ai");
                return "123";
            }
            @Override public Snapshot get(Identity identity, String id) { return new Snapshot("succeeded", COMPLETE, null); }
        };
        FoodRepository foods = (FoodRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{FoodRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "lockOwner" -> null;
                    case "request" -> null;
                    case "customFoods" -> List.of();
                    case "saveCustom" -> {
                        assertThat(args[0]).isEqualTo(IDENTITY.owner());
                        assertThat(args[4]).isEqualTo("food-recognition:1");
                        assertThat((byte[]) args[7]).containsExactly(png);
                        writes.incrementAndGet();
                        events.add("save");
                        yield "42";
                    }
                    case "food" -> new Food("42", "custom", "Oats", "Grain Co", Nutrients.unknown(), null, null, null, true, null, "{}", null);
                    default -> throw new AssertionError(method.getName());
                });
        FoodRecognitionDomainService service = new FoodRecognitionDomainService(repository, ai,
                new FoodImageMattingDomainService((bytes, fileName, contentType) -> {
                    assertThat(bytes).containsExactly(FoodRecognitionPolicy.imageBytes(command().foodImage()));
                    events.add("matte");
                    return new FoodImageMattingResult(png, "image/png");
                }), new FoodDomainService(foods));

        service.enqueue(IDENTITY, command());
        assertThat(service.advance(repository.current)).isFalse();
        assertThat(events).containsExactly("ai");
        assertThat(writes).hasValue(0);
        assertThat(service.advance(repository.current)).isTrue();
        assertThat(events).containsExactly("ai", "matte");
        service.complete(repository.current);
        assertThat(events).containsExactly("ai", "matte", "save");
        assertThat(repository.savedFoodId).isEqualTo("42");
        repository.current = null;
        service.complete(task("save", "123", null, 0));
        assertThat(writes).hasValue(1);
    }

    @Test void idempotentEnqueueRejectsDifferentDraftAndTransientFailuresHaveBoundedRetries() {
        MemoryRepository repository = new MemoryRepository();
        FoodRecognitionDomainService service = new FoodRecognitionDomainService(repository, null, null, null);
        assertThat(service.enqueue(IDENTITY, command()).id()).isEqualTo(1);
        assertThat(service.enqueue(IDENTITY, command()).id()).isEqualTo(1);
        assertThatThrownBy(() -> service.enqueue(IDENTITY, new Command("key", image("food"), image("label"), "Different", null, null, null)))
                .hasMessageContaining("idempotency key");
        service.fail(task("poll", "123", null, 0), new IllegalStateException("network"));
        assertThat(repository.terminalFailure).isFalse();
        service.fail(task("poll", "123", null, 4), new IllegalStateException("network"));
        assertThat(repository.terminalFailure).isTrue();
        service.fail(task("poll", "123", null, 0), new IllegalArgumentException("invalid label"));
        assertThat(repository.terminalFailure).isTrue();
    }

    @Test void failedInputExpiresAfterSevenDaysAndCleanupIsAvailableToTheWorker() {
        MemoryRepository repository = new MemoryRepository();
        FoodRecognitionDomainService service = new FoodRecognitionDomainService(repository, null, null, null);
        Task template = task("poll", "123", null, 0);
        Task recent = new Task(template.id(), template.identity(), template.requestFingerprint(), template.command(),
                "failed", template.phase(), template.aiTaskId(), null, 1, 0, null, null, null, "failed", template.createdAt(),
                template.cycleStartedAt(), template.updatedAt(), OffsetDateTime.now().minusDays(6));
        Task expired = new Task(recent.id(), recent.identity(), recent.requestFingerprint(), recent.command(), recent.status(),
                recent.phase(), recent.aiTaskId(), null, 1, 0, null, null, null, "failed", recent.createdAt(),
                recent.cycleStartedAt(), recent.updatedAt(), OffsetDateTime.now().minusDays(8));
        assertThat(service.retryable(recent)).isTrue();
        assertThat(service.retryable(expired)).isFalse();
        assertThat(service.purgeExpiredInputs()).isEqualTo(1);
    }

    private static Image image(String name) { return new Image(name + ".jpg", "image/jpeg", "data:image/jpeg;base64,AQID"); }
    private static Command command() { return FoodRecognitionPolicy.validate(new Command("key", image("food"), image("label"), null, null, null, null)); }
    private static Task task(String phase, String aiId, Prepared prepared, int failures) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Task(1, IDENTITY, FoodRecognitionPolicy.fingerprint(command()), command(), "running", phase, aiId, prepared,
                failures, 0, "claim", null, null, null, now, now, now, null);
    }

    private static final class MemoryRepository implements FoodRecognitionRepository {
        private Task current;
        private boolean terminalFailure;
        private String savedFoodId;
        @Override public Task create(Identity identity, Command command, String fingerprint) {
            if (current == null) current = task("submit", null, null, 0);
            return current;
        }
        @Override public Task find(long id, String owner) { return current; }
        @Override public List<Task> list(String owner, int limit) { return current == null ? List.of() : List.of(current); }
        @Override public Task retry(long id, String owner) { return current; }
        @Override public int purgeExpiredInputs() { return 1; }
        @Override public Task claimNext() { return current; }
        @Override public void awaitAi(Task task, String aiId) { current = task("poll", aiId, null, 0); }
        @Override public boolean prepare(Task task, Prepared prepared) { current = task("save", task.aiTaskId(), prepared, 0); return true; }
        @Override public Task lockForCompletion(Task task) { return current; }
        @Override public void succeed(Task task, String foodId, String foodName) { savedFoodId = foodId; }
        @Override public void fail(Task task, String error, boolean terminal, int retryDelaySeconds) { terminalFailure = terminal; }
    }
}
