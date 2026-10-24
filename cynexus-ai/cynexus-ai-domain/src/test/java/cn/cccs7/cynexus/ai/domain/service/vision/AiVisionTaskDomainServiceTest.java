package cn.cccs7.cynexus.ai.domain.service.vision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.entity.AiVisionTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AiVisionTaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AiVisionTaskDomainServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T00:00:00Z"), ZoneOffset.UTC);
    @Test
    void enqueueCreatesQueuedTaskWithBoundedImageSet() {
        MemoryRepository repository = new MemoryRepository();
        AiVisionTaskBO task = new AiVisionTaskDomainService(repository).enqueue(
                "food_nutrition_label_parse", null, null,
                List.of(AiVisionTaskBO.Image.builder().fileName("label.jpg").contentType("image/jpeg")
                        .dataUrl("data:image/jpeg;base64,abc").build()), "tenant", "principal", "request-1");

        assertThat(task.getStatus()).isEqualTo(AiVisionTaskDomainService.QUEUED);
        assertThat(task.getImages()).hasSize(1);
        assertThat(task.getIdempotencyKey()).isEqualTo("request-1");
    }

    @Test
    void enqueueRejectsMissingScenarioOrImages() {
        AiVisionTaskDomainService service = new AiVisionTaskDomainService(new MemoryRepository());
        assertThatThrownBy(() -> service.enqueue(" ", null, null, List.of(), "tenant", "principal", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.enqueue("food_nutrition_label_parse", null, null, List.of(), "tenant", "principal", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enqueueRejectsMoreThanTwoImages() {
        AiVisionTaskDomainService service = new AiVisionTaskDomainService(new MemoryRepository());
        List<AiVisionTaskBO.Image> images = List.of(
                AiVisionTaskBO.Image.builder().dataUrl("data:image/jpeg;base64,one").build(),
                AiVisionTaskBO.Image.builder().dataUrl("data:image/jpeg;base64,two").build(),
                AiVisionTaskBO.Image.builder().dataUrl("data:image/jpeg;base64,three").build());

        assertThatThrownBy(() -> service.enqueue("food_nutrition_label_parse", null, null, images,
                "tenant", "principal", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vision task requires one or two images");
    }

    @Test
    void enqueueAcceptsTwoImagesButRejectsAnImageWithoutData() {
        AiVisionTaskDomainService service = new AiVisionTaskDomainService(new MemoryRepository());
        AiVisionTaskBO.Image image = AiVisionTaskBO.Image.builder().dataUrl("data:image/jpeg;base64,one").build();

        assertThat(service.enqueue("food_nutrition_label_parse", null, null, List.of(image, image),
                "tenant", "principal", null).getImages()).hasSize(2);
        assertThatThrownBy(() -> service.enqueue("food_nutrition_label_parse", null, null,
                List.of(AiVisionTaskBO.Image.builder().build()), "tenant", "principal", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vision image data is required");
    }

    @Test
    void listClampsRequestedLimit() {
        MemoryRepository repository = new MemoryRepository();
        new AiVisionTaskDomainService(repository).list(500, "tenant", "principal");
        assertThat(repository.lastLimit).isEqualTo(100);
    }

    @Test
    void retryCreatesNewQueuedTaskOnlyForFailedOwnerTask() {
        MemoryRepository repository = new MemoryRepository();
        AiVisionTaskBO failed = AiVisionTaskBO.builder().id(1L).scenarioCode("food_nutrition_label_parse")
                .status(AiVisionTaskDomainService.FAILED).attemptCount(1).maxAttempts(2).images(List.of())
                .inputsAvailable(true).completedAt(OffsetDateTime.now(CLOCK).minusDays(1)).build();
        repository.tasks.add(failed);
        AiVisionTaskBO retried = new AiVisionTaskDomainService(repository, CLOCK).retry(1L, "tenant", "principal");

        assertThat(retried.getId()).isEqualTo(2L);
        assertThat(retried.getStatus()).isEqualTo(AiVisionTaskDomainService.QUEUED);
        assertThat(retried.getAttemptCount()).isZero();
        assertThatThrownBy(() -> new AiVisionTaskDomainService(repository).retry(2L, "tenant", "principal"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retryRejectsExpiredOrRemovedInputsAndCleanupUsesSameCutoff() {
        MemoryRepository repository = new MemoryRepository();
        AiVisionTaskDomainService service = new AiVisionTaskDomainService(repository, CLOCK);
        AiVisionTaskBO task = AiVisionTaskBO.builder().id(1L).status(AiVisionTaskDomainService.FAILED)
                .inputsAvailable(true).completedAt(OffsetDateTime.now(CLOCK).minusDays(7)).build();
        repository.tasks.add(task);

        assertThat(service.isRetryable(task)).isFalse();
        assertThatThrownBy(() -> service.retry(1L, "tenant", "principal")).isInstanceOf(IllegalStateException.class);
        task.setCompletedAt(OffsetDateTime.now(CLOCK).minusDays(7).plusNanos(1));
        assertThat(service.isRetryable(task)).isTrue();
        task.setInputsAvailable(false);
        assertThat(service.isRetryable(task)).isFalse();
        assertThatThrownBy(() -> service.retry(1L, "tenant", "principal")).isInstanceOf(IllegalStateException.class);
        service.purgeTerminalInputs();
        assertThat(repository.lastCutoff).isEqualTo(OffsetDateTime.now(CLOCK).minusDays(7));
    }

    @Test
    void successfulTaskDiscardsImagesBeforePersistence() {
        MemoryRepository repository = new MemoryRepository();
        AiVisionTaskBO task = AiVisionTaskBO.builder().inputsAvailable(true)
                .images(List.of(AiVisionTaskBO.Image.builder().dataUrl("data:image/jpeg;base64,one").build())).build();

        new AiVisionTaskDomainService(repository).succeed(task, "execution", "provider", "model", "result");

        assertThat(task.getImages()).isEmpty();
        assertThat(task.isInputsAvailable()).isFalse();
    }

    private static final class MemoryRepository implements AiVisionTaskRepository {
        private final AtomicLong sequence = new AtomicLong(1);
        private final List<AiVisionTaskBO> tasks = new ArrayList<>();
        private int lastLimit;
        private OffsetDateTime lastCutoff;

        @Override public AiVisionTaskBO create(AiVisionTaskBO task) {
            task.setId(sequence.getAndIncrement());
            tasks.add(task);
            return task;
        }
        @Override public AiVisionTaskBO find(Long id, String tenantKey, String principalKey) { return tasks.stream().filter(task -> task.getId().equals(id)).findFirst().orElse(null); }
        @Override public List<AiVisionTaskBO> listByOwner(int limit, String tenantKey, String principalKey) { lastLimit = limit; return List.of(); }
        @Override public AiVisionTaskBO retryFailed(Long id, String tenantKey, String principalKey, OffsetDateTime failedAfter) {
            AiVisionTaskBO original = find(id, tenantKey, principalKey);
            if (original == null || !AiVisionTaskDomainService.FAILED.equals(original.getStatus())) return null;
            AiVisionTaskBO retry = AiVisionTaskBO.builder().id(sequence.incrementAndGet()).scenarioCode(original.getScenarioCode())
                    .status(AiVisionTaskDomainService.QUEUED).progress(0).attemptCount(0).maxAttempts(original.getMaxAttempts())
                    .images(original.getImages()).build();
            tasks.add(retry);
            return retry;
        }
        @Override public int purgeTerminalInputs(OffsetDateTime failedBefore) { lastCutoff = failedBefore; return 0; }
        @Override public AiVisionTaskBO claimNext(String workerId) { return null; }
        @Override public void releaseClaim(AiVisionTaskBO task) { }
        @Override public int recoverStaleTasks(int timeoutMinutes) { return 0; }
        @Override public void markSucceeded(AiVisionTaskBO task) { }
        @Override public void markFailed(AiVisionTaskBO task, String errorMessage) { }
    }
}
