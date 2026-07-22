package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockAiModelAdapter implements AiModelAdapter {

    private static final int TITLE_MAX_LENGTH = 40;
    private static final int DESCRIPTION_MAX_LENGTH = 120;
    private static final String SOURCE_MARKER = "原始文本：";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String providerCode() {
        return "mock";
    }

    @Override
    public AiCompletionResultBO complete(AiModelBO model, String prompt) {
        String sourceText = extractSourceText(prompt);
        String title = firstMeaningfulLine(sourceText);
        String description = compact(sourceText);
        Map<String, Object> result = Map.of(
                "title", truncate(title, TITLE_MAX_LENGTH),
                "description", truncate(description, DESCRIPTION_MAX_LENGTH),
                "tags", fallbackTags(sourceText),
                "categories", List.of("Knowledge Base"),
                "content", buildContent(title, description, sourceText)
        );
        try {
            String content = objectMapper.writeValueAsString(result);
            return AiCompletionResultBO.builder()
                    .content(content)
                    .providerCode(providerCode())
                    .modelCode(model == null ? "mock-model" : model.getModelCode())
                    .inputTokens(estimateTokens(prompt))
                    .outputTokens(estimateTokens(content))
                    .totalTokens(estimateTokens(prompt) + estimateTokens(content))
                    .rawResponse(content)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build mock AI response", e);
        }
    }

    private String extractSourceText(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        int markerIndex = prompt.lastIndexOf(SOURCE_MARKER);
        if (markerIndex < 0) {
            return prompt.trim();
        }
        return prompt.substring(markerIndex + SOURCE_MARKER.length()).trim();
    }

    private String firstMeaningfulLine(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return "Untitled Knowledge Document";
        }
        return sourceText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("Untitled Knowledge Document");
    }

    private List<String> fallbackTags(String sourceText) {
        String lower = sourceText == null ? "" : sourceText.toLowerCase();
        if (lower.contains("ai") || lower.contains("prompt") || lower.contains("llm")) {
            return List.of("markdown", "knowledge-base", "ai");
        }
        return List.of("markdown", "knowledge-base");
    }

    private String buildContent(String title, String description, String sourceText) {
        return "# " + truncate(title, TITLE_MAX_LENGTH)
                + System.lineSeparator()
                + System.lineSeparator()
                + "## Summary"
                + System.lineSeparator()
                + truncate(description, DESCRIPTION_MAX_LENGTH)
                + System.lineSeparator()
                + System.lineSeparator()
                + "## Source Notes"
                + System.lineSeparator()
                + (sourceText == null ? "" : sourceText.trim());
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        String normalized = compact(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }
}
