package cn.cccs7.cynexus.knowledgebase.domain.content;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;

/** Centralizes the invariants and defaults shared by knowledge-base writes. */
public final class KnowledgeBaseContentPolicy {

    private KnowledgeBaseContentPolicy() {
    }

    public static KnowledgeDocumentBO normalizeDocument(KnowledgeDocumentBO document) {
        if (document == null || blank(document.getTitle())) {
            throw new IllegalArgumentException("document title is required");
        }
        document.setTitle(document.getTitle().trim());
        document.setDescription(trimToEmpty(document.getDescription()));
        document.setContent(trimToEmpty(document.getContent()));
        document.setContentFormat(defaultValue(document.getContentFormat(), "markdown"));
        document.setStatus(normalizeStatus(document.getStatus()));
        document.setExtra(defaultValue(document.getExtra(), "{}"));
        return document;
    }

    public static KnowledgeTagBO normalizeTag(KnowledgeTagBO tag) {
        if (tag == null || blank(tag.getTagName())) {
            throw new IllegalArgumentException("tag name is required");
        }
        tag.setTagName(tag.getTagName().trim());
        tag.setTagCode(defaultValue(tag.getTagCode(), slug(tag.getTagName())));
        return tag;
    }

    public static KnowledgeCategoryBO normalizeCategory(KnowledgeCategoryBO category) {
        if (category == null || blank(category.getCategoryName())) {
            throw new IllegalArgumentException("category name is required");
        }
        category.setCategoryName(category.getCategoryName().trim());
        category.setCategoryCode(defaultValue(category.getCategoryCode(), slug(category.getCategoryName())));
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        return category;
    }

    public static String normalizeFilter(String value) {
        return blank(value) ? null : value.trim();
    }

    private static String normalizeStatus(String value) {
        String status = defaultValue(value, "draft").toLowerCase();
        if (!status.equals("draft") && !status.equals("published") && !status.equals("archived")) {
            throw new IllegalArgumentException("unsupported document status: " + status);
        }
        return status;
    }

    private static String slug(String value) {
        String result = value.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("(^-|-$)", "");
        return result.isBlank() ? "item" : result;
    }

    private static String defaultValue(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
