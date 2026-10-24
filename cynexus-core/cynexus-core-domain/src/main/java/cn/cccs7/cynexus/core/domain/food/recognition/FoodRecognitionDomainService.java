package cn.cccs7.cynexus.core.domain.food.recognition;

import cn.cccs7.cynexus.core.domain.food.FoodDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingDomainService;
import cn.cccs7.cynexus.core.domain.food.FoodImageMattingResult;
import cn.cccs7.cynexus.core.domain.food.FoodModels.Food;
import cn.cccs7.cynexus.core.domain.food.FoodPolicy;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.*;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;

/** Durable, user-submitted workflow; image uploads and writes are excluded from autonomous Agent tools. */
@Service
public class FoodRecognitionDomainService {
    private final FoodRecognitionRepository repository;
    private final FoodRecognitionAiPort ai;
    private final FoodImageMattingDomainService matting;
    private final FoodDomainService foods;

    public FoodRecognitionDomainService(FoodRecognitionRepository repository, FoodRecognitionAiPort ai,
                                        FoodImageMattingDomainService matting, FoodDomainService foods) {
        this.repository = repository;
        this.ai = ai;
        this.matting = matting;
        this.foods = foods;
    }

    public Task enqueue(Identity identity, Command request) {
        FoodPolicy.require(identity != null && identity.owner() != null && !identity.owner().isBlank()
                && identity.tenant() != null && !identity.tenant().isBlank()
                && identity.principal() != null && !identity.principal().isBlank(), "A trusted owner session is required");
        Command command = FoodRecognitionPolicy.validate(request);
        String fingerprint = FoodRecognitionPolicy.fingerprint(command);
        Task task = repository.create(identity, command, fingerprint);
        FoodPolicy.require(fingerprint.equals(task.requestFingerprint()), "The idempotency key was already used for another food");
        return task;
    }

    public Task get(long id, String owner) { return repository.find(id, owner); }
    public List<Task> list(String owner, int limit) { return repository.list(owner, Math.max(1, Math.min(limit, 100))); }
    public Task retry(long id, String owner) { return repository.retry(id, owner); }
    public Task claimNext() { return repository.claimNext(); }
    public int purgeExpiredInputs() { return repository.purgeExpiredInputs(); }

    public boolean retryable(Task task) {
        return task != null && "failed".equals(task.status()) && task.retryCount() < 20
                && task.completedAt() != null && task.completedAt().isAfter(OffsetDateTime.now().minusDays(7));
    }

    /** External processing precedes a separate transaction which commits the food and task together. */
    public boolean advance(Task task) {
        if ("save".equals(task.phase())) return true;
        FoodPolicy.require(task.cycleStartedAt().isAfter(OffsetDateTime.now().minusMinutes(30)), "Food recognition timed out; retry the task");
        if (task.aiTaskId() == null) {
            Command command = task.command();
            List<Image> images = command.foodImage() == null ? List.of(command.nutritionLabelImage())
                    : List.of(command.foodImage(), command.nutritionLabelImage());
            String aiId = ai.enqueue(task.identity(), "food-recognition:" + task.id() + ":" + task.retryCount(), images);
            repository.awaitAi(task, aiId);
            return false;
        }
        FoodRecognitionAiPort.Snapshot snapshot = ai.get(task.identity(), task.aiTaskId());
        String status = snapshot == null || snapshot.status() == null ? "" : snapshot.status().toLowerCase(java.util.Locale.ROOT);
        if ("queued".equals(status) || "running".equals(status)) {
            repository.awaitAi(task, task.aiTaskId());
            return false;
        }
        FoodPolicy.require("succeeded".equals(status), "failed".equals(status)
                ? "Nutrition analysis failed; retry the task" : "Nutrition analysis returned an invalid task status");
        Prepared parsed = FoodRecognitionPolicy.parse(snapshot.content(), task.command());
        String artwork = null;
        if (task.command().foodImage() != null) {
            Image source = task.command().foodImage();
            FoodImageMattingResult result = matting.matte(FoodRecognitionPolicy.imageBytes(source), source.fileName(), source.contentType());
            artwork = "data:image/png;base64," + Base64.getEncoder().encodeToString(result.content());
        }
        return repository.prepare(task, new Prepared(parsed.name(), parsed.brand(), parsed.assetKey(), parsed.nutrients(), artwork));
    }

    /** Called inside the application transaction; the row lock fences expired worker claims. */
    public void complete(Task claimed) {
        Task current = repository.lockForCompletion(claimed);
        if (current == null) return;
        Prepared prepared = current.prepared();
        FoodPolicy.require(prepared != null, "Recognized food details are missing");
        Food food = foods.saveCustom(current.identity().owner(), prepared.name(), prepared.brand(), prepared.nutrients(),
                "food-recognition:" + current.id(), prepared.assetKey(), prepared.assetImageDataUrl());
        repository.succeed(current, food.id(), food.name());
    }

    public void fail(Task task, RuntimeException failure) {
        boolean permanent = failure instanceof IllegalArgumentException
                || failure instanceof FoodRecognitionAiPort.Failure aiFailure && !aiFailure.retryable();
        String message = permanent ? failure.getMessage() : "Food recognition service is temporarily unavailable";
        repository.fail(task, message, permanent || task.failureCount() + 1 >= 5,
                Math.min(120, 5 << Math.min(task.failureCount(), 4)));
    }
}
