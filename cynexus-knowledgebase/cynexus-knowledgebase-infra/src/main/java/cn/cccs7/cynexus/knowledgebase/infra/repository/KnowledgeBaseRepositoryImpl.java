package cn.cccs7.cynexus.knowledgebase.infra.repository;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import cn.cccs7.cynexus.knowledgebase.domain.content.KnowledgeDocumentSearchQuery;
import cn.cccs7.cynexus.knowledgebase.domain.repository.KnowledgeBaseRepository;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentCategoryMappingPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentTagMappingPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.service.KnowledgeBaseBasicService;
import cn.cccs7.cynexus.knowledgebase.infra.convert.CategoryPOConverter;
import cn.cccs7.cynexus.knowledgebase.infra.convert.DocumentPOConverter;
import cn.cccs7.cynexus.knowledgebase.infra.convert.TagPOConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private final KnowledgeBaseBasicService basicService;

    public KnowledgeBaseRepositoryImpl(KnowledgeBaseBasicService basicService) {
        this.basicService = basicService;
    }

    @Override
    public List<KnowledgeDocumentBO> listDocuments(KnowledgeDocumentSearchQuery query) {
        List<KnowledgeDocumentBO> documents = DocumentPOConverter.INSTANCE.toTargetList(
                basicService.listDocuments(query));
        enrichDocuments(documents);
        return documents;
    }

    @Override
    public KnowledgeDocumentBO findDocumentById(Long id) {
        KnowledgeDocumentBO document = DocumentPOConverter.INSTANCE.toTarget(basicService.getDocumentById(id));
        enrichDocuments(document == null ? List.of() : List.of(document));
        return document;
    }

    @Override
    public Long createDocument(KnowledgeDocumentBO document) {
        return basicService.createDocument(
                DocumentPOConverter.INSTANCE.toSource(document),
                document.getTagIds(),
                document.getCategoryIds());
    }

    @Override
    public boolean updateDocument(KnowledgeDocumentBO document) {
        return basicService.updateDocument(
                DocumentPOConverter.INSTANCE.toSource(document),
                document.getTagIds(),
                document.getCategoryIds());
    }

    @Override
    public boolean deleteDocument(Long id) {
        return basicService.deleteDocument(id);
    }

    @Override
    public List<KnowledgeTagBO> listTags(String keyword) {
        return TagPOConverter.INSTANCE.toTargetList(basicService.listTags(keyword));
    }

    @Override
    public Long createTag(KnowledgeTagBO tag) {
        return basicService.createTag(TagPOConverter.INSTANCE.toSource(tag));
    }

    @Override
    public boolean updateTag(KnowledgeTagBO tag) {
        return basicService.updateTag(TagPOConverter.INSTANCE.toSource(tag));
    }

    @Override
    public boolean deleteTag(Long id) {
        return basicService.deleteTag(id);
    }

    @Override
    public List<KnowledgeCategoryBO> listCategories(String keyword) {
        return CategoryPOConverter.INSTANCE.toTargetList(basicService.listCategories(keyword));
    }

    @Override
    public Long createCategory(KnowledgeCategoryBO category) {
        return basicService.createCategory(CategoryPOConverter.INSTANCE.toSource(category));
    }

    @Override
    public boolean updateCategory(KnowledgeCategoryBO category) {
        return basicService.updateCategory(CategoryPOConverter.INSTANCE.toSource(category));
    }

    @Override
    public boolean deleteCategory(Long id) {
        return basicService.deleteCategory(id);
    }

    private void enrichDocuments(List<KnowledgeDocumentBO> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<Long> documentIds = documents.stream().map(KnowledgeDocumentBO::getId).filter(Objects::nonNull).toList();
        List<DocumentTagMappingPO> tagMappings = basicService.listDocumentTagMappings(documentIds);
        List<DocumentCategoryMappingPO> categoryMappings = basicService.listDocumentCategoryMappings(documentIds);
        List<Long> tagIds = tagMappings.stream().map(DocumentTagMappingPO::getTagId).distinct().toList();
        List<Long> categoryIds = categoryMappings.stream().map(DocumentCategoryMappingPO::getCategoryId).distinct().toList();
        Map<Long, KnowledgeTagBO> tags = TagPOConverter.INSTANCE.toTargetList(basicService.listTagsByIds(tagIds))
                .stream()
                .collect(Collectors.toMap(KnowledgeTagBO::getId, Function.identity()));
        Map<Long, KnowledgeCategoryBO> categories = CategoryPOConverter.INSTANCE.toTargetList(basicService.listCategoriesByIds(categoryIds))
                .stream()
                .collect(Collectors.toMap(KnowledgeCategoryBO::getId, Function.identity()));

        for (KnowledgeDocumentBO document : documents) {
            List<Long> documentTagIds = tagMappings.stream()
                    .filter(mapping -> Objects.equals(mapping.getDocumentId(), document.getId()))
                    .map(DocumentTagMappingPO::getTagId)
                    .toList();
            List<Long> documentCategoryIds = categoryMappings.stream()
                    .filter(mapping -> Objects.equals(mapping.getDocumentId(), document.getId()))
                    .map(DocumentCategoryMappingPO::getCategoryId)
                    .toList();
            document.setTagIds(documentTagIds);
            document.setCategoryIds(documentCategoryIds);
            document.setTags(documentTagIds.stream().map(tags::get).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new)));
            document.setCategories(documentCategoryIds.stream().map(categories::get).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new)));
        }
    }
}
