package cn.cccs7.cynexus.ai.domain.marketinformation;

import cn.cccs7.cynexus.ai.domain.entity.MarketInformationAiTaskBO;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Domain protocol rules for market-information event and brief analysis. */
public final class MarketInformationAnalysisDomainPolicy {

    private static final String DISCLAIMER = "Information analysis only; not investment advice.";
    private static final Set<String> EVENT_TYPES = Set.of("policy", "macro", "industry", "company", "regulation", "market");
    private static final Set<String> DIRECTIONS = Set.of("positive", "negative", "mixed", "neutral", "unknown");
    private static final Set<String> MATERIALITIES = Set.of("high", "medium", "low", "unknown");
    private static final Set<String> HORIZONS = Set.of("intraday", "short", "medium", "long", "unknown");
    private static final Set<String> NOVELTIES = Set.of("new", "developing", "repeated");
    private static final Set<String> EXPECTATION_GAPS = Set.of("above", "in_line", "below", "unknown");
    private static final Set<String> CONTENT_QUALITIES = Set.of("full_text", "ocr", "mixed", "summary_only", "unknown");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiStructuredOutputParser structuredOutputParser;

    public MarketInformationAnalysisDomainPolicy() {
        this(AiStructuredOutputParser.shared());
    }

    public MarketInformationAnalysisDomainPolicy(AiStructuredOutputParser structuredOutputParser) {
        this.structuredOutputParser = java.util.Objects.requireNonNull(structuredOutputParser, "structuredOutputParser is required");
    }

    public JsonNode extractObject(String raw) throws Exception {
        JsonNode result = objectMapper.readTree(structuredOutputParser.extractObject(raw));
        if (!result.isObject()) {
            throw new IllegalArgumentException("Response must be a JSON object");
        }
        return result;
    }

