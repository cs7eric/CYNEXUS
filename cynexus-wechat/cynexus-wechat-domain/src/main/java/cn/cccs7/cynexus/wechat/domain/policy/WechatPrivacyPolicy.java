package cn.cccs7.cynexus.wechat.domain.policy;

public final class WechatPrivacyPolicy {
    private WechatPrivacyPolicy() {
    }

    public static String maskIdentifier(String value) {
        if (value == null || value.isBlank()) return "-";
        if (value.length() <= 6) return "***";
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }
}
