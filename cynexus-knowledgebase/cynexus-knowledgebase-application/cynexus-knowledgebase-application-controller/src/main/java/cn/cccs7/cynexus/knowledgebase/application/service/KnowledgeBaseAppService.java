package cn.cccs7.cynexus.knowledgebase.application.service;

import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeCategoryDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeTagDTO;
import cn.cccs7.cynexus.knowledgebase.application.convert.KnowledgeCategoryDTOConverter;
import cn.cccs7.cynexus.knowledgebase.application.convert.KnowledgeDocumentDTOConverter;
import cn.cccs7.cynexus.knowledgebase.application.convert.KnowledgeTagDTOConverter;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase.KnowledgeDocumentLifecycleDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseAppService {

    private final KnowledgeDocumentLifecycleDomainService lifecycleDomainService;

    public KnowledgeBaseAppService(KnowledgeDocumentLifecycleDomainService lifecycleDomainService) {
        this.lifecycleDomainService = lifecycleDomainService;
    }

    public List<KnowledgeDocumentDTO> listDocuments(String keyword, String status, Long tagId, Long categoryId) {
        return KnowledgeDocumentDTOConverter.INSTANCE.toSourceList(
                lifecycleDomainService.listDocuments(keyword, status, tagId, categoryId));
    }

    public KnowledgeDocumentDTO findDocumentById(Long id) {
        KnowledgeDocumentBO document = lifecycleDomainService.findDocumentById(id);
        return document == null ? null : KnowledgeDocumentDTOConverter.INSTANCE.toSource(document);
    }

    public Long createDocument(KnowledgeDocumentDTO dto) {
        return lifecycleDomainService.createDocument(KnowledgeDocumentDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateDocument(KnowledgeDocumentDTO dto) {
        return lifecycleDomainService.updateDocument(KnowledgeDocumentDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteDocument(Long id) {
        return lifecycleDomainService.deleteDocument(id);
    }

    public List<KnowledgeTagDTO> listTags(String keyword) {
        return KnowledgeTagDTOConverter.INSTANCE.toSourceList(lifecycleDomainService.listTags(keyword));
    }

    public Long createTag(KnowledgeTagDTO dto) {
        return lifecycleDomainService.createTag(KnowledgeTagDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateTag(KnowledgeTagDTO dto) {
        return lifecycleDomainService.updateTag(KnowledgeTagDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteTag(Long id) {
        return lifecycleDomainService.deleteTag(id);
    }

    public List<KnowledgeCategoryDTO> listCategories(String keyword) {
        return KnowledgeCategoryDTOConverter.INSTANCE.toSourceList(lifecycleDomainService.listCategories(keyword));
    }

    public Long createCategory(KnowledgeCategoryDTO dto) {
        return lifecycleDomainService.createCategory(KnowledgeCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean updateCategory(KnowledgeCategoryDTO dto) {
        return lifecycleDomainService.updateCategory(KnowledgeCategoryDTOConverter.INSTANCE.toTarget(dto));
    }

    public boolean deleteCategory(Long id) {
        return lifecycleDomainService.deleteCategory(id);
    }
}