    public JsonNode readJson(String raw) {
        try {
            return raw == null || raw.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    public JsonNode enrichEvent(JsonNode value, MarketInformationAiTaskBO task) {
        if (!(value instanceof ObjectNode event)) {
            return value;
        }
        Set<String> qualities = new HashSet<>();
        Set<String> direct = new LinkedHashSet<>();
        Set<String> inferred = new LinkedHashSet<>();
        if (task != null && task.getSources() != null) {
            task.getSources().forEach(source -> {
                if (source.getContentQuality() != null && !source.getContentQuality().isBlank()) {
                    qualities.add(source.getContentQuality().toLowerCase(Locale.ROOT));
                }
                for (String mapping : source.getDeterministicImpact() == null ? List.<String>of() : source.getDeterministicImpact()) {
                    if (mapping.endsWith("[Direct]")) {
                        direct.add(mapping.replace(" [Direct]", ""));
                    } else if (mapping.endsWith("[Inferred]")) {
                        inferred.add(mapping.replace(" [Inferred]", ""));
                    }
                }
            });
        }
        String quality = qualities.isEmpty() ? "unknown"
                : qualities.size() == 1 ? qualities.iterator().next()
                : qualities.stream().allMatch(item -> Set.of("full_text", "ocr").contains(item)) ? "full_text" : "mixed";
        event.put("contentQuality", CONTENT_QUALITIES.contains(quality) ? quality : "unknown");
        ArrayNode industries = event.putArray("deterministicImpactIndustries");
        direct.forEach(industries::add);
        inferred.stream().filter(item -> !direct.contains(item)).forEach(industries::add);
        event.put("deterministicImpactStatus", !direct.isEmpty() ? "Direct" : !inferred.isEmpty() ? "Inferred" : "Unmapped");
        return event;
    }

    public JsonNode validateEvent(JsonNode value, MarketInformationAiTaskBO task) {
        for (String field : List.of("summary", "eventFacts", "informationDelta", "whyItMatters", "transmissionPaths",
                "marketExpectationGap", "affectedScopes", "affectedIndustries", "affectedCompanies", "entities",
                "marketDirection", "materiality", "timeHorizon", "novelty", "catalysts", "scenarios", "watchItems",
                "confidence", "contentQuality", "evidence", "risks", "uncertainties", "disclaimer")) {
            if (!value.has(field)) throw new IllegalArgumentException("Event analysis missing field: " + field);
        }
        requireText(value, "summary");
        for (String field : List.of("eventFacts", "transmissionPaths", "affectedScopes", "affectedIndustries",
                "affectedCompanies", "entities", "catalysts", "watchItems", "risks", "uncertainties")) {
            requireStringArray(value, field);
        }
        requireText(value, "informationDelta");
        requireText(value, "whyItMatters");
        requireEnum(value, "marketExpectationGap", EXPECTATION_GAPS);
        requireEnum(value, "eventType", EVENT_TYPES);
        requireEnum(value, "marketDirection", DIRECTIONS);
        requireEnum(value, "materiality", MATERIALITIES);
        requireEnum(value, "timeHorizon", HORIZONS);
        requireEnum(value, "novelty", NOVELTIES);
        requireEnum(value, "contentQuality", CONTENT_QUALITIES);
        JsonNode scenarios = requireObject(value, "scenarios");
        for (String scenario : List.of("bull", "base", "bear")) requireText(scenarios, scenario);
        requireConfidence(value, "confidence");
        requireArray(value, "evidence");
        if (!DISCLAIMER.equals(value.path("disclaimer").asText())) throw new IllegalArgumentException("Invalid disclaimer");
        Map<String, String> sourceText = task == null || task.getSources() == null ? Map.of() : task.getSources().stream()
                .collect(Collectors.toMap(MarketInformationAiTaskBO.Source::getSourceId,
                        source -> normalize(source.getContent(), ""), (left, right) -> left));
        for (JsonNode evidence : value.path("evidence")) {
            requireText(evidence, "claim");
            requireText(evidence, "sourceId");
            requireText(evidence, "quote");
            String sourceId = evidence.path("sourceId").asText();
            String quote = evidence.path("quote").asText();
            if (quote.isBlank() || !sourceText.getOrDefault(sourceId, "").contains(quote)) {
                throw new IllegalArgumentException("Evidence quote is not an exact source substring: " + sourceId);
            }
        }
        return value;
    }

    public JsonNode enrichBrief(JsonNode value, JsonNode input) {
        if (!(value instanceof ObjectNode brief)) return value;
        brief.set("sourceCoverage", input.path("sourceCoverage").deepCopy());
        brief.set("analysisWindow", input.path("analysisWindow").deepCopy());
        Map<Long, JsonNode> inputEvents = new HashMap<>();
        for (JsonNode event : input.path("events")) inputEvents.put(event.path("clusterId").asLong(), event);
        Set<Long> references = new LinkedHashSet<>();
        for (JsonNode keyEvent : brief.path("keyEvents")) {
            long clusterId = keyEvent.path("clusterId").asLong(Long.MIN_VALUE);
            JsonNode sourceEvent = inputEvents.get(clusterId);
            if (!(keyEvent instanceof ObjectNode target) || sourceEvent == null) continue;
            JsonNode analysis = sourceEvent.path("analysis");
            target.put("title", sourceEvent.path("title").asText(""));
            target.put("eventType", analysis.path("eventType").asText("market"));
            target.put("direction", analysis.path("marketDirection").asText("unknown"));
            target.put("materiality", analysis.path("materiality").asText("unknown"));
            target.put("timeHorizon", analysis.path("timeHorizon").asText("unknown"));
            target.set("affectedScopes", analysis.path("affectedScopes").deepCopy());
            target.put("confidence", analysis.path("confidence").asDouble(0));
            references.add(clusterId);
        }
        for (String field : List.of("policySignals", "industrySignals", "companySignals", "riskSignals")) {
            for (JsonNode item : brief.path(field)) for (JsonNode reference : item.path("eventReferences")) {
                if (reference.canConvertToLong()) references.add(reference.asLong());
            }
        }
        ArrayNode eventReferences = objectMapper.createArrayNode();
        references.forEach(eventReferences::add);
        brief.set("eventReferences", eventReferences);
        return brief;
    }

    public JsonNode validateBrief(JsonNode value, JsonNode input) {
        for (String field : List.of("headline", "executiveSummary", "marketTone", "keyEvents", "policySignals",
                "industrySignals", "companySignals", "riskSignals", "uncertainties", "sourceCoverage",
                "analysisWindow", "eventReferences", "disclaimer")) {
            if (!value.has(field)) throw new IllegalArgumentException("Daily brief missing field: " + field);
        }
        requireText(value, "headline");
        requireText(value, "executiveSummary");
        JsonNode tone = requireObject(value, "marketTone");
        requireEnum(tone, "direction", DIRECTIONS);
        requireConfidence(tone, "confidence");
        requireText(tone, "rationale");
        Set<Long> availableIds = new HashSet<>();
        for (JsonNode event : input.path("events")) availableIds.add(event.path("clusterId").asLong());
        if (availableIds.isEmpty()) throw new IllegalArgumentException("Daily brief input has no events");
        Set<Long> usedIds = new LinkedHashSet<>();
        JsonNode keyEvents = requireArray(value, "keyEvents");
        if (keyEvents.isEmpty()) throw new IllegalArgumentException("Daily brief keyEvents must not be empty");
        for (JsonNode event : keyEvents) {
            usedIds.add(requireReference(event.path("clusterId"), availableIds, "keyEvents.clusterId"));
            requireText(event, "title"); requireText(event, "summary");
            requireEnum(event, "eventType", EVENT_TYPES); requireEnum(event, "direction", DIRECTIONS);
            requireEnum(event, "materiality", MATERIALITIES); requireEnum(event, "timeHorizon", HORIZONS);
            requireArray(event, "affectedScopes"); requireConfidence(event, "confidence");
        }
        for (String field : List.of("policySignals", "industrySignals", "companySignals")) {
            for (JsonNode signal : requireArray(value, field)) {
                requireText(signal, "summary"); requireEnum(signal, "direction", DIRECTIONS);
                requireEnum(signal, "materiality", MATERIALITIES); requireArray(signal, "affectedScopes");
                Set<Long> ids = new LinkedHashSet<>(); collectReferences(signal, "eventReferences", availableIds, ids); usedIds.addAll(ids);
            }
        }
        for (JsonNode risk : requireArray(value, "riskSignals")) {
            requireText(risk, "summary"); requireEnum(risk, "severity", Set.of("high", "medium", "low"));
            requireEnum(risk, "timeHorizon", HORIZONS); requireArray(risk, "affectedScopes");
            Set<Long> ids = new LinkedHashSet<>(); collectReferences(risk, "eventReferences", availableIds, ids); usedIds.addAll(ids);
        }
        requireArray(value, "uncertainties");
        Set<Long> declaredIds = new LinkedHashSet<>(); collectReferences(value, "eventReferences", availableIds, declaredIds);
        if (declaredIds.isEmpty() || !declaredIds.containsAll(usedIds)) throw new IllegalArgumentException("Daily brief eventReferences are incomplete");
        if (!value.path("sourceCoverage").equals(input.path("sourceCoverage"))) throw new IllegalArgumentException("Daily brief sourceCoverage must match the input exactly");
        if (!value.path("analysisWindow").equals(input.path("analysisWindow"))) throw new IllegalArgumentException("Daily brief analysisWindow must match the input exactly");
        if (!DISCLAIMER.equals(value.path("disclaimer").asText())) throw new IllegalArgumentException("Invalid daily brief disclaimer");
        return value;
    }

    public boolean isStale(String expectedHash, String input) {
        return expectedHash != null && !expectedHash.isBlank() && !expectedHash.equals(sha256(input));
    }

    public String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalize(value, "").getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash Market Information brief input", exception);
        }
    }

