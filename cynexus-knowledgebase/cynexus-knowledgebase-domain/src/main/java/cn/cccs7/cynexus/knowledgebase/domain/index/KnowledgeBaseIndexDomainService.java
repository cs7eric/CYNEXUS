package cn.cccs7.cynexus.knowledgebase.domain.index;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;

public interface KnowledgeBaseIndexDomainService {

    void refreshDocumentIndex(KnowledgeDocumentBO document);

    void deleteDocumentIndex(Long documentId);
}
