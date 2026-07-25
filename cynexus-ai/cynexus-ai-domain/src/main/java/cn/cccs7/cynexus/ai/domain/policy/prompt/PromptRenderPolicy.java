package cn.cccs7.cynexus.ai.domain.policy.prompt;

import java.util.Map;

/** Pure prompt template rendering shared by scenario and capability execution. */
public final class PromptRenderPolicy {

    private PromptRenderPolicy() {
    }

    public static String render(String template, String userInput, Map<String, Object> variables) {
        String rendered = template == null ? "" : template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
        }
        return rendered + System.lineSeparator() + "User: " + (userInput == null ? "" : userInput);
    }

    public static String applyVariables(String template, Map<String, String> variables) {
        String rendered = template == null ? "" : template;
        if (variables == null) {
            return rendered;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }
}