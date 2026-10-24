package cn.cccs7.cynexus.ai.domain.validation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VisionOutputDomainPolicyTest {
    @Test void receiptRequiresPositiveAmountsAndValidTransactionType() {
        assertTrue(VisionOutputDomainPolicy.isValid("accounting_receipt_parse",
                "{\"items\":[{\"imageIndex\":0,\"amount\":12.5,\"transactionType\":\"expense\"}]}"));
        assertFalse(VisionOutputDomainPolicy.isValid("accounting_receipt_parse",
                "{\"items\":[{\"imageIndex\":-1,\"amount\":0,\"transactionType\":\"other\"}]}"));
    }

    @Test void tradeOnlyAllowsKnownSidesAndInstrumentTypes() {
        assertTrue(VisionOutputDomainPolicy.isValid("portfolio_trade_screenshot_parse",
                "{\"candidates\":[{\"side\":\"BUY\",\"instrumentType\":\"STOCK\",\"rowNo\":1}]}"));
        assertFalse(VisionOutputDomainPolicy.isValid("portfolio_trade_screenshot_parse",
                "{\"candidates\":[{\"side\":\"HOLD\",\"instrumentType\":\"CRYPTO\",\"rowNo\":0}]}"));
    }

    @Test void reviewAndWechatRouteEnforceConfidenceBoundsAndUnknownRoute() {
        assertTrue(VisionOutputDomainPolicy.isValid("portfolio_trade_screenshot_review",
                "{\"consistent\":true,\"confidence\":0.8,\"issues\":[]}"));
        assertTrue(VisionOutputDomainPolicy.isValid("wechat_image_route",
                "{\"route\":\"UNKNOWN\",\"confidence\":0,\"warnings\":[]}"));
        assertFalse(VisionOutputDomainPolicy.isValid("wechat_image_route",
                "{\"route\":\"PORTFOLIO_TRADE\",\"confidence\":1.2}"));
    }

    @Test void nutritionLabelEnforcesStrictShapeAndUnknownValues() {
        String valid = "{\"recognized\":true,\"foodName\":\"Oats\",\"brand\":null,\"basis\":\"per_serving\",\"servingSizeGrams\":40,\"nutrition\":{\"caloriesKcal\":160,\"carbsG\":24,\"proteinG\":5,\"fatG\":4},\"confidence\":\"high\"}";
        assertTrue(VisionOutputDomainPolicy.isValid("food_nutrition_label_parse", valid));
        assertTrue(VisionOutputDomainPolicy.isValid("food_nutrition_label_parse",
                "{\"recognized\":false,\"foodName\":null,\"brand\":null,\"basis\":\"unknown\",\"servingSizeGrams\":null,\"nutrition\":{\"caloriesKcal\":null,\"carbsG\":null,\"proteinG\":null,\"fatG\":null},\"confidence\":\"low\"}"));
        assertFalse(VisionOutputDomainPolicy.isValid("food_nutrition_label_parse", valid.replace("\"confidence\":\"high\"", "\"confidence\":\"high\",\"extra\":true")));
        assertFalse(VisionOutputDomainPolicy.isValid("food_nutrition_label_parse",
                "{\"recognized\":false,\"foodName\":null,\"brand\":null,\"basis\":\"unknown\",\"servingSizeGrams\":null,\"nutrition\":{\"caloriesKcal\":100,\"carbsG\":null,\"proteinG\":null,\"fatG\":null},\"confidence\":\"low\"}"));
    }
}
