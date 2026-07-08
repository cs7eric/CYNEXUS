package cn.cccs7.cynexus.knowledgebase.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.TagPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TagPOConverter extends BaseMapper<TagPO, KnowledgeTagBO> {

    TagPOConverter INSTANCE = Mappers.getMapper(TagPOConverter.class);
}
