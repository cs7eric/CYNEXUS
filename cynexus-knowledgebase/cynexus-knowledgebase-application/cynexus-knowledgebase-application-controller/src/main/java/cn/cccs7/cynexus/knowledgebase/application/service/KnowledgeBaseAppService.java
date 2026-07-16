package cn.cccs7.cynexus.knowledgebase.application.service;

import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeCategoryDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeTagDTO;
import cn.cccs7.cynexus.knowledgebase.application.convert.KnowledgeCategoryDTOConverter;
import cn.cccs7.cynexus.knowledgebase.application.convert.KnowledgeDocumentDTOConverter;
import cn.cccs7.cynexus.knowledgebase.application.convert.KnowledgeTagDTOConverter;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.knowledgebase.KnowledgeBaseDomainService;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeBaseIndexDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseAppService {

    private final KnowledgeBaseDomainService domainService;
    private final KnowledgeBaseIndexDomainService indexDomainService;

    public KnowledgeBaseAppService(KnowledgeBaseDomainService domainService,
                                   KnowledgeBaseIndexDomainService indexDomainService) {
        this.domainService = domainService;
        this.indexDomainService = indexDomainService;
    }

    public List<KnowledgeDocumentDTO> listDocuments(String keyword, String status, Long tagId, Long categoryId) {
        return KnowledgeDocumentDTOConverter.INSTANCE.toSourceList(
                domainService.listDocuments(keyword, status, tagId, categoryId));
    }

    public KnowledgeDocumentDTO findDocumentById(Long id) {
        KnowledgeDocumentBO document = domainService.findDocumentById(id);
        return document == null ? null : KnowledgeDocumentDTOConverter.INSTANCE.toSource(document);
    }

    public Long createDocument(KnowledgeDocumentDTO dto) {
        Long id = domainService.createDocument(KnowledgeDocumentDTOConverter.INSTANCE.toTarget(dto));
        indexDomainService.refreshDocumentIndex(domainService.findDocumentById(id));
        return id;
    }

    public boolean updateDocument(KnowledgeDocumentDTO dto) {
        boolean updated = domainService.updateDocument(KnowledgeDocumentDTOConverter.INSTANCE.toTarget(dto));
        if (updated && dto != null && dto.getId() != null) {
            indexDomainService.refreshDocumentIndex(domainService.findDocumentById(dto.getId()));
        }
        return updated;
    }

    public boolean deleteDocument(Long id) {
        boolean deleted = domainService.deleteDocument(id);
        if (deleted) {
            indexDomainService.deleteDocumentIndex(id);
        }
        return deleted;
    }

    public List<KnowledgeTagDTO> listTags(String keyword) {
        return KnowledgeTagDTOConverter.INSTANCE.toSourceList(domainService.listTags(keyword));
    }

    public Long createTag(KnowledgeTagDTO dto) {
        return domainService.createTag(KnowledgeTagDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateTag(KnowledgeTagDTO dto) {
        return domainService.updateTag(KnowledgeTagDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteTag(Long id) {
        return domainService.deleteTag(id);
    }

    public List<KnowledgeCategoryDTO> listCategories(String keyword) {
        return KnowledgeCategoryDTOConverter.INSTANCE.toSourceList(domainService.listCategories(keyword));
    }

    public Long createCategory(KnowledgeCategoryDTO dto) {
        return domainService.createCategory(KnowledgeCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateCategory(KnowledgeCategoryDTO dto) {
        return domainService.updateCategory(KnowledgeCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteCategory(Long id) {
        return domainService.deleteCategory(id);
    }
}
