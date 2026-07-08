package cn.cccs7.cynexus.knowledgebase.infra.basic.service;

import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentChunkPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentIndexTaskPO;
import java.util.List;

public interface KnowledgeBaseIndexBasicService {

    void replaceDocumentIndex(Long documentId, List<DocumentChunkPO> chunks, List<DocumentIndexTaskPO> tasks);

    void deleteDocumentIndex(Long documentId);
}
