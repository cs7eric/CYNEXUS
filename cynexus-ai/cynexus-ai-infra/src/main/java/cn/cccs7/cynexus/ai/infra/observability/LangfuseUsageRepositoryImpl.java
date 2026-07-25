package cn.cccs7.cynexus.ai.infra.observability;

import cn.cccs7.cynexus.common.core.constant.CommonConstants;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageObservationBO;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageObservabilityRepository;
import cn.cccs7.cynexus.ai.domain.policy.security.AgentOutboundEndpointPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class LangfuseUsageRepositoryImpl implements AiUsageObservabilityRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(LangfuseUsageRepositoryImpl.class);
    private static final String DEFAULT_ALLOWED_ORIGINS = "http://127.0.0.1:33000,http://cynexus-langfuse-web:3000";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final boolean enabled;
    private final String baseUrl;
    private final String authorization;

    public LangfuseUsageRepositoryImpl(
            @Value("${cynexus.ai.langfuse.enabled:false}") boolean enabled,
            @Value("${cynexus.ai.langfuse.base-url:http://127.0.0.1:33000}") String baseUrl,
            @Value("${cynexus.ai.langfuse.public-key:}") String publicKey,
            @Value("${cynexus.ai.langfuse.secret-key:}") String secretKey,
            @Value("${cynexus.ai.security.langfuse-allowed-origins:}") String allowedOrigins) {
        boolean configured = enabled && !isBlank(publicKey) && !isBlank(secretKey);
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        try {
            String origins = isBlank(allowedOrigins) ? DEFAULT_ALLOWED_ORIGINS : allowedOrigins;
            AgentOutboundEndpointPolicy.requireAllowed(normalizedBaseUrl, origins);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Langfuse usage endpoint rejected by outbound allow-list: {}", exception.getMessage());
            configured = false;
        }
        this.enabled = configured;
        this.baseUrl = normalizedBaseUrl;
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(
                (safe(publicKey) + ":" + safe(secretKey)).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<AiUsageObservationBO> list(OffsetDateTime from, OffsetDateTime to, int limit) {
        if (!enabled) {
            LOGGER.warn("Langfuse usage is disabled or keys are missing");
            return List.of();
        }
        int requested = Math.max(1, Math.min(limit, 1000));
        int pageSize = Math.min(requested, 100);
        List<AiUsageObservationBO> result = new ArrayList<>(requested);
        Set<String> seenIds = new HashSet<>();
        for (int page = 1; result.size() < requested; page++) {
            JsonNode response = fetchPage(from, to, page, pageSize);
            List<AiUsageObservationBO> rows = parse(response);
            if (rows.isEmpty()) break;
            int before = result.size();
            for (AiUsageObservationBO row : rows) {
                if (row.getId() == null || seenIds.add(row.getId())) result.add(row);
            }
            if (result.size() == before) break;
            if (rows.size() < pageSize) break;
        }
        return result.size() > requested ? result.subList(0, requested) : result;
    }

    private JsonNode fetchPage(OffsetDateTime from, OffsetDateTime to, int page, int pageSize) {
        StringBuilder query = new StringBuilder("?limit=").append(pageSize).append("&page=").append(page);
        if (from != null) query.append("&fromTimestamp=").append(encode(from.toString()));
        if (to != null) query.append("&toTimestamp=").append(encode(to.toString()));
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(baseUrl + "/api/public/observations" + query).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3_000);
            connection.setReadTimeout(8_000);
            connection.setRequestProperty("Authorization", authorization);
            connection.setRequestProperty(CommonConstants.HTTP_HEADER_ACCEPT, CommonConstants.MEDIA_TYPE_APPLICATION_JSON);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOGGER.warn("Langfuse observations returned status {} from {} page {}", status, baseUrl, page);
                return objectMapper.createObjectNode();
            }
            try (var stream = connection.getInputStream()) {
                return objectMapper.readTree(stream);
            }
        } catch (IOException e) {
            LOGGER.warn("Langfuse observations unavailable at {} page {}: {}", baseUrl, page, e.getMessage());
            return objectMapper.createObjectNode();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private List<AiUsageObservationBO> parse(JsonNode root) {
        JsonNode rows = root.isArray() ? root : root.path("data");
        if (!rows.isArray()) rows = root.path("data").path("data");
        if (!rows.isArray()) return List.of();
        List<AiUsageObservationBO> result = new ArrayList<>();
        for (JsonNode row : rows) {
            result.add(AiUsageObservationBO.builder()
                    .id(text(row, "id"))
                    .name(firstText(row, "name", "metadata.capability"))
                    .providerCode(firstText(row, "metadata.providerCode", "metadata.provider_code", "provider"))
                    .modelCode(firstText(row, "model", "providedModelName", "metadata.modelCode"))
                    .status(status(row))
                    .inputTokens(integer(row, "usageDetails.input", "usage.input", "inputTokens", "promptTokens"))
                    .outputTokens(integer(row, "usageDetails.output", "usage.output", "outputTokens"))
                    .totalTokens(integer(row, "usageDetails.total", "usage.total", "totalTokens"))
                    .totalCost(decimalOrNull(row, "costDetails.total", "totalCost", "calculatedTotalCost"))
                    .startedAt(date(row, "startTime", "createdAt"))
                    .latencyMs(latency(row))
                    .statusMessage(text(row, "statusMessage"))
                    .build());
        }
        return result;
    }

    private String status(JsonNode row) {
        String value = firstText(row, "metadata.status", "level");
        return "ERROR".equalsIgnoreCase(value) || !text(row, "statusMessage").isBlank() ? "FAILED" : "SUCCEEDED";
    }

    private Long latency(JsonNode row) {
        JsonNode value = row.at("/latency");
        if (value.isNumber()) return Math.round(value.asDouble() * (value.asDouble() < 100 ? 1000 : 1));
        OffsetDateTime start = date(row, "startTime");
        OffsetDateTime end = date(row, "endTime");
        return start != null && end != null ? Math.max(0, Duration.between(start, end).toMillis()) : 0L;
    }

    private Integer integer(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node.at("/" + path.replace('.', '/'));
            if (value.isNumber()) return Math.max(0, value.asInt());
        }
        return 0;
    }

    private BigDecimal decimal(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node.at("/" + path.replace('.', '/'));
            if (value.isNumber()) return value.decimalValue();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal decimalOrNull(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node.at("/" + path.replace('.', '/'));
            if (value.isNumber()) return value.decimalValue();
        }
        return null;
    }

    private OffsetDateTime date(JsonNode node, String... paths) {
        String value = firstText(node, paths);
        if (value.isBlank()) return null;
        try { return OffsetDateTime.parse(value); } catch (Exception ignored) { return null; }
    }

    private String firstText(JsonNode node, String... paths) {
        for (String path : paths) {
            String value = text(node, path);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String text(JsonNode node, String path) {
        JsonNode value = node.at("/" + path.replace('.', '/'));
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static String normalizeBaseUrl(String value) {
        String result = isBlank(value) ? "http://127.0.0.1:33000" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
