package cn.cccs7.cynexus.wechat.infra.client;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.common.cache.redis.RedisCacheOperations;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatMessageGateway;
import cn.cccs7.cynexus.wechat.domain.gateway.WechatAccessTokenProvider;
import cn.cccs7.cynexus.wechat.domain.model.WechatPushFailure;
import cn.cccs7.cynexus.wechat.domain.model.WechatPushResult;
import cn.cccs7.cynexus.wechat.infra.config.WechatProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class WechatOfficialAccountClient implements WechatMessageGateway, WechatAccessTokenProvider {
    private static final String TOKEN_KEY_PREFIX = "cynexus:wechat:access-token:";
    private final WechatProperties properties;
    private final RedisCacheOperations redis;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final Duration timeout;

    public WechatOfficialAccountClient(WechatProperties properties, RedisCacheOperations redis) {
        this.properties = properties;
        this.redis = redis;
        this.timeout = Duration.ofMillis(Math.max(properties.getTimeoutMs(), 1000));
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public WechatPushResult pushText(List<String> openIds, String content) {
        String accessToken = accessToken();
        List<WechatPushFailure> failures = new ArrayList<>();
        int sent = 0;
        for (String openId : openIds) {
            try {
                sendText(accessToken, openId, content);
                sent++;
            } catch (Exception exception) {
                failures.add(new WechatPushFailure(openId, exception.getMessage()));
            }
        }
        return new WechatPushResult(openIds.size(), sent, List.copyOf(failures));
    }

    @Override
    public String accessToken() {
        requireConfigured();
        String cacheKey = TOKEN_KEY_PREFIX + properties.getAppId();
        String cached = redis.get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        URI uri = URI.create(apiBaseUrl() + "/cgi-bin/token?grant_type=client_credential&appid="
                + encode(properties.getAppId()) + "&secret=" + encode(properties.getAppSecret()));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
            Object token = body.get("access_token");
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !(token instanceof String value)) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "WeChat access token request failed: " + body.get("errmsg"));
            }
            redis.set(cacheKey, value, Math.max(properties.getAccessTokenCacheSeconds(), 60), TimeUnit.SECONDS);
            return value;
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "WeChat access token request failed: " + exception.getMessage());
        }
    }

    private void sendText(String accessToken, String openId, String content) throws Exception {
        URI uri = URI.create(apiBaseUrl() + "/cgi-bin/message/custom/send?access_token=" + encode(accessToken));
        Map<String, Object> payload = Map.of("touser", openId, "msgtype", "text", "text", Map.of("content", content));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
        Object errorCode = body.get("errcode");
        if (response.statusCode() < 200 || response.statusCode() >= 300
                || (errorCode instanceof Number number && number.intValue() != 0)) {
            throw new IllegalStateException("WeChat push failed: " + body.get("errmsg"));
        }
    }

    private void requireConfigured() {
        if (properties.getAppId().isBlank() || properties.getAppSecret().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "WeChat appId and appSecret are not configured");
        }
    }

    private String apiBaseUrl() {
        return properties.getApiBaseUrl().replaceAll("/+$", "");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
