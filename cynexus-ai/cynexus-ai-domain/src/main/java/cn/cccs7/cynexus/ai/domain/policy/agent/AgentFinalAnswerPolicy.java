package cn.cccs7.cynexus.ai.domain.policy.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Converts provider answer envelopes into user-facing final text. */
public final class AgentFinalAnswerPolicy {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentFinalAnswerPolicy() {
    }

    public static String normalize(String content) {
        String raw = content == null || content.isBlank() ? "The Agent did not produce an answer." : content.trim();
        try {
            JsonNode root = MAPPER.readTree(raw);
            if (root == null || !root.isObject()) {
                return normalizeMarkdown(raw);
            }
            String type = root.path("type").asText("");
            if (!("analysis".equals(type) || "answer".equals(type) || "final".equals(type)
                    || root.has("conclusion") || root.has("usedModules"))) {
                return normalizeMarkdown(raw);
            }
            StringBuilder answer = new StringBuilder();
            appendField(answer, "结论", firstNonBlank(root, "conclusion", "summary", "answer"));
            appendField(answer, "数据日期", root.path("dataDate").asText(""));
            appendList(answer, "使用的数据模块", root.get("usedModules"));
            appendList(answer, "关键输入输出", root.get("keyInputsOutputs"));
            appendList(answer, "限制", root.get("limitations"));
            appendList(answer, "风险观察", root.get("riskWatch"));
            appendList(answer, "下一步建议", root.get("nextSteps"));
            return normalizeMarkdown(answer.length() == 0 ? raw : answer.toString().trim());
        } catch (Exception ignored) {
            return normalizeMarkdown(raw);
        }
    }

    private static String normalizeMarkdown(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("\\\\([*_~])", "$1").trim();
        normalized = normalized
                .replaceAll("(?<!\\n)---(?=#{1,6})", "\n\n---\n\n")
                .replaceAll("(?m)(?<!^)(?<![#\\n])(#{1,6})(?=\\s|[^\\s#])", "\n$1")
                .replaceAll("(?m)^(#{1,6})[ \\t]*", "$1 ")
                .replaceAll("(?m)^(#{1,6} (?:总览|支出结构|主要流水明细|收入结构|资产结构|负债结构)[^|\\n]{0,100})(?=\\|)", "$1\n\n")
                .replaceAll("(?m)^(#{1,6} [一二三四五六七八九十]+、[^|\\n]{1,80})(?=\\|)", "$1\n")
                .replaceAll("(?m)^(#{1,6} (?:结论|核心数据|数据模块|关键输入输出|限制|下一步))(?=\\S)", "$1\n\n")
                .replaceAll("(?<!\\n)([-*+]\\s+\\*\\*)", "\n$1")
                .replaceAll("[ \\t]+---(?=\\s|$)", "\n\n---\n\n");
        return normalized.trim();
    }

    private static String firstNonBlank(JsonNode root, String... fields) {
        for (String field : fields) {
            String value = root.path(field).asText("").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static void appendField(StringBuilder answer, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (answer.length() > 0) {
            answer.append("\n\n");
        }
        answer.append("## ").append(label).append("\n\n").append(value);
    }

    private static void appendList(StringBuilder answer, String label, JsonNode values) {
        if (values == null || !values.isArray() || values.isEmpty()) {
            return;
        }
        if (answer.length() > 0) {
            answer.append("\n\n");
        }
        answer.append("## ").append(label).append("\n\n");
        for (JsonNode value : values) {
            if (value.isObject()) {
                String input = value.path("input").asText("").trim();
                String output = value.path("output").asText("").trim();
                String text = input.isBlank() ? value.toString() : input + (output.isBlank() ? "" : "：" + output);
                answer.append("- ").append(text).append("\n");
            } else if (!value.asText("").isBlank()) {
                answer.append("- ").append(value.asText()).append("\n");
            }
        }
    }
}
