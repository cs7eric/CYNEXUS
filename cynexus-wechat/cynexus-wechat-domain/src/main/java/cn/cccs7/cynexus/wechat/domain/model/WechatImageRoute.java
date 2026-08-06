package cn.cccs7.cynexus.wechat.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record WechatImageRoute(
        String route,
        BigDecimal confidence,
        String reason,
        List<String> signals,
        List<String> warnings) {

    public WechatImageRoute {
        route = route == null || route.isBlank() ? "UNKNOWN" : route;
        confidence = confidence == null ? BigDecimal.ZERO : confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        reason = reason == null ? "" : reason;
        signals = signals == null ? List.of() : List.copyOf(signals);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static WechatImageRoute unknown(String warning) {
        return new WechatImageRoute("UNKNOWN", BigDecimal.ZERO, "", List.of(),
                warning == null || warning.isBlank() ? List.of() : List.of(warning));
    }
}
