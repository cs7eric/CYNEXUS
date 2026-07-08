package cn.cccs7.cynexus.knowledgebase.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeTagDTO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KnowledgeTagDTOConverter extends BaseMapper<KnowledgeTagDTO, KnowledgeTagBO> {

    KnowledgeTagDTOConverter INSTANCE = Mappers.getMapper(KnowledgeTagDTOConverter.class);
}
