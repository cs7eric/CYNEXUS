package cn.cccs7.cynexus.knowledgebase.domain.repository;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentChunkBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentIndexTaskBO;
import java.util.List;

public interface KnowledgeBaseIndexRepository {

    void replaceDocumentIndex(Long documentId,
                              List<KnowledgeDocumentChunkBO> chunks,
                              List<KnowledgeDocumentIndexTaskBO> tasks);

    void deleteDocumentIndex(Long documentId);
}
