package cn.cccs7.cynexus.knowledgebase.api.feign;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeTagDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeCategoryDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cynexus-knowledgebase", contextId = "knowledgeBaseFeignClient")
public interface KnowledgeBaseFeignClient {

    @GetMapping("/api/knowledge-base/documents/{id}")
    Result<KnowledgeDocumentDTO> getDocument(@PathVariable("id") Long id);

    @GetMapping("/api/knowledge-base/documents")
    Result<List<KnowledgeDocumentDTO>> listDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "categoryId", required = false) Long categoryId);

    @PostMapping("/api/knowledge-base/documents")
    Result<Long> createDocument(@RequestBody KnowledgeDocumentDTO document);

    @org.springframework.web.bind.annotation.PutMapping("/api/knowledge-base/documents")
    Result<Boolean> updateDocument(@RequestBody KnowledgeDocumentDTO document);

    @GetMapping("/api/knowledge-base/tags")
    Result<List<KnowledgeTagDTO>> listTags(@RequestParam(value = "keyword", required = false) String keyword);

    @PostMapping("/api/knowledge-base/tags")
    Result<Long> createTag(@RequestBody KnowledgeTagDTO tag);

    @GetMapping("/api/knowledge-base/categories")
    Result<List<KnowledgeCategoryDTO>> listCategories(@RequestParam(value = "keyword", required = false) String keyword);

    @PostMapping("/api/knowledge-base/categories")
    Result<Long> createCategory(@RequestBody KnowledgeCategoryDTO category);
}
