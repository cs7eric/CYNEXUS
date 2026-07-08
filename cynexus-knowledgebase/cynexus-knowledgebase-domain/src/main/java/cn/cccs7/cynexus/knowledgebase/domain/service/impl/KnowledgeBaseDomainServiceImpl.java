package cn.cccs7.cynexus.knowledgebase.domain.service.impl;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import cn.cccs7.cynexus.knowledgebase.domain.repository.KnowledgeBaseRepository;
import cn.cccs7.cynexus.knowledgebase.domain.service.KnowledgeBaseDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseDomainServiceImpl implements KnowledgeBaseDomainService {

    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseDomainServiceImpl(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<KnowledgeDocumentBO> listDocuments(String keyword, String status, Long tagId, Long categoryId) {
        return repository.listDocuments(keyword, status, tagId, categoryId);
    }

    @Override
    public KnowledgeDocumentBO findDocumentById(Long id) {
        return repository.findDocumentById(id);
    }

    @Override
    public Long createDocument(KnowledgeDocumentBO document) {
        validateDocument(document);
        return repository.createDocument(document);
    }

    @Override
    public boolean updateDocument(KnowledgeDocumentBO document) {
        validateDocument(document);
        return repository.updateDocument(document);
    }

    @Override
    public boolean deleteDocument(Long id) {
        return repository.deleteDocument(id);
    }

    @Override
    public List<KnowledgeTagBO> listTags(String keyword) {
        return repository.listTags(keyword);
    }

    @Override
    public Long createTag(KnowledgeTagBO tag) {
        return repository.createTag(tag);
    }

    @Override
    public boolean updateTag(KnowledgeTagBO tag) {
        return repository.updateTag(tag);
    }

    @Override
    public boolean deleteTag(Long id) {
        return repository.deleteTag(id);
    }

    @Override
    public List<KnowledgeCategoryBO> listCategories(String keyword) {
        return repository.listCategories(keyword);
    }

    @Override
    public Long createCategory(KnowledgeCategoryBO category) {
        return repository.createCategory(category);
    }

    @Override
    public boolean updateCategory(KnowledgeCategoryBO category) {
        return repository.updateCategory(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        return repository.deleteCategory(id);
    }

    private void validateDocument(KnowledgeDocumentBO document) {
        if (document == null || document.getTitle() == null || document.getTitle().isBlank()) {
            throw new IllegalArgumentException("document title is required");
        }
    }
}
