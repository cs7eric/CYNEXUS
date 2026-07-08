package cn.cccs7.cynexus.knowledgebase.infra.basic.service.impl;

import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.CategoryPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentCategoryMappingPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.DocumentTagMappingPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.entity.TagPO;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.CategoryMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.DocumentCategoryMappingMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.DocumentMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.DocumentTagMappingMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.mapper.TagMapper;
import cn.cccs7.cynexus.knowledgebase.infra.basic.service.KnowledgeBaseBasicService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeBaseBasicServiceImpl implements KnowledgeBaseBasicService {

    private final DocumentMapper documentMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final DocumentTagMappingMapper documentTagMappingMapper;
    private final DocumentCategoryMappingMapper documentCategoryMappingMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public KnowledgeBaseBasicServiceImpl(
            DocumentMapper documentMapper,
            TagMapper tagMapper,
            CategoryMapper categoryMapper,
            DocumentTagMappingMapper documentTagMappingMapper,
            DocumentCategoryMappingMapper documentCategoryMappingMapper,
            SnowflakeIdGenerator snowflakeIdGenerator) {
        this.documentMapper = documentMapper;
        this.tagMapper = tagMapper;
        this.categoryMapper = categoryMapper;
        this.documentTagMappingMapper = documentTagMappingMapper;
        this.documentCategoryMappingMapper = documentCategoryMappingMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    public List<DocumentPO> listDocuments(String keyword, String status, Long tagId, Long categoryId) {
        Set<Long> filteredIds = resolveFilteredDocumentIds(tagId, categoryId);
        if (filteredIds != null && filteredIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<DocumentPO> wrapper = new LambdaQueryWrapper<DocumentPO>()
                .isNull(DocumentPO::getDeletedAt)
                .eq(hasText(status), DocumentPO::getStatus, trimToNull(status))
                .in(filteredIds != null, DocumentPO::getId, filteredIds);
        if (hasText(keyword)) {
            String like = trimToNull(keyword);
            wrapper.and(query -> query
                    .like(DocumentPO::getTitle, like)
                    .or()
                    .like(DocumentPO::getDescription, like)
                    .or()
                    .like(DocumentPO::getContent, like));
        }
        wrapper.orderByDesc(DocumentPO::getUpdatedAt, DocumentPO::getCreatedAt);
        return documentMapper.selectList(wrapper);
    }

    @Override
    public DocumentPO getDocumentById(Long id) {
        if (id == null) {
            return null;
        }
        return documentMapper.selectOne(new LambdaQueryWrapper<DocumentPO>()
                .eq(DocumentPO::getId, id)
                .isNull(DocumentPO::getDeletedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(DocumentPO document, List<Long> tagIds, List<Long> categoryIds) {
        if (document.getId() == null) {
            document.setId(snowflakeIdGenerator.nextId());
        }
        applyDocumentDefaults(document);
        documentMapper.insert(document);
        replaceDocumentTags(document.getId(), tagIds);
        replaceDocumentCategories(document.getId(), categoryIds);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDocument(DocumentPO document, List<Long> tagIds, List<Long> categoryIds) {
        if (document == null || document.getId() == null) {
            return false;
        }
        applyDocumentDefaults(document);
        document.setUpdatedAt(OffsetDateTime.now());
        boolean updated = documentMapper.updateById(document) > 0;
        if (updated) {
            replaceDocumentTags(document.getId(), tagIds);
            replaceDocumentCategories(document.getId(), categoryIds);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        softDeleteDocumentMappings(id, now);
        return documentMapper.update(null, new LambdaUpdateWrapper<DocumentPO>()
                .eq(DocumentPO::getId, id)
                .isNull(DocumentPO::getDeletedAt)
                .set(DocumentPO::getUpdatedAt, now)
                .set(DocumentPO::getDeletedAt, now)) > 0;
    }

    @Override
    public List<DocumentTagMappingPO> listDocumentTagMappings(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        return documentTagMappingMapper.selectList(new LambdaQueryWrapper<DocumentTagMappingPO>()
                .in(DocumentTagMappingPO::getDocumentId, documentIds)
                .isNull(DocumentTagMappingPO::getDeletedAt));
    }

    @Override
    public List<DocumentCategoryMappingPO> listDocumentCategoryMappings(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        return documentCategoryMappingMapper.selectList(new LambdaQueryWrapper<DocumentCategoryMappingPO>()
                .in(DocumentCategoryMappingPO::getDocumentId, documentIds)
                .isNull(DocumentCategoryMappingPO::getDeletedAt));
    }

    @Override
    public List<TagPO> listTags(String keyword) {
        LambdaQueryWrapper<TagPO> wrapper = new LambdaQueryWrapper<TagPO>().isNull(TagPO::getDeletedAt);
        if (hasText(keyword)) {
            String like = trimToNull(keyword);
            wrapper.and(query -> query.like(TagPO::getTagCode, like).or().like(TagPO::getTagName, like));
        }
        wrapper.orderByAsc(TagPO::getTagName);
        return tagMapper.selectList(wrapper);
    }

    @Override
    public List<TagPO> listTagsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectList(new LambdaQueryWrapper<TagPO>()
                .in(TagPO::getId, ids)
                .isNull(TagPO::getDeletedAt));
    }

    @Override
    public Long createTag(TagPO tag) {
        if (tag.getId() == null) {
            tag.setId(snowflakeIdGenerator.nextId());
        }
        if (!hasText(tag.getTagCode())) {
            tag.setTagCode(slug(tag.getTagName()));
        }
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public boolean updateTag(TagPO tag) {
        tag.setUpdatedAt(OffsetDateTime.now());
        return tagMapper.updateById(tag) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        documentTagMappingMapper.update(null, new LambdaUpdateWrapper<DocumentTagMappingPO>()
                .eq(DocumentTagMappingPO::getTagId, id)
                .isNull(DocumentTagMappingPO::getDeletedAt)
                .set(DocumentTagMappingPO::getUpdatedAt, now)
                .set(DocumentTagMappingPO::getDeletedAt, now));
        return tagMapper.update(null, new LambdaUpdateWrapper<TagPO>()
                .eq(TagPO::getId, id)
                .isNull(TagPO::getDeletedAt)
                .set(TagPO::getUpdatedAt, now)
                .set(TagPO::getDeletedAt, now)) > 0;
    }

    @Override
    public List<CategoryPO> listCategories(String keyword) {
        LambdaQueryWrapper<CategoryPO> wrapper = new LambdaQueryWrapper<CategoryPO>().isNull(CategoryPO::getDeletedAt);
        if (hasText(keyword)) {
            String like = trimToNull(keyword);
            wrapper.and(query -> query.like(CategoryPO::getCategoryCode, like).or().like(CategoryPO::getCategoryName, like));
        }
        wrapper.orderByAsc(CategoryPO::getSortOrder, CategoryPO::getCategoryName);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public List<CategoryPO> listCategoriesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryPO>()
                .in(CategoryPO::getId, ids)
                .isNull(CategoryPO::getDeletedAt));
    }

    @Override
    public Long createCategory(CategoryPO category) {
        if (category.getId() == null) {
            category.setId(snowflakeIdGenerator.nextId());
        }
        if (!hasText(category.getCategoryCode())) {
            category.setCategoryCode(slug(category.getCategoryName()));
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public boolean updateCategory(CategoryPO category) {
        category.setUpdatedAt(OffsetDateTime.now());
        return categoryMapper.updateById(category) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        documentCategoryMappingMapper.update(null, new LambdaUpdateWrapper<DocumentCategoryMappingPO>()
                .eq(DocumentCategoryMappingPO::getCategoryId, id)
                .isNull(DocumentCategoryMappingPO::getDeletedAt)
                .set(DocumentCategoryMappingPO::getUpdatedAt, now)
                .set(DocumentCategoryMappingPO::getDeletedAt, now));
        return categoryMapper.update(null, new LambdaUpdateWrapper<CategoryPO>()
                .eq(CategoryPO::getId, id)
                .isNull(CategoryPO::getDeletedAt)
                .set(CategoryPO::getUpdatedAt, now)
                .set(CategoryPO::getDeletedAt, now)) > 0;
    }

    private Set<Long> resolveFilteredDocumentIds(Long tagId, Long categoryId) {
        Set<Long> filteredIds = null;
        if (tagId != null) {
            filteredIds = documentIdsForTag(tagId);
        }
        if (categoryId != null) {
            Set<Long> categoryIds = documentIdsForCategory(categoryId);
            if (filteredIds == null) {
                filteredIds = categoryIds;
            } else {
                filteredIds.retainAll(categoryIds);
            }
        }
        return filteredIds;
    }

    private Set<Long> documentIdsForTag(Long tagId) {
        List<DocumentTagMappingPO> rows = documentTagMappingMapper.selectList(new LambdaQueryWrapper<DocumentTagMappingPO>()
                .eq(DocumentTagMappingPO::getTagId, tagId)
                .isNull(DocumentTagMappingPO::getDeletedAt));
        Set<Long> ids = new LinkedHashSet<>();
        rows.forEach(row -> ids.add(row.getDocumentId()));
        return ids;
    }

    private Set<Long> documentIdsForCategory(Long categoryId) {
        List<DocumentCategoryMappingPO> rows = documentCategoryMappingMapper.selectList(
                new LambdaQueryWrapper<DocumentCategoryMappingPO>()
                        .eq(DocumentCategoryMappingPO::getCategoryId, categoryId)
                        .isNull(DocumentCategoryMappingPO::getDeletedAt));
        Set<Long> ids = new LinkedHashSet<>();
        rows.forEach(row -> ids.add(row.getDocumentId()));
        return ids;
    }

    private void replaceDocumentTags(Long documentId, List<Long> tagIds) {
        OffsetDateTime now = OffsetDateTime.now();
        documentTagMappingMapper.update(null, new LambdaUpdateWrapper<DocumentTagMappingPO>()
                .eq(DocumentTagMappingPO::getDocumentId, documentId)
                .isNull(DocumentTagMappingPO::getDeletedAt)
                .set(DocumentTagMappingPO::getUpdatedAt, now)
                .set(DocumentTagMappingPO::getDeletedAt, now));
        for (Long tagId : distinctIds(tagIds)) {
            DocumentTagMappingPO mapping = new DocumentTagMappingPO();
            mapping.setId(snowflakeIdGenerator.nextId());
            mapping.setDocumentId(documentId);
            mapping.setTagId(tagId);
            documentTagMappingMapper.insert(mapping);
        }
    }

    private void replaceDocumentCategories(Long documentId, List<Long> categoryIds) {
        OffsetDateTime now = OffsetDateTime.now();
        documentCategoryMappingMapper.update(null, new LambdaUpdateWrapper<DocumentCategoryMappingPO>()
                .eq(DocumentCategoryMappingPO::getDocumentId, documentId)
                .isNull(DocumentCategoryMappingPO::getDeletedAt)
                .set(DocumentCategoryMappingPO::getUpdatedAt, now)
                .set(DocumentCategoryMappingPO::getDeletedAt, now));
        for (Long categoryId : distinctIds(categoryIds)) {
            DocumentCategoryMappingPO mapping = new DocumentCategoryMappingPO();
            mapping.setId(snowflakeIdGenerator.nextId());
            mapping.setDocumentId(documentId);
            mapping.setCategoryId(categoryId);
            documentCategoryMappingMapper.insert(mapping);
        }
    }

    private void softDeleteDocumentMappings(Long documentId, OffsetDateTime now) {
        documentTagMappingMapper.update(null, new LambdaUpdateWrapper<DocumentTagMappingPO>()
                .eq(DocumentTagMappingPO::getDocumentId, documentId)
                .isNull(DocumentTagMappingPO::getDeletedAt)
                .set(DocumentTagMappingPO::getUpdatedAt, now)
                .set(DocumentTagMappingPO::getDeletedAt, now));
        documentCategoryMappingMapper.update(null, new LambdaUpdateWrapper<DocumentCategoryMappingPO>()
                .eq(DocumentCategoryMappingPO::getDocumentId, documentId)
                .isNull(DocumentCategoryMappingPO::getDeletedAt)
                .set(DocumentCategoryMappingPO::getUpdatedAt, now)
                .set(DocumentCategoryMappingPO::getDeletedAt, now));
    }

    private void applyDocumentDefaults(DocumentPO document) {
        if (!hasText(document.getContent())) {
            document.setContent("");
        }
        if (!hasText(document.getContentFormat())) {
            document.setContentFormat("markdown");
        }
        if (!hasText(document.getStatus())) {
            document.setStatus("draft");
        }
        if (!hasText(document.getExtra())) {
            document.setExtra("{}");
        }
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids.stream().filter(id -> id != null && id > 0).toList()));
    }

    private String slug(String value) {
        if (!hasText(value)) {
            return "item-" + snowflakeIdGenerator.nextId();
        }
        return value.trim().toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-").replaceAll("(^-|-$)", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
