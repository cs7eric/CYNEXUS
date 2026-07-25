package cn.cccs7.cynexus.ai.domain.policy.agent;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentClarificationBO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/** Parses clarification envelopes emitted by the Agent model. */
public final class AgentClarificationPolicy {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentClarificationPolicy() {
    }

    public static AgentClarificationBO parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            JsonNode root = findClarificationNode(content);
            if (root == null) {
                return null;
            }
            List<String> questions = boundedStrings(root.get("questions"), 5, 500);
            List<String> missingFields = boundedStrings(root.get("missingFields"), 8, 120);
            if (questions.isEmpty()) {
                return null;
            }
            return AgentClarificationBO.builder()
                    .summary(boundedText(root.path("summary").asText("需要补充信息后才能继续分析。"), 500))
                    .questions(questions)
                    .missingFields(missingFields)
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String toAnswer(AgentClarificationBO clarification) {
        if (clarification == null) {
            return "需要补充信息后才能继续分析。";
        }
        StringBuilder answer = new StringBuilder(
                clarification.getSummary() == null ? "" : clarification.getSummary());
        if (clarification.getQuestions() != null) {
            for (String question : clarification.getQuestions()) {
                answer.append("\n- ").append(question);
            }
        }
        return answer.toString();
    }

    private static JsonNode findClarificationNode(String content) throws Exception {
        for (int start = 0; start < content.length(); start++) {
            if (content.charAt(start) != '{') {
                continue;
            }
            int depth = 0;
            boolean quoted = false;
            boolean escaped = false;
            for (int index = start; index < content.length(); index++) {
                char current = content.charAt(index);
                if (quoted) {
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        quoted = false;
                    }
                    continue;
                }
                if (current == '"') {
                    quoted = true;
                } else if (current == '{') {
                    depth++;
                } else if (current == '}' && --depth == 0) {
                    JsonNode candidate = MAPPER.readTree(content.substring(start, index + 1));
                    if (candidate != null && "clarification".equals(candidate.path("type").asText(""))) {
                        return candidate;
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static List<String> boundedStrings(JsonNode node, int maxItems, int maxChars) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String text = boundedText(item.asText(""), maxChars);
            if (!text.isBlank()) {
                values.add(text);
            }
            if (values.size() >= maxItems) {
                break;
            }
        }
        return values;
    }

    private static String boundedText(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }
}