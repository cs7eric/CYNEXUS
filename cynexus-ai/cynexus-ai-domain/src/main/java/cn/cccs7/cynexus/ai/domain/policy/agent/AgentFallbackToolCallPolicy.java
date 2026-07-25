package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parses fallback tool-call envelopes when the provider returns structured text instead of native tool calls. */
public final class AgentFallbackToolCallPolicy {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentFallbackToolCallPolicy() {
    }

    @SuppressWarnings("unchecked")
    public static List<AgentModelToolCall> parse(String content, int maxToolCalls) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(content);
            String type = root.path("type").asText("");
            if (!"tool_calls".equals(type) && !"action".equals(type)) {
                return List.of();
            }
            JsonNode calls = root.get("calls");
            if (calls == null || !calls.isArray() || calls.isEmpty()
                    || calls.size() > Math.max(0, maxToolCalls)) {
                return List.of();
            }
            List<AgentModelToolCall> parsed = new ArrayList<>();
            for (JsonNode item : calls) {
                String toolCode = item.path("toolCode").asText(item.path("tool").asText(""));
                if (toolCode.isBlank() || !item.has("arguments") || !item.get("arguments").isObject()) {
                    return List.of();
                }
                Map<String, Object> arguments = MAPPER.convertValue(item.get("arguments"), Map.class);
                parsed.add(AgentModelToolCall.builder()
                        .callId(item.path("callId").asText("fallback-" + parsed.size()))
                        .toolCode(toolCode)
                        .arguments(arguments)
                        .build());
            }
            return List.copyOf(parsed);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}