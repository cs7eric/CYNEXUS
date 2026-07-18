package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCaptureDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAiCaptureConfirmRequest;
import cn.cccs7.cynexus.core.application.service.FinanceAiCaptureAppService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/core/finance/ai-captures")
public class FinanceAiCaptureController {
    private final FinanceAiCaptureAppService service;

    public FinanceAiCaptureController(FinanceAiCaptureAppService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FinanceAiCaptureDTO> parse(@RequestParam("images") MultipartFile[] images) { return Result.success(service.parse(images)); }

    @GetMapping("/{id}")
    public Result<FinanceAiCaptureDTO> get(@PathVariable Long id) { return Result.success(service.get(id)); }

    @PostMapping("/{id}/confirm")
    public Result<FinanceAiCaptureDTO> confirm(@PathVariable Long id, @RequestBody FinanceAiCaptureConfirmRequest request) { return Result.success(service.confirm(id, request)); }

    @DeleteMapping("/{id}")
    public Result<Boolean> archive(@PathVariable Long id) { return Result.success(service.archive(id)); }
}
