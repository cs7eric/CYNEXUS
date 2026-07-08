package cn.cccs7.cynexus.knowledgebase.infra.convert;

import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DocumentPOConverter extends BaseMapper<DocumentPO, KnowledgeDocumentBO> {

    DocumentPOConverter INSTANCE = Mappers.getMapper(DocumentPOConverter.class);

    @Override
    @Mapping(target = "tagIds", ignore = true)
    @Mapping(target = "categoryIds", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "categories", ignore = true)
    KnowledgeDocumentBO toTarget(DocumentPO source);
}
