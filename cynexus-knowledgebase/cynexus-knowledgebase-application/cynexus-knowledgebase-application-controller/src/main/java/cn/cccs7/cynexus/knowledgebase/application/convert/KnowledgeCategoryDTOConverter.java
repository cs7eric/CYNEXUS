package cn.cccs7.cynexus.knowledgebase.application.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeCategoryDTO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KnowledgeCategoryDTOConverter extends BaseMapper<KnowledgeCategoryDTO, KnowledgeCategoryBO> {

    KnowledgeCategoryDTOConverter INSTANCE = Mappers.getMapper(KnowledgeCategoryDTOConverter.class);
}
