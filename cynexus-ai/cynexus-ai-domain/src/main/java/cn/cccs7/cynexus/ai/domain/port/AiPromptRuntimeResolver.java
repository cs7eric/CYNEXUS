package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a capability's DB-managed prompt and renders its runtime variables. */
public interface AiPromptRuntimeResolver {

    AiPromptRuntime resolve(String capability, String promptKey, String fallbackSystemPrompt,
                            String fallbackUserPrompt);

    record AiPromptRuntime(String promptKey, String promptVersion, String systemPrompt,
                           String userPromptTemplate, Set<String> declaredVariables, String outputSchema) {
        private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

        public AiPromptRuntime(String promptKey, String promptVersion, String systemPrompt,
                               String userPromptTemplate) {
            this(promptKey, promptVersion, systemPrompt, userPromptTemplate, placeholders(userPromptTemplate), null);
        }

        public AiPromptRuntime(String promptKey, String promptVersion, String systemPrompt,
                               String userPromptTemplate, Set<String> declaredVariables) {
            this(promptKey, promptVersion, systemPrompt, userPromptTemplate, declaredVariables, null);
        }

        public AiPromptRuntime {
            // Legacy prompt rows may predate the variables column. Infer only
            // their actual placeholders, while still rejecting unrelated input.
            declaredVariables = declaredVariables == null || declaredVariables.isEmpty()
                    ? placeholders(userPromptTemplate) : Set.copyOf(declaredVariables);
        }

        public String render(Map<String, String> variables) {
            String rendered = userPromptTemplate == null ? "" : userPromptTemplate;
            if (variables != null) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    if (!declaredVariables.contains(entry.getKey())) {
                        throw new IllegalArgumentException("Prompt variable is not declared: " + entry.getKey());
                    }
                    String replacement = entry.getValue() == null ? "" : entry.getValue();
                    rendered = rendered.replaceAll("\\{\\{\\s*" + Pattern.quote(entry.getKey()) + "\\s*}}",
                            Matcher.quoteReplacement(replacement));
                }
            }
            Matcher unresolved = PLACEHOLDER.matcher(rendered);
            if (unresolved.find()) {
                throw new IllegalArgumentException("Prompt contains undeclared or missing variable: " + unresolved.group(1));
            }
            return (systemPrompt == null ? "" : systemPrompt) + System.lineSeparator() + rendered;
        }

        public static AiPromptRuntime from(AiPromptBO prompt) {
            return new AiPromptRuntime(prompt.getPromptKey(), prompt.getPromptVersion(),
                    normalize(prompt.getSystemPrompt()), normalize(prompt.getUserPromptTemplate()),
                    declared(prompt.getVariables()), normalize(prompt.getOutputSchema()));
        }

        private static Set<String> declared(String raw) {
            Set<String> values = new LinkedHashSet<>();
            if (raw != null) {
                Matcher matcher = Pattern.compile("\\\"([A-Za-z0-9_.-]+)\\\"").matcher(raw);
                while (matcher.find()) values.add(matcher.group(1));
            }
            return values;
        }

        private static Set<String> placeholders(String template) {
            Set<String> values = new LinkedHashSet<>();
            Matcher matcher = PLACEHOLDER.matcher(template == null ? "" : template);
            while (matcher.find()) values.add(matcher.group(1));
            return values;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
