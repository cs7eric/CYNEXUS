package cn.cccs7.cynexus.wechat.domain.service;

import cn.cccs7.cynexus.wechat.domain.gateway.WechatVisionGateway;
import cn.cccs7.cynexus.wechat.domain.model.WechatImageRoute;
import cn.cccs7.cynexus.wechat.domain.model.WechatStoredImage;
import cn.cccs7.cynexus.wechat.domain.model.WechatVisionResult;
import cn.cccs7.cynexus.wechat.domain.policy.WechatImageRouteCatalog;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class WechatImageRoutingDomainService {
    private final WechatVisionGateway visionGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatImageRoutingDomainService(WechatVisionGateway visionGateway) {
        this.visionGateway = Objects.requireNonNull(visionGateway, "visionGateway");
    }

    public WechatImageRoute classify(WechatStoredImage image) {
        WechatVisionResult result = visionGateway.classify(image);
        if (result == null || result.status() == null || !"SUCCEEDED".equalsIgnoreCase(result.status())) {
            return WechatImageRoute.unknown("微信图片分类 AI 执行失败");
        }
        try {
            JsonNode node = objectMapper.readTree(result.content());
            String route = text(node, "route");
            if (!WechatImageRouteCatalog.isKnown(route)) {
                return WechatImageRoute.unknown("AI 返回了未注册的图片路由");
            }
            return new WechatImageRoute(
                    route,
                    decimal(node, "confidence"),
                    text(node, "reason"),
                    strings(node, "signals"),
                    strings(node, "warnings"));
        } catch (Exception exception) {
            return WechatImageRoute.unknown("微信图片分类结果不是合法 JSON");
        }
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node == null ? null : node.get(name);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private static BigDecimal decimal(JsonNode node, String name) {
        JsonNode value = node == null ? null : node.get(name);
        if (value == null || !value.isNumber()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(value.asDouble()).max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private static List<String> strings(JsonNode node, String name) {
        JsonNode values = node == null ? null : node.get(name);
        if (values == null || !values.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : values) {
            String value = item.asText("").trim();
            if (!value.isBlank()) result.add(value);
        }
        return List.copyOf(result);
    }
}
