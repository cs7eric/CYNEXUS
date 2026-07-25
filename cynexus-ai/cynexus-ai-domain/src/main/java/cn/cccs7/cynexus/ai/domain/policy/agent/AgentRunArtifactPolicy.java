package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds plan/evidence/warning JSON artifacts for an Agent run. */
public final class AgentRunArtifactPolicy {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentRunArtifactPolicy() {
    }

    public static String planJson(List<AgentRunStepBO> steps) {
        List<Map<String, Object>> plan = safeSteps(steps).stream()
                .filter(step -> "TOOL".equals(step.getKind()))
                .map(step -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("toolCode", valueOr(step.getToolCode(), ""));
                    item.put("summary", valueOr(step.getSummary(), ""));
                    item.put("status", valueOr(step.getStatus(), ""));
                    return item;
                })
                .collect(Collectors.toList());
        return json(Map.of("steps", plan));
    }

    public static String evidenceJson(List<AgentRunStepBO> steps) {
        List<Map<String, Object>> evidence = safeSteps(steps).stream()
                .filter(step -> "TOOL".equals(step.getKind()) && "SUCCEEDED".equals(step.getStatus()))
                .map(step -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("toolCode", valueOr(step.getToolCode(), ""));
                    item.put("source", valueOr(step.getSource(), ""));
                    item.put("sourceUrl", valueOr(step.getSourceUrl(), ""));
                    item.put("dataDate", valueOr(step.getDataDate(), ""));
                    item.put("summary", valueOr(step.getSummary(), ""));
                    item.put("citations", jsonValue(step.getCitationJson(), List.of()));
                    return item;
                })
                .collect(Collectors.toList());
        return json(evidence);
    }

    public static String warningsJson(List<AgentRunStepBO> steps) {
        List<Map<String, Object>> warnings = safeSteps(steps).stream()
                .filter(step -> step.getErrorMessage() != null && !step.getErrorMessage().isBlank())
                .map(step -> Map.<String, Object>of(
                        "toolCode", valueOr(step.getToolCode(), ""),
                        "message", step.getErrorMessage()))
                .collect(Collectors.toList());
        return json(warnings);
    }

    private static List<AgentRunStepBO> safeSteps(List<AgentRunStepBO> steps) {
        return steps == null ? List.of() : steps;
    }

    private static Object jsonValue(String value, Object fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return MAPPER.readTree(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String json(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}