package cn.cccs7.cynexus.ai.domain.rag;

/**
 * Normalizes and validates the business contract for permission-scoped RAG
 * retrieval. Transport DTOs stay outside the domain; callers provide the
 * primitive request values and receive the port command to execute.
 */
public final class KnowledgeRagSearchPolicy {

    public SearchRequest normalize(String tenantKey, String principalKey, String query,
                                   Integer topK, String permissionScope) {
        if (blank(tenantKey) || blank(principalKey) || blank(query)) {
            throw new IllegalArgumentException("tenantKey, principalKey and query are required");
        }
        int normalizedTopK = topK == null ? 8 : Math.max(1, Math.min(50, topK));
        return new SearchRequest(tenantKey.trim(), principalKey.trim(), query.trim(), normalizedTopK,
                permissionScope == null ? null : permissionScope.trim());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record SearchRequest(String tenantKey, String principalKey, String query,
                                int topK, String permissionScope) {
    }
}
