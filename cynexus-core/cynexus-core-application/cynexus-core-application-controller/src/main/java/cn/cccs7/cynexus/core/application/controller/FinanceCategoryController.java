package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceCategoryDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceCategoryRequest;
import cn.cccs7.cynexus.core.application.service.FinanceLedgerAppService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/finance/categories")
public class FinanceCategoryController {
    private final FinanceLedgerAppService service;

    public FinanceCategoryController(FinanceLedgerAppService service) { this.service = service; }

    @GetMapping
    public Result<List<FinanceCategoryDTO>> list(@RequestParam(defaultValue = "false") boolean includeArchived) { return Result.success(service.listCategories(includeArchived)); }

    @PostMapping
    public Result<FinanceCategoryDTO> save(@RequestBody FinanceCategoryRequest request) { return Result.success(service.saveCategory(request)); }
}
