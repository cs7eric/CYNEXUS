package cn.cccs7.cynexus.knowledgebase.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeCategoryDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeDocumentDTO;
import cn.cccs7.cynexus.knowledgebase.api.dto.KnowledgeTagDTO;
import cn.cccs7.cynexus.knowledgebase.application.service.KnowledgeBaseAppService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    private final KnowledgeBaseAppService appService;

    public KnowledgeBaseController(KnowledgeBaseAppService appService) {
        this.appService = appService;
    }

    @GetMapping("/documents")
    public Result<List<KnowledgeDocumentDTO>> listDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return Result.success(appService.listDocuments(keyword, status, tagId, categoryId));
    }

    @GetMapping("/documents/{id}")
    public Result<KnowledgeDocumentDTO> getDocument(@PathVariable("id") Long id) {
        return Result.success(appService.findDocumentById(id));
    }

    @PostMapping("/documents")
    public Result<Long> createDocument(@RequestBody KnowledgeDocumentDTO dto) {
        return Result.success(appService.createDocument(dto));
    }

    @PutMapping("/documents")
    public Result<Boolean> updateDocument(@RequestBody KnowledgeDocumentDTO dto) {
        return Result.success(appService.updateDocument(dto));
    }

    @DeleteMapping("/documents/{id}")
    public Result<Boolean> deleteDocument(@PathVariable("id") Long id) {
        return Result.success(appService.deleteDocument(id));
    }

    @GetMapping("/tags")
    public Result<List<KnowledgeTagDTO>> listTags(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(appService.listTags(keyword));
    }

    @PostMapping("/tags")
    public Result<Long> createTag(@RequestBody KnowledgeTagDTO dto) {
        return Result.success(appService.createTag(dto));
    }

    @PutMapping("/tags")
    public Result<Boolean> updateTag(@RequestBody KnowledgeTagDTO dto) {
        return Result.success(appService.updateTag(dto));
    }

    @DeleteMapping("/tags/{id}")
    public Result<Boolean> deleteTag(@PathVariable("id") Long id) {
        return Result.success(appService.deleteTag(id));
    }

    @GetMapping("/categories")
    public Result<List<KnowledgeCategoryDTO>> listCategories(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(appService.listCategories(keyword));
    }

    @PostMapping("/categories")
    public Result<Long> createCategory(@RequestBody KnowledgeCategoryDTO dto) {
        return Result.success(appService.createCategory(dto));
    }

    @PutMapping("/categories")
    public Result<Boolean> updateCategory(@RequestBody KnowledgeCategoryDTO dto) {
        return Result.success(appService.updateCategory(dto));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Boolean> deleteCategory(@PathVariable("id") Long id) {
        return Result.success(appService.deleteCategory(id));
    }
}
