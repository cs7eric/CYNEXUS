package cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.impl;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeBaseIndexDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.knowledgebase.KnowledgeBaseDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.KnowledgeDocumentLifecycleDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDocumentLifecycleDomainServiceImpl implements KnowledgeDocumentLifecycleDomainService {

    private final KnowledgeBaseDomainService knowledgeBaseDomainService;
    private final KnowledgeBaseIndexDomainService knowledgeBaseIndexDomainService;

    public KnowledgeDocumentLifecycleDomainServiceImpl(
            KnowledgeBaseDomainService knowledgeBaseDomainService,
            KnowledgeBaseIndexDomainService knowledgeBaseIndexDomainService) {
        this.knowledgeBaseDomainService = knowledgeBaseDomainService;
        this.knowledgeBaseIndexDomainService = knowledgeBaseIndexDomainService;
    }

    @Override
    public List<KnowledgeDocumentBO> listDocuments(String keyword, String status, Long tagId, Long categoryId) {
        return knowledgeBaseDomainService.listDocuments(keyword, status, tagId, categoryId);
    }

    @Override
    public KnowledgeDocumentBO findDocumentById(Long id) {
        return knowledgeBaseDomainService.findDocumentById(id);
    }

    @Override
    public Long createDocument(KnowledgeDocumentBO document) {
        Long documentId = knowledgeBaseDomainService.createDocument(document);
        knowledgeBaseIndexDomainService.refreshDocumentIndex(
                knowledgeBaseDomainService.findDocumentById(documentId));
        return documentId;
    }

    @Override
    public boolean updateDocument(KnowledgeDocumentBO document) {
        boolean updated = knowledgeBaseDomainService.updateDocument(document);
        if (updated && document != null && document.getId() != null) {
            knowledgeBaseIndexDomainService.refreshDocumentIndex(
                    knowledgeBaseDomainService.findDocumentById(document.getId()));
        }
        return updated;
    }

    @Override
    public boolean deleteDocument(Long id) {
        boolean deleted = knowledgeBaseDomainService.deleteDocument(id);
        if (deleted) {
            knowledgeBaseIndexDomainService.deleteDocumentIndex(id);
        }
        return deleted;
    }

    @Override
    public List<KnowledgeTagBO> listTags(String keyword) {
        return knowledgeBaseDomainService.listTags(keyword);
    }

    @Override
    public Long createTag(KnowledgeTagBO tag) {
        return knowledgeBaseDomainService.createTag(tag);
    }

    @Override
    public boolean updateTag(KnowledgeTagBO tag) {
        return knowledgeBaseDomainService.updateTag(tag);
    }

    @Override
    public boolean deleteTag(Long id) {
        return knowledgeBaseDomainService.deleteTag(id);
    }

    @Override
    public List<KnowledgeCategoryBO> listCategories(String keyword) {
        return knowledgeBaseDomainService.listCategories(keyword);
    }

    @Override
    public Long createCategory(KnowledgeCategoryBO category) {
        return knowledgeBaseDomainService.createCategory(category);
    }

    @Override
    public boolean updateCategory(KnowledgeCategoryBO category) {
        return knowledgeBaseDomainService.updateCategory(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        return knowledgeBaseDomainService.deleteCategory(id);
    }
}
