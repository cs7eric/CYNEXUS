package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Normalizes and bounds Agent tool outputs before persistence / model feedback. */
public final class AgentToolOutputPolicy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int PREVIEW_CHARS = 8_000;

    private AgentToolOutputPolicy() {
    }

    public static String normalize(String output) {
        String raw = output == null ? "{}" : output;
        try {
            var node = MAPPER.readTree(raw);
            return MAPPER.writeValueAsString(node == null ? MAPPER.createObjectNode() : node);
        } catch (Exception ignored) {
            try {
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("invalidJson", true);
                fallback.put("raw", raw);
                return MAPPER.writeValueAsString(fallback);
            } catch (Exception exception) {
                return "{\"invalidJson\":true}";
            }
        }
    }

    public static AgentToolResult bound(AgentToolResult result, int maxOutputChars) {
        if (result == null) {
            return AgentToolResult.failure("unknown", "The tool returned no result.", "cynexus-ai", "EMPTY_RESULT");
        }
        String output = normalize(result.getOutputJson() == null ? "{}" : result.getOutputJson());
        List<String> warnings = result.getWarnings() == null ? List.of() : new ArrayList<>(result.getWarnings());
        int limit = Math.max(1, maxOutputChars);
        if (output.length() <= limit) {
            return copy(result, output, result.getSummary() == null ? "" : result.getSummary(), warnings);
        }
        String preview = output.substring(0, Math.min(output.length(), PREVIEW_CHARS));
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("truncated", true);
        compact.put("originalChars", output.length());
        compact.put("preview", preview);
        String compactOutput;
        try {
            compactOutput = MAPPER.writeValueAsString(compact);
        } catch (Exception exception) {
            compactOutput = "{\"truncated\":true}";
        }
        String summary = (result.getSummary() == null ? "" : result.getSummary()) + " Output truncated.";
        List<String> nextWarnings = new ArrayList<>(warnings);
        nextWarnings.add("OUTPUT_TRUNCATED");
        return copy(result, compactOutput, summary, nextWarnings);
    }

    private static AgentToolResult copy(AgentToolResult result, String output, String summary, List<String> warnings) {
        return AgentToolResult.builder()
                .toolCode(result.getToolCode())
                .status(result.getStatus())
                .outputJson(output)
                .summary(summary)
                .dataDate(result.getDataDate())
                .source(result.getSource())
                .sourceUrl(result.getSourceUrl())
                .warnings(warnings == null ? List.of() : List.copyOf(warnings))
                .citations(result.getCitations() == null ? List.of() : result.getCitations())
                .build();
    }
}