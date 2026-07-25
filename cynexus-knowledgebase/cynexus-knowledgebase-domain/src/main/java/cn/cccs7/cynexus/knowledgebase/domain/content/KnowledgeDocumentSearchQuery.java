package cn.cccs7.cynexus.knowledgebase.domain.content;

import cn.cccs7.cynexus.knowledgebase.common.constant.KnowledgeBaseConstants;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalized, bounded search intent for knowledge documents.
 *
 * <p>The repository uses the tokens to retrieve a bounded candidate set. The
 * domain then ranks those candidates so matching behavior is consistent across
 * persistence implementations.</p>
 */
public final class KnowledgeDocumentSearchQuery {
    private static final Pattern NON_WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}\\u4e00-\\u9fff]+", Pattern.UNICODE_CHARACTER_CLASS);

    private final String keyword;
    private final String status;
    private final Long tagId;
    private final Long categoryId;
    private final List<String> tokens;

    private KnowledgeDocumentSearchQuery(String keyword, String status, Long tagId, Long categoryId, List<String> tokens) {
        this.keyword = keyword;
        this.status = status;
        this.tagId = tagId;
        this.categoryId = categoryId;
        this.tokens = tokens;
    }

    public static KnowledgeDocumentSearchQuery of(String rawKeyword, String rawStatus, Long tagId, Long categoryId) {
        String normalizedKeyword = normalize(rawKeyword);
        return new KnowledgeDocumentSearchQuery(
                normalizedKeyword,
                normalizeStatus(rawStatus),
                tagId,
                categoryId,
                tokenize(normalizedKeyword));
    }

    public String getKeyword() {
        return keyword;
    }

    public String getStatus() {
        return status;
    }

    public Long getTagId() {
        return tagId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public boolean hasText() {
        return !tokens.isEmpty();
    }

    /** Ranks database candidates without inventing relevance data. */
    public List<KnowledgeDocumentBO> rank(List<KnowledgeDocumentBO> candidates) {
        if (candidates == null || candidates.isEmpty() || !hasText()) {
            return candidates == null ? List.of() : List.copyOf(candidates);
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt(this::score).reversed()
                        .thenComparing(KnowledgeDocumentBO::getUpdatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private int score(KnowledgeDocumentBO document) {
        String title = lower(document == null ? null : document.getTitle());
        String description = lower(document == null ? null : document.getDescription());
        String content = lower(document == null ? null : document.getContent());
        int score = keyword.length() > 0 && title.contains(keyword) ? 1_000 : 0;
        for (String token : tokens) {
            if (title.contains(token)) score += 100;
            else if (description.contains(token)) score += 20;
            else if (content.contains(token)) score += 1;
        }
        return score;
    }

    private static List<String> tokenize(String keyword) {
        if (keyword.isBlank()) return List.of();
        Set<String> tokens = new LinkedHashSet<>();
        for (String segment : NON_WORD_SEPARATOR.split(keyword)) {
            if (segment.isBlank()) continue;
            if (containsCjk(segment)) {
                addCjkTokens(segment, tokens);
            } else {
                tokens.add(segment);
            }
            if (tokens.size() >= KnowledgeBaseConstants.MAX_SEARCH_TOKENS) break;
        }
        return List.copyOf(tokens).subList(0, Math.min(tokens.size(), KnowledgeBaseConstants.MAX_SEARCH_TOKENS));
    }

    private static void addCjkTokens(String segment, Set<String> tokens) {
        if (segment.length() <= 4) {
            tokens.add(segment);
        }
        for (int index = 0; index + 1 < segment.length() && tokens.size() < KnowledgeBaseConstants.MAX_SEARCH_TOKENS; index++) {
            String pair = segment.substring(index, index + 2);
            if (containsCjk(pair)) tokens.add(pair);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= KnowledgeBaseConstants.MAX_SEARCH_KEYWORD_LENGTH
                ? normalized
                : normalized.substring(0, KnowledgeBaseConstants.MAX_SEARCH_KEYWORD_LENGTH);
    }

    private static String normalizeStatus(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean containsCjk(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4e00 && codePoint <= 0x9fff);
    }
}
