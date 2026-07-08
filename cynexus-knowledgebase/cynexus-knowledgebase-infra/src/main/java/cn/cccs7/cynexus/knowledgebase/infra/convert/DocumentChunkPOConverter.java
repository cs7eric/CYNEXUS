package cn.cccs7.cynexus.knowledgebase.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentChunkBO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentChunkPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DocumentChunkPOConverter extends BaseMapper<DocumentChunkPO, KnowledgeDocumentChunkBO> {

    DocumentChunkPOConverter INSTANCE = Mappers.getMapper(DocumentChunkPOConverter.class);
}
