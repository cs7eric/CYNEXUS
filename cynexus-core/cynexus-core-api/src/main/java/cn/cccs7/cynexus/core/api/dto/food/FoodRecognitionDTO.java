package cn.cccs7.cynexus.core.api.dto.food;

import java.time.OffsetDateTime;

public final class FoodRecognitionDTO {
    private FoodRecognitionDTO() { }
    public record ImageInput(String fileName, String contentType, String dataUrl) { }
    public record Request(String idempotencyKey, ImageInput foodImage, ImageInput nutritionLabelImage,
                          String name, String brand, String assetKey, FoodDTO.Nutrients nutrients) { }
    public record Task(String id, String processId, boolean accepted, String status, String phase,
                       String workflowType, String workflowKey, String pollUrl, String aiTaskId,
                       String foodId, String foodName, String errorMessage, boolean retryable, int attemptCount,
                       int retryCount, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt) { }
}
