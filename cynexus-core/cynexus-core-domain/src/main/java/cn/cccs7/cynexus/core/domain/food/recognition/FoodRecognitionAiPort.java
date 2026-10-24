package cn.cccs7.cynexus.core.domain.food.recognition;

import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Identity;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Image;
import java.util.List;

/** Uses the existing food_nutrition_label_parse capability and its managed prompt/binding. */
public interface FoodRecognitionAiPort {
    String enqueue(Identity identity, String idempotencyKey, List<Image> images);
    Snapshot get(Identity identity, String aiTaskId);
    record Snapshot(String status, String content, String errorMessage) { }

    class Failure extends RuntimeException {
        private final boolean retryable;
        public Failure(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
        public boolean retryable() { return retryable; }
    }
}
