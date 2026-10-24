package cn.cccs7.cynexus.ai.domain.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

class VisionInputDomainPolicyTest {
    @Test
    void nutritionLabelAcceptsLabelWithOptionalFoodImageFirst() {
        assertDoesNotThrow(() -> VisionInputDomainPolicy.validate("food_nutrition_label_parse",
                java.util.List.of(image("image/jpeg", "data:image/jpeg;base64,AA=="))));
        assertDoesNotThrow(() -> VisionInputDomainPolicy.validate("food_nutrition_label_parse",
                java.util.List.of(image("image/jpeg", "data:image/jpeg;base64,AA=="),
                        image("image/png", "data:image/png;base64,AA=="))));
    }

    @Test
    void nutritionLabelRejectsMissingTooManyUnsupportedAndOversizedImages() {
        assertThrows(BizException.class, () -> VisionInputDomainPolicy.validate("food_nutrition_label_parse", java.util.List.of()));
        assertThrows(BizException.class, () -> VisionInputDomainPolicy.validate("food_nutrition_label_parse",
                java.util.List.of(image("image/jpeg", "data:image/jpeg;base64,AA=="),
                        image("image/jpeg", "data:image/jpeg;base64,AA=="),
                        image("image/jpeg", "data:image/jpeg;base64,AA=="))));
        assertThrows(BizException.class, () -> VisionInputDomainPolicy.validate("food_nutrition_label_parse",
                java.util.List.of(image("image/gif", "data:image/gif;base64,AA=="))));
        assertThrows(BizException.class, () -> VisionInputDomainPolicy.validate("food_nutrition_label_parse",
                java.util.List.of(image("image/jpeg", "data:image/png;base64,AA=="))));
        assertThrows(BizException.class, () -> VisionInputDomainPolicy.validate("food_nutrition_label_parse",
                java.util.List.of(image("image/jpeg", "data:image/jpeg;base64," + "A".repeat(3_600_001)))));
    }

    @Test
    void otherVisionCapabilitiesKeepTheirExistingImageRules() {
        assertDoesNotThrow(() -> VisionInputDomainPolicy.validate("accounting_receipt_parse", java.util.List.of()));
    }

    private AiImageInputBO image(String contentType, String dataUrl) {
        return AiImageInputBO.builder().contentType(contentType).dataUrl(dataUrl).build();
    }
}
