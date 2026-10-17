package cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.impl;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeBaseIndexDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeDocumentIndexEventPublisher;
import cn.cccs7.cynexus.knowledgebase.domain.knowledgebase.KnowledgeBaseDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.KnowledgeDocumentLifecycleDomainService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDocumentLifecycleDomainServiceImpl implements KnowledgeDocumentLifecycleDomainService {
    private static final String DEFAULT_TENANT = "default";
    private static final String DEFAULT_OWNER = "default";

    private final KnowledgeBaseDomainService knowledgeBaseDomainService;
    private final KnowledgeBaseIndexDomainService knowledgeBaseIndexDomainService;
    private final KnowledgeDocumentIndexEventPublisher indexEventPublisher;

    public KnowledgeDocumentLifecycleDomainServiceImpl(
            KnowledgeBaseDomainService knowledgeBaseDomainService,
            KnowledgeBaseIndexDomainService knowledgeBaseIndexDomainService) {
        this(knowledgeBaseDomainService, knowledgeBaseIndexDomainService, new ObjectProvider<KnowledgeDocumentIndexEventPublisher>() {
            private final KnowledgeDocumentIndexEventPublisher noop = new KnowledgeDocumentIndexEventPublisher() {
                @Override public void documentChanged(KnowledgeDocumentBO document) { }
                @Override public void documentDeleted(Long documentId) { }
            };
            @Override public KnowledgeDocumentIndexEventPublisher getObject(Object... args) { return noop; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfAvailable() { return noop; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfUnique() { return noop; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfAvailable(java.util.function.Supplier<KnowledgeDocumentIndexEventPublisher> supplier) { return noop; }
            @Override public KnowledgeDocumentIndexEventPublisher getIfUnique(java.util.function.Supplier<KnowledgeDocumentIndexEventPublisher> supplier) { return noop; }
            @Override public java.util.stream.Stream<KnowledgeDocumentIndexEventPublisher> stream() { return java.util.stream.Stream.of(noop); }
            @Override public java.util.stream.Stream<KnowledgeDocumentIndexEventPublisher> orderedStream() { return java.util.stream.Stream.of(noop); }
        });
    }

    @Autowired
    public KnowledgeDocumentLifecycleDomainServiceImpl(
            KnowledgeBaseDomainService knowledgeBaseDomainService,
            KnowledgeBaseIndexDomainService knowledgeBaseIndexDomainService,
            ObjectProvider<KnowledgeDocumentIndexEventPublisher> indexEventPublisher) {
        this.knowledgeBaseDomainService = knowledgeBaseDomainService;
        this.knowledgeBaseIndexDomainService = knowledgeBaseIndexDomainService;
        this.indexEventPublisher = indexEventPublisher.getIfAvailable(() -> new KnowledgeDocumentIndexEventPublisher() {
            @Override public void documentChanged(KnowledgeDocumentBO document) { }
            @Override public void documentDeleted(Long documentId) { }
        });
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
        applyIdentityDefaults(document);
        Long documentId = knowledgeBaseDomainService.createDocument(document);
        KnowledgeDocumentBO created = knowledgeBaseDomainService.findDocumentById(documentId);
        knowledgeBaseIndexDomainService.refreshDocumentIndex(created);
        indexEventPublisher.documentChanged(created);
        return documentId;
    }

    @Override
    public boolean updateDocument(KnowledgeDocumentBO document) {
        preserveExistingIdentity(document);
        boolean updated = knowledgeBaseDomainService.updateDocument(document);
        if (updated && document != null && document.getId() != null) {
            KnowledgeDocumentBO updatedDocument = knowledgeBaseDomainService.findDocumentById(document.getId());
            knowledgeBaseIndexDomainService.refreshDocumentIndex(updatedDocument);
            indexEventPublisher.documentChanged(updatedDocument);
        }
        return updated;
    }

    @Override
    public boolean deleteDocument(Long id) {
        boolean deleted = knowledgeBaseDomainService.deleteDocument(id);
        if (deleted) {
            knowledgeBaseIndexDomainService.deleteDocumentIndex(id);
            indexEventPublisher.documentDeleted(id);
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

    private void applyIdentityDefaults(KnowledgeDocumentBO document) {
        if (document == null) return;
        if (document.getTenantKey() == null || document.getTenantKey().isBlank()) document.setTenantKey(DEFAULT_TENANT);
        if (document.getOwnerKey() == null || document.getOwnerKey().isBlank()) document.setOwnerKey(DEFAULT_OWNER);
    }

    private void preserveExistingIdentity(KnowledgeDocumentBO document) {
        if (document == null || document.getId() == null) {
            applyIdentityDefaults(document);
            return;
        }
        KnowledgeDocumentBO existing = knowledgeBaseDomainService.findDocumentById(document.getId());
        if (document.getTenantKey() == null || document.getTenantKey().isBlank()) {
            document.setTenantKey(existing == null ? DEFAULT_TENANT : existing.getTenantKey());
        }
        if (document.getOwnerKey() == null || document.getOwnerKey().isBlank()) {
            document.setOwnerKey(existing == null ? DEFAULT_OWNER : existing.getOwnerKey());
        }
        applyIdentityDefaults(document);
    }
}
