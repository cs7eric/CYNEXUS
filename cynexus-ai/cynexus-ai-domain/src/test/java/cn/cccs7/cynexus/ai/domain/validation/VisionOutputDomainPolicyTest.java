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
}
