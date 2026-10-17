package cn.cccs7.cynexus.knowledgebase.domain.index;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;

/** Publishes document lifecycle changes for asynchronous AI/vector indexing. */
public interface KnowledgeDocumentIndexEventPublisher {
    void documentChanged(KnowledgeDocumentBO document);
    void documentDeleted(Long documentId);
}
