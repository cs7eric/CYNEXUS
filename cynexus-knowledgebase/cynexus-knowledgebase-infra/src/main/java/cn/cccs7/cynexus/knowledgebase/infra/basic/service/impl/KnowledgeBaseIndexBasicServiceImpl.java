package cn.cccs7.cynexus.knowledgebase.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentChunkPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentIndexTaskPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.DocumentChunkMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.DocumentIndexTaskMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.service.KnowledgeBaseIndexBasicService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeBaseIndexBasicServiceImpl implements KnowledgeBaseIndexBasicService {

    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentIndexTaskMapper documentIndexTaskMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public KnowledgeBaseIndexBasicServiceImpl(DocumentChunkMapper documentChunkMapper,
                                              DocumentIndexTaskMapper documentIndexTaskMapper,
                                              SnowflakeIdGenerator snowflakeIdGenerator) {
        this.documentChunkMapper = documentChunkMapper;
        this.documentIndexTaskMapper = documentIndexTaskMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    @Transactional
    public void replaceDocumentIndex(Long documentId, List<DocumentChunkPO> chunks, List<DocumentIndexTaskPO> tasks) {
        deleteDocumentIndex(documentId);
        if (chunks != null) {
            for (DocumentChunkPO chunk : chunks) {
                if (chunk.getId() == null) {
                    chunk.setId(snowflakeIdGenerator.nextId());
                }
                documentChunkMapper.insert(chunk);
            }
        }
        if (tasks != null) {
            for (DocumentIndexTaskPO task : tasks) {
                if (task.getId() == null) {
                    task.setId(snowflakeIdGenerator.nextId());
                }
                documentIndexTaskMapper.insert(task);
            }
        }
    }

    @Override
    @Transactional
    public void deleteDocumentIndex(Long documentId) {
        if (documentId == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        documentChunkMapper.update(null, new LambdaUpdateWrapper<DocumentChunkPO>()
                .eq(DocumentChunkPO::getDocumentId, documentId)
                .isNull(DocumentChunkPO::getDeletedAt)
                .set(DocumentChunkPO::getUpdatedAt, now)
                .set(DocumentChunkPO::getDeletedAt, now));
        documentIndexTaskMapper.update(null, new LambdaUpdateWrapper<DocumentIndexTaskPO>()
                .eq(DocumentIndexTaskPO::getDocumentId, documentId)
                .isNull(DocumentIndexTaskPO::getDeletedAt)
                .set(DocumentIndexTaskPO::getUpdatedAt, now)
                .set(DocumentIndexTaskPO::getDeletedAt, now));
    }
}
