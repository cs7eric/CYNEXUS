package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.api.dto.food.FoodRecognitionDTO;
import cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionDomainService;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FoodRecognitionAppService {
    private final FoodRecognitionDomainService domain;

    public FoodRecognitionAppService(FoodRecognitionDomainService domain) { this.domain = domain; }

    public FoodRecognitionDTO.Task enqueue(FoodIdentityResolver.Identity identity, FoodRecognitionDTO.Request request) {
        var nutrients = request == null ? null : request.nutrients();
        var command = request == null ? null : new FoodRecognitionModels.Command(request.idempotencyKey(), image(request.foodImage()),
                image(request.nutritionLabelImage()), request.name(), request.brand(), request.assetKey(), nutrients == null ? null
                : new Nutrients(nutrients.caloriesKcal(), nutrients.carbsG(), nutrients.proteinG(), nutrients.fatG()));
        return dto(domain.enqueue(new FoodRecognitionModels.Identity(identity.owner(), identity.tenant(), identity.principal()), command));
    }

    public FoodRecognitionDTO.Task get(long id, String owner) { return dto(domain.get(id, owner)); }
    public List<FoodRecognitionDTO.Task> list(String owner, int limit) { return domain.list(owner, limit).stream().map(this::dto).toList(); }
    public FoodRecognitionDTO.Task retry(long id, String owner) { return dto(domain.retry(id, owner)); }

    private FoodRecognitionModels.Image image(FoodRecognitionDTO.ImageInput value) {
        return value == null ? null : new FoodRecognitionModels.Image(value.fileName(), value.contentType(), value.dataUrl());
    }

    private FoodRecognitionDTO.Task dto(FoodRecognitionModels.Task task) {
        if (task == null) return null;
        String id = Long.toString(task.id());
        return new FoodRecognitionDTO.Task(id, id, true, task.status(), task.phase(), "FOOD_RECOGNITION",
                "food.nutrition-recognition", "/api/core/food/recognition-tasks/" + id, task.aiTaskId(),
                task.foodId(), task.foodName(), task.errorMessage(), domain.retryable(task),
                task.failureCount(), task.retryCount(), task.createdAt(), task.updatedAt(), task.completedAt());
    }
}
