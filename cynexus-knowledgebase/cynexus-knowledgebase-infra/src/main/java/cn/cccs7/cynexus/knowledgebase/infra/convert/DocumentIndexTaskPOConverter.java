package cn.cccs7.cynexus.knowledgebase.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentIndexTaskBO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentIndexTaskPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DocumentIndexTaskPOConverter extends BaseMapper<DocumentIndexTaskPO, KnowledgeDocumentIndexTaskBO> {

    DocumentIndexTaskPOConverter INSTANCE = Mappers.getMapper(DocumentIndexTaskPOConverter.class);
}
