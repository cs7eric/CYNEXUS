package cn.cccs7.cynexus.core.domain.food;

import java.util.List;

/** Gateway for the bounded, preview-only fridge inventory extraction capability. */
public interface FridgeInventoryAiClient {
    Completion preview(String input, String inputMode);

    default Completion preview(String input, String inputMode, String tenant, String principal) {
        return preview(input, inputMode);
    }

    default Completion matchNutrition(String input, String tenant, String principal) {
        throw new IllegalStateException("Fridge nutrition matching is unavailable");
    }


    record Completion(String content, String providerCode, String modelCode) { }
}
