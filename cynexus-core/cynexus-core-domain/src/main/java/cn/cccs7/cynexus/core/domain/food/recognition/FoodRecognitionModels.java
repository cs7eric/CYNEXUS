package cn.cccs7.cynexus.core.domain.food.recognition;

import cn.cccs7.cynexus.core.domain.food.FoodModels.Nutrients;
import java.time.OffsetDateTime;

public final class FoodRecognitionModels {
    private FoodRecognitionModels() { }

    public record Identity(String owner, String tenant, String principal) { }
    public record Image(String fileName, String contentType, String dataUrl) { }
    public record Command(String idempotencyKey, Image foodImage, Image nutritionLabelImage,
                          String name, String brand, String assetKey, Nutrients nutrients) { }
    public record Prepared(String name, String brand, String assetKey, Nutrients nutrients,
                           String assetImageDataUrl) { }
    public record Task(long id, Identity identity, String requestFingerprint, Command command,
                       String status, String phase, String aiTaskId, Prepared prepared, int failureCount,
                       int retryCount, String claimToken, String foodId, String foodName, String errorMessage,
                       OffsetDateTime createdAt, OffsetDateTime cycleStartedAt, OffsetDateTime updatedAt,
                       OffsetDateTime completedAt) { }
}
