package cn.cccs7.cynexus.ai.domain.port;

import java.util.Map;

/** Domain-facing indexing orchestration; implementations must preserve tenant ownership metadata. */
public interface KnowledgeIndexingPort {
    void index(IndexCommand command);
    void delete(String documentId);
    record IndexCommand(String documentId, String tenantKey, String ownerKey, String content,
                        Map<String, String> metadata) {}
}
