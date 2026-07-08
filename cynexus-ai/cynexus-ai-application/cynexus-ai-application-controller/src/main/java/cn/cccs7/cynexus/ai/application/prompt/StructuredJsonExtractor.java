package cn.cccs7.cynexus.ai.application.prompt;

import org.springframework.stereotype.Component;

@Component
public class StructuredJsonExtractor {

    public String extractObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = stripCodeFence(raw.trim());
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return text;
        }
        return text.substring(start, end + 1);
    }

    private String stripCodeFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }
        String withoutOpeningFence = value.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
        return withoutOpeningFence.replaceFirst("\\s*```$", "").trim();
    }
}
