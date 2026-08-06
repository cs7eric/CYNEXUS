package cn.cccs7.cynexus.wechat.domain.policy;

import java.util.Map;

public final class WechatImageRouteCatalog {
    public static final String UNKNOWN = "UNKNOWN";
    private static final Map<String, String> CAPABILITIES = Map.of(
            "ACCOUNTING_RECEIPT", "accounting_receipt_parse",
            "PORTFOLIO_TRADE", "portfolio_trade_screenshot_parse");

    private WechatImageRouteCatalog() {
    }

    public static boolean isKnown(String route) {
        return UNKNOWN.equals(route) || CAPABILITIES.containsKey(route);
    }

    public static String capability(String route) {
        return CAPABILITIES.get(route);
    }
}
