package cn.cccs7.cynexus.ai.domain.port;

import java.util.List;

/** Permission-aware boundary for indexing and semantic retrieval. */
public interface KnowledgeRagPort {
    void index(IndexDocument document);
    default void delete(String documentId) { }
    List<RetrievedChunk> search(SearchRequest request);

    record IndexDocument(String documentId, String tenantKey, String ownerKey, String content,
                         java.util.Map<String, String> metadata) {}
    record SearchRequest(String tenantKey, String principalKey, String query, int topK, String permissionScope) {
        /** Backward-compatible owner-only search for existing callers. */
        public SearchRequest(String tenantKey, String principalKey, String query, int topK) {
            this(tenantKey, principalKey, query, topK, null);
        }
    }
    record RetrievedChunk(String documentId, String content, double score,
                          java.util.Map<String, String> metadata) {}
}
