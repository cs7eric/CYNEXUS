package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.infra.ai.cache.PromptCacheUsage;
import cn.cccs7.cynexus.ai.infra.ai.cache.PromptCacheUsageParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Collects Responses events from either SSE or WebSocket transport. */
final class ResponsesWebSocketEventAccumulator {

    private final ObjectMapper objectMapper;
    private final StringBuilder content = new StringBuilder();
    private final StringBuilder raw = new StringBuilder();
    private final Map<String, ToolAccumulator> tools = new LinkedHashMap<>();
    private final UsageAccumulator usage = new UsageAccumulator();

    ResponsesWebSocketEventAccumulator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String accept(String eventJson, Consumer<String> textDeltaConsumer,
                  Consumer<String> thinkingDeltaConsumer) throws IOException {
        JsonNode root = objectMapper.readTree(eventJson);
        if (root == null || !root.isObject()) throw new IOException("Responses event must be a JSON object");
        String normalizedEvent = objectMapper.writeValueAsString(root);
        raw.append(normalizedEvent).append('\n');
        String type = textValue(root.get(AiConstants.RESPONSES_TYPE_FIELD), "");
        JsonNode usageNode = root.get(AiConstants.AI_USAGE_FIELD);
        JsonNode responseNode = root.get(AiConstants.RESPONSES_RESPONSE_FIELD);
        if (responseNode != null && responseNode.isObject()) {
            usage.responseId = nonBlank(textValue(responseNode.get(AiConstants.RESPONSES_RESPONSE_ID_FIELD), ""),
                    usage.responseId);
            JsonNode outputItems = responseNode.get(AiConstants.RESPONSES_OUTPUT_ITEMS_FIELD);
            if (outputItems != null && outputItems.isArray()) {
                usage.providerOutputJson = objectMapper.writeValueAsString(outputItems);
            }
        }
        if (AiConstants.RESPONSES_COMPLETED_EVENT.equals(type)) {
            usageNode = responseNode == null ? usageNode : responseNode.get(AiConstants.AI_USAGE_FIELD);
        }
        captureUsage(usageNode);
        if (AiConstants.RESPONSES_OUTPUT_TEXT_DELTA_EVENT.equals(type)) {
            String text = textValue(root.get(AiConstants.RESPONSES_DELTA_FIELD), "");
            content.append(text);
            if (!text.isBlank() && textDeltaConsumer != null) textDeltaConsumer.accept(text);
        } else if (AiConstants.RESPONSES_REASONING_SUMMARY_TEXT_DELTA_EVENT.equals(type)
                || AiConstants.RESPONSES_REASONING_TEXT_DELTA_EVENT.equals(type)
                || AiConstants.RESPONSES_REASONING_DELTA_EVENT.equals(type)) {
            emitThinking(textValue(root.get(AiConstants.RESPONSES_DELTA_FIELD), ""), thinkingDeltaConsumer);
        } else if (AiConstants.RESPONSES_REASONING_SUMMARY_PART_ADDED_EVENT.equals(type)
                || AiConstants.RESPONSES_REASONING_SUMMARY_PART_DONE_EVENT.equals(type)) {
            String thinking = textValue(root.path(AiConstants.RESPONSES_PART_FIELD)
                    .get(AiConstants.RESPONSES_TEXT_FIELD), "");
            emitThinking(thinking.isBlank() ? textValue(root.get(AiConstants.RESPONSES_DELTA_FIELD), "") : thinking,
                    thinkingDeltaConsumer);
        } else if (AiConstants.RESPONSES_OUTPUT_ITEM_ADDED_EVENT.equals(type)
                || AiConstants.RESPONSES_OUTPUT_ITEM_DONE_EVENT.equals(type)) {
            appendOutputItem(root.get(AiConstants.RESPONSES_ITEM_FIELD));
        } else if (AiConstants.RESPONSES_FUNCTION_CALL_ARGUMENTS_DELTA_EVENT.equals(type)) {
            findTool(textValue(root.get(AiConstants.RESPONSES_FUNCTION_CALL_ITEM_ID_EVENT_FIELD), ""),
                    textValue(root.get(AiConstants.RESPONSES_FUNCTION_CALL_ID_FIELD), ""))
                    .arguments.append(textValue(root.get(AiConstants.RESPONSES_DELTA_FIELD), ""));
        } else if (AiConstants.RESPONSES_FUNCTION_CALL_ARGUMENTS_DONE_EVENT.equals(type)) {
            ToolAccumulator accumulator = findTool(textValue(root.get(AiConstants.RESPONSES_FUNCTION_CALL_ITEM_ID_EVENT_FIELD), ""),
                    textValue(root.get(AiConstants.RESPONSES_FUNCTION_CALL_ID_FIELD), ""));
            if (accumulator.arguments.isEmpty()) {
                accumulator.arguments.append(textValue(root.get(AiConstants.RESPONSES_FUNCTION_CALL_ARGUMENTS_FIELD), ""));
            }
        }
        return type;
    }

