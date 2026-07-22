package cn.cccs7.cynexus.knowledgebase.domain.service.knowledgebase;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeCategoryBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeTagBO;
import java.util.List;

/**
 * Coordinates the knowledge document lifecycle and its related metadata.
 *
 * <p>Document writes and index changes belong to one domain use-case boundary;
 * callers must not persist a document and refresh its index independently.</p>
 */
public interface KnowledgeDocumentLifecycleDomainService {

    List<KnowledgeDocumentBO> listDocuments(String keyword, String status, Long tagId, Long categoryId);

    KnowledgeDocumentBO findDocumentById(Long id);

    Long createDocument(KnowledgeDocumentBO document);

    boolean updateDocument(KnowledgeDocumentBO document);

    boolean deleteDocument(Long id);

    List<KnowledgeTagBO> listTags(String keyword);

    Long createTag(KnowledgeTagBO tag);

    boolean updateTag(KnowledgeTagBO tag);

    boolean deleteTag(Long id);

    List<KnowledgeCategoryBO> listCategories(String keyword);

    Long createCategory(KnowledgeCategoryBO category);

    boolean updateCategory(KnowledgeCategoryBO category);

    boolean deleteCategory(Long id);
}