    public String normalize(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    public String truncate(String value, int max) {
        String text = value == null ? "" : value;
        return text.length() <= max ? text : text.substring(0, max);
    }

    private void requireEnum(JsonNode value, String field, Set<String> accepted) {
        if (!accepted.contains(value.path(field).asText())) throw new IllegalArgumentException("Invalid " + field);
    }

    private JsonNode requireObject(JsonNode value, String field) {
        JsonNode item = value.path(field); if (!item.isObject()) throw new IllegalArgumentException(field + " must be an object"); return item;
    }

    private JsonNode requireArray(JsonNode value, String field) {
        JsonNode item = value.path(field); if (!item.isArray()) throw new IllegalArgumentException(field + " must be an array"); return item;
    }

    private void requireStringArray(JsonNode value, String field) {
        for (JsonNode item : requireArray(value, field)) if (!item.isTextual() || item.asText().isBlank()) throw new IllegalArgumentException(field + " must contain only non-blank strings");
    }

    private void requireText(JsonNode value, String field) {
        if (!value.path(field).isTextual() || value.path(field).asText().isBlank()) throw new IllegalArgumentException(field + " must be a non-blank string");
    }

    private void requireConfidence(JsonNode value, String field) {
        JsonNode confidenceNode = value.path(field);
        if (!confidenceNode.isNumber()) throw new IllegalArgumentException(field + " must be between 0 and 1");
        double confidence = confidenceNode.asDouble();
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }

    private void collectReferences(JsonNode value, String field, Set<Long> availableIds, Set<Long> target) {
        for (JsonNode reference : requireArray(value, field)) {
            long clusterId = requireReference(reference, availableIds, field);
            if (!target.add(clusterId)) throw new IllegalArgumentException(field + " contains duplicate clusterId: " + clusterId);
        }
    }

    private long requireReference(JsonNode value, Set<Long> availableIds, String field) {
        if (!value.canConvertToLong() || !availableIds.contains(value.asLong())) throw new IllegalArgumentException(field + " references an unavailable clusterId");
        return value.asLong();
    }
}