    boolean hasOutput() {
        return !content.isEmpty() || !tools.isEmpty();
    }

    AgentModelTurnResponse toResponse(AgentModelTurnRequest request) {
        List<AgentModelToolCall> calls = new ArrayList<>();
        for (ToolAccumulator accumulator : tools.values()) {
            if (accumulator.name == null || accumulator.name.isBlank()) continue;
            try {
                JsonNode arguments = accumulator.arguments.isEmpty() ? objectMapper.createObjectNode()
                        : objectMapper.readTree(accumulator.arguments.toString());
                if (!arguments.isObject()) throw new IllegalStateException("AI tool arguments must be a JSON object");
                String callId = nonBlank(accumulator.callId, "stream-call-" + calls.size());
                calls.add(AgentModelToolCall.builder().itemId(accumulator.itemId).callId(callId)
                        .toolCode(canonicalToolName(accumulator.name))
                        .arguments(objectMapper.convertValue(arguments, Map.class)).build());
            } catch (IOException exception) {
                throw new IllegalStateException("AI provider emitted invalid tool arguments", exception);
            }
        }
        int estimatedInputTokens = estimateTokens(requestPrompt(request));
        int estimatedOutputTokens = estimateTokens(content.toString());
        int inputTokens = usage.inputTokens == null ? estimatedInputTokens : usage.inputTokens;
        int outputTokens = usage.outputTokens == null ? estimatedOutputTokens : usage.outputTokens;
        int totalTokens = usage.totalTokens == null ? inputTokens + outputTokens : usage.totalTokens;
        return AgentModelTurnResponse.builder().type(calls.isEmpty() ? "FINAL" : "TOOL_CALLS")
                .content(content.toString()).toolCalls(calls).inputTokens(inputTokens)
                .cachedInputTokens(usage.cachedInputTokens).cacheWriteTokens(usage.cacheWriteTokens)
                .outputTokens(outputTokens).totalTokens(totalTokens).providerResponseId(usage.responseId)
                .providerOutputJson(usage.providerOutputJson)
                .rawResponse(formatRawResponse()).build();
    }

