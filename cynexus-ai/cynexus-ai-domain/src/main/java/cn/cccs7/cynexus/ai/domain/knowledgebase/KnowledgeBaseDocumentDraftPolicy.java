package cn.cccs7.cynexus.ai.domain.knowledgebase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Domain normalization and deterministic fallback rules for generated knowledge documents. */
public final class KnowledgeBaseDocumentDraftPolicy {
    public static final int MAX_SOURCE_LENGTH = 20_000;

    public String normalizeSource(String value) {
        return value == null ? "" : value.trim();
    }

    public String truncateSource(String value) {
        return value.length() <= MAX_SOURCE_LENGTH ? value : value.substring(0, MAX_SOURCE_LENGTH);
    }

    public Draft normalize(String title, String description, List<String> tags, List<String> categories,
                           String content, String sourceText, String error) {
        String safeSource = normalizeSource(sourceText);
        String normalizedTitle = normalizeSource(title);
        String normalizedDescription = normalizeSource(description);
        String normalizedContent = normalizeSource(content);
        String fallbackContent = fallbackContent(safeSource);
        if (error != null && !normalizeSource(error).isBlank()) {
            fallbackContent += System.lineSeparator() + System.lineSeparator() + "<!-- AI fallback reason: "
                    + truncate(normalizeSource(error), 500).replace("--", "-") + " -->";
        }
        return new Draft(
                normalizedTitle.isBlank() ? title(safeSource) : normalizedTitle,
                normalizedDescription.isBlank() ? description(safeSource) : normalizedDescription,
                normalizeList(tags, tags(safeSource)),
                normalizeList(categories, List.of("Knowledge Base")),
                normalizedContent.isBlank() ? fallbackContent : normalizedContent);
    }

    public String truncate(String value, int maxLength) {
        String normalized = normalizeSource(value);
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private List<String> normalizeList(List<String> values, List<String> fallback) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) for (String value : values) if (!normalizeSource(value).isBlank()) result.add(normalizeSource(value));
        return result.isEmpty() ? fallback : new ArrayList<>(result);
    }

    private List<String> tags(String source) {
        List<String> result = new ArrayList<>(List.of("markdown", "knowledge-base"));
        if (source.toLowerCase().contains("ai")) result.add("ai");
        return result;
    }

    private String title(String source) {
        return source.lines().map(String::trim).filter(item -> !item.isBlank()).findFirst()
                .map(item -> truncate(item, 40)).orElse("Untitled Knowledge Document");
    }

    private String description(String source) { return truncate(source.replaceAll("\\s+", " ").trim(), 120); }

    private String fallbackContent(String source) {
        return "# " + title(source) + System.lineSeparator() + System.lineSeparator()
                + "## Summary" + System.lineSeparator() + description(source) + System.lineSeparator() + System.lineSeparator()
                + "## Source Notes" + System.lineSeparator() + source;
    }

    public record Draft(String title, String description, List<String> tags, List<String> categories, String content) { }
}
