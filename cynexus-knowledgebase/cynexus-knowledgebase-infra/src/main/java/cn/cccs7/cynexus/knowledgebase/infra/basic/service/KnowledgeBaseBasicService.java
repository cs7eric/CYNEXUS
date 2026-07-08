package cn.cccs7.cynexus.knowledgebase.infra.basic.service;

import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.CategoryPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentCategoryMappingPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentTagMappingPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.TagPO;
import java.util.List;

public interface KnowledgeBaseBasicService {

    List<DocumentPO> listDocuments(String keyword, String status, Long tagId, Long categoryId);

    DocumentPO getDocumentById(Long id);

    Long createDocument(DocumentPO document, List<Long> tagIds, List<Long> categoryIds);

    boolean updateDocument(DocumentPO document, List<Long> tagIds, List<Long> categoryIds);

    boolean deleteDocument(Long id);

    List<DocumentTagMappingPO> listDocumentTagMappings(List<Long> documentIds);

    List<DocumentCategoryMappingPO> listDocumentCategoryMappings(List<Long> documentIds);

    List<TagPO> listTags(String keyword);

    List<TagPO> listTagsByIds(List<Long> ids);

    Long createTag(TagPO tag);

    boolean updateTag(TagPO tag);

    boolean deleteTag(Long id);

    List<CategoryPO> listCategories(String keyword);

    List<CategoryPO> listCategoriesByIds(List<Long> ids);

    Long createCategory(CategoryPO category);

    boolean updateCategory(CategoryPO category);

    boolean deleteCategory(Long id);
}
