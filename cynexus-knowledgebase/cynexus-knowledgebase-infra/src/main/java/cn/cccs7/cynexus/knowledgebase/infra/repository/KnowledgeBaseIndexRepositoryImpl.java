package cn.cccs7.cynexus.knowledgebase.infra.repository;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentChunkBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentIndexTaskBO;
import cn.cccs7.cynexus.knowledgebase.domain.repository.KnowledgeBaseIndexRepository;
import cn.cccs7.cynexus.knowledgebase.infra.basic.service.KnowledgeBaseIndexBasicService;
import cn.cccs7.cynexus.knowledgebase.infra.convert.DocumentChunkPOConverter;
import cn.cccs7.cynexus.knowledgebase.infra.convert.DocumentIndexTaskPOConverter;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeBaseIndexRepositoryImpl implements KnowledgeBaseIndexRepository {

    private final KnowledgeBaseIndexBasicService indexBasicService;

    public KnowledgeBaseIndexRepositoryImpl(KnowledgeBaseIndexBasicService indexBasicService) {
        this.indexBasicService = indexBasicService;
    }

    @Override
    public void replaceDocumentIndex(Long documentId,
                                     List<KnowledgeDocumentChunkBO> chunks,
                                     List<KnowledgeDocumentIndexTaskBO> tasks) {
        indexBasicService.replaceDocumentIndex(
                documentId,
                DocumentChunkPOConverter.INSTANCE.toSourceList(chunks),
                DocumentIndexTaskPOConverter.INSTANCE.toSourceList(tasks));
    }

    @Override
    public void deleteDocumentIndex(Long documentId) {
        indexBasicService.deleteDocumentIndex(documentId);
    }
}
