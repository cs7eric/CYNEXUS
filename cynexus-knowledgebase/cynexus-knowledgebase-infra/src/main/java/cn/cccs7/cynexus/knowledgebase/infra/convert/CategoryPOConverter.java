package cn.cccs7.cynexus.knowledgebase.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.CategoryPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CategoryPOConverter extends BaseMapper<CategoryPO, KnowledgeCategoryBO> {

    CategoryPOConverter INSTANCE = Mappers.getMapper(CategoryPOConverter.class);
}