    private void appendOutputItem(JsonNode item) {
        if (item == null || !AiConstants.RESPONSES_FUNCTION_CALL_TYPE.equals(
                textValue(item.get(AiConstants.RESPONSES_TYPE_FIELD), ""))) return;
        String itemId = textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_ITEM_ID_FIELD), "");
        String callId = textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_ID_FIELD), "");
        String key = nonBlank(itemId, nonBlank(callId, String.valueOf(tools.size())));
        ToolAccumulator accumulator = tools.computeIfAbsent(key, ignored -> new ToolAccumulator());
        accumulator.itemId = nonBlank(itemId, accumulator.itemId);
        accumulator.callId = nonBlank(callId, nonBlank(itemId, accumulator.callId));
        accumulator.name = nonBlank(textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_NAME_FIELD), ""),
                accumulator.name);
        accumulator.arguments.append(textValue(item.get(AiConstants.RESPONSES_FUNCTION_CALL_ARGUMENTS_FIELD), ""));
    }

    private ToolAccumulator findTool(String itemId, String callId) {
        ToolAccumulator accumulator = tools.get(itemId);
        if (accumulator == null) accumulator = tools.get(callId);
        if (accumulator == null) {
            accumulator = tools.values().stream()
                    .filter(candidate -> (!itemId.isBlank() && itemId.equals(candidate.itemId))
                            || (!callId.isBlank() && callId.equals(candidate.callId)))
                    .findFirst().orElse(null);
        }
        if (accumulator != null) return accumulator;
        String key = nonBlank(itemId, nonBlank(callId, String.valueOf(tools.size())));
        accumulator = tools.computeIfAbsent(key, ignored -> new ToolAccumulator());
        accumulator.itemId = nonBlank(itemId, accumulator.itemId);
        accumulator.callId = nonBlank(callId, accumulator.callId);
        return accumulator;
    }

    private void captureUsage(JsonNode usageNode) {
        if (usageNode == null || !usageNode.isObject()) return;
        Integer input = firstInt(usageNode, AiConstants.AI_USAGE_INPUT_TOKENS_FIELD,
                AiConstants.AI_USAGE_PROMPT_TOKENS_FIELD);
        Integer output = firstInt(usageNode, AiConstants.AI_USAGE_OUTPUT_TOKENS_FIELD,
                AiConstants.AI_USAGE_COMPLETION_TOKENS_FIELD);
        Integer total = firstInt(usageNode, AiConstants.AI_USAGE_TOTAL_TOKENS_FIELD);
        PromptCacheUsage cacheUsage = PromptCacheUsageParser.parse(usageNode);
        if (input != null) usage.inputTokens = input;
        if (cacheUsage.getCachedInputTokens() != null) usage.cachedInputTokens = cacheUsage.getCachedInputTokens();
        if (cacheUsage.getCacheWriteTokens() != null) usage.cacheWriteTokens = cacheUsage.getCacheWriteTokens();
        if (output != null) usage.outputTokens = output;
        if (total != null) usage.totalTokens = total;
    }

    private String formatRawResponse() {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put(AiConstants.RESPONSES_EVENTS_FIELD, raw.toString());
        if (usage.inputTokens != null || usage.cachedInputTokens != null || usage.cacheWriteTokens != null
                || usage.outputTokens != null || usage.totalTokens != null) {
            ObjectNode usageNode = envelope.putObject(AiConstants.AI_USAGE_FIELD);
            if (usage.inputTokens != null) usageNode.put(AiConstants.AI_USAGE_INPUT_TOKENS_FIELD, usage.inputTokens);
            if (usage.cachedInputTokens != null) usageNode.put(AiConstants.PROMPT_CACHE_USAGE_CACHED_INPUT_TOKENS_FIELD,
                    usage.cachedInputTokens);
            if (usage.cacheWriteTokens != null) usageNode.put(AiConstants.PROMPT_CACHE_USAGE_WRITE_TOKENS_FIELD,
                    usage.cacheWriteTokens);
            if (usage.outputTokens != null) usageNode.put(AiConstants.AI_USAGE_OUTPUT_TOKENS_FIELD, usage.outputTokens);
            if (usage.totalTokens != null) usageNode.put(AiConstants.AI_USAGE_TOTAL_TOKENS_FIELD, usage.totalTokens);
        }
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            return raw.toString();
        }
    }

    private void emitThinking(String thinking, Consumer<String> consumer) {
        if (thinking != null && !thinking.isBlank() && consumer != null) consumer.accept(thinking);
    }

    private String requestPrompt(AgentModelTurnRequest request) {
        return request == null || request.getMessages() == null ? "" : request.getMessages().stream()
                .map(AgentModelMessage::getContent).filter(value -> value != null)
                .reduce("", (left, right) -> left + right);
    }

    private String canonicalToolName(String providerToolName) {
        if (providerToolName == null || !providerToolName.startsWith(AiConstants.PROVIDER_TOOL_PREFIX)) {
            return providerToolName;
        }
        String hex = providerToolName.substring(AiConstants.PROVIDER_TOOL_PREFIX.length());
        if (hex.isBlank() || hex.length() % 2 != 0 || !hex.matches("[0-9a-fA-F]+")) return providerToolName;
        try {
            byte[] decoded = new byte[hex.length() / 2];
            for (int index = 0; index < hex.length(); index += 2) {
                decoded[index / 2] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
            }
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            return providerToolName;
        }
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private Integer firstInt(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.canConvertToInt()) return value.asInt();
        }
        return null;
    }

    private String textValue(JsonNode node, String fallback) {
        return node == null || node.isNull() ? fallback : node.asText(fallback);
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class ToolAccumulator {
        private String itemId;
        private String callId;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }

    private static final class UsageAccumulator {
        private Integer inputTokens;
        private Integer cachedInputTokens;
        private Integer cacheWriteTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        private String responseId;
        private String providerOutputJson;
    }
}
