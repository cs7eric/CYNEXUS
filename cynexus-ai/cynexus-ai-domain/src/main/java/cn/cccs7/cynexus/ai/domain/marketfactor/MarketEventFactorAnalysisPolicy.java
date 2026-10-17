package cn.cccs7.cynexus.ai.domain.marketfactor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Validation boundary for model summaries of deterministic event-factor evidence. */
public final class MarketEventFactorAnalysisPolicy {
    private final AiStructuredOutputParser structuredOutputParser;

    public MarketEventFactorAnalysisPolicy() {
        this(AiStructuredOutputParser.shared());
    }

    public MarketEventFactorAnalysisPolicy(AiStructuredOutputParser structuredOutputParser) {
        this.structuredOutputParser = java.util.Objects.requireNonNull(structuredOutputParser, "structuredOutputParser is required");
    }
    private static final int DEFAULT_MAX_EVENTS = 12;
    private static final int MAX_EVENTS = 20;

    public int maxEvents(Integer requested) {
        return requested == null ? DEFAULT_MAX_EVENTS : Math.max(1, Math.min(MAX_EVENTS, requested));
    }

    public String cohort(String value) {
        String normalized = value == null || value.isBlank() ? "LIMIT_UP" : value.trim().toUpperCase(Locale.ROOT);
        if (!"LIMIT_UP".equals(normalized) && !"LARGE_MOVE".equals(normalized)) {
            throw new IllegalArgumentException("cohort must be LIMIT_UP or LARGE_MOVE");
        }
        return normalized;
    }

    public BigDecimal threshold(BigDecimal value) {
        if (value == null) return new BigDecimal("7");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("30")) > 0) {
            throw new IllegalArgumentException("minLargeMovePct must be between 0 and 30");
        }
        return value;
    }

    public ObjectNode parseAndValidate(String content, Set<String> knownRefs, Set<String> eventCodes, ObjectMapper mapper) {
        JsonNode root = parse(content, mapper);
        JsonNode source = root.path("factor_analysis");
        if (!source.isObject()) throw new IllegalArgumentException("AI factor analysis must contain factor_analysis");
        String summary = text(source.path("summary"), 900, "未形成可引用的因子观察摘要。");
        ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.put("summary", summary);
        ArrayNode hypotheses = value.putArray("hypotheses");
        JsonNode rawHypotheses = source.path("hypotheses");
        if (rawHypotheses.isArray()) {
            for (JsonNode node : rawHypotheses) {
                if (hypotheses.size() >= 8 || !node.isObject()) break;
                ArrayNode evidence = refs(node.path("evidence_refs"), knownRefs, 6, value.arrayNode());
                if (evidence.isEmpty()) continue;
                String scope = "EVENT".equalsIgnoreCase(text(node.path("scope"), 16, "COHORT")) ? "EVENT" : "COHORT";
                String eventCode = text(node.path("event_code"), 16, "");
                if ("EVENT".equals(scope) && !eventCodes.contains(eventCode)) continue;
                ObjectNode hypothesis = hypotheses.addObject();
                hypothesis.put("label", text(node.path("label"), 160, "可继续验证的因子组合"));
                hypothesis.put("scope", scope);
                if ("EVENT".equals(scope)) hypothesis.put("event_code", eventCode);
                hypothesis.set("evidence_refs", evidence);
                hypothesis.set("counter_evidence_refs", refs(node.path("counter_evidence_refs"), knownRefs, 5, value.arrayNode()));
                hypothesis.set("research_conditions", textList(node.path("research_conditions"), 4, 240, value.arrayNode()));
            }
        }
        value.set("caveats", textList(source.path("caveats"), 6, 320, value.arrayNode()));
        return value;
    }

    private JsonNode parse(String content, ObjectMapper mapper) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("AI factor analysis returned empty content");
        try {
            return mapper.readTree(structuredOutputParser.extractObject(content));
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI factor analysis returned invalid JSON", exception);
        }
    }

    private ArrayNode refs(JsonNode source, Set<String> knownRefs, int max, ArrayNode target) {
        if (!source.isArray()) return target;
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonNode item : source) {
            if (values.size() >= max || !item.isTextual()) break;
            String ref = item.asText().trim();
            if (knownRefs.contains(ref)) values.add(ref);
        }
        values.forEach(target::add);
        return target;
    }

    private ArrayNode textList(JsonNode source, int max, int length, ArrayNode target) {
        if (!source.isArray()) return target;
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode item : source) {
            if (values.size() >= max || !item.isTextual()) break;
            String value = text(item, length, "");
            if (!value.isEmpty()) values.add(value);
        }
        values.forEach(target::add);
        return target;
    }

    private String text(JsonNode node, int max, String fallback) {
        if (node == null || !node.isValueNode()) return fallback;
        String value = node.asText("").trim();
        if (value.isEmpty()) return fallback;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
