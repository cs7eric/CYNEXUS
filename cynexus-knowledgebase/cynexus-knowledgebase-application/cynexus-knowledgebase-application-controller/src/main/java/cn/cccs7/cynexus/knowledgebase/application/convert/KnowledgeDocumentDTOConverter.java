package cn.cccs7.cynexus.knowledgebase.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KnowledgeDocumentDTOConverter extends BaseMapper<KnowledgeDocumentDTO, KnowledgeDocumentBO> {

    KnowledgeDocumentDTOConverter INSTANCE = Mappers.getMapper(KnowledgeDocumentDTOConverter.class);
}
