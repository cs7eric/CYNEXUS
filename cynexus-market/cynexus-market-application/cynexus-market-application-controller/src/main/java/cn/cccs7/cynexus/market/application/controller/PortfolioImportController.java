package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskPageDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportReviewDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportProcessHistoryDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportCandidateDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioImportConfirmRequest;
import cn.cccs7.cynexus.market.api.request.PortfolioCandidateEditRequest;
import cn.cccs7.cynexus.market.application.service.PortfolioImportAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/market/portfolio/imports")
public class PortfolioImportController {
    private final PortfolioImportAppService service;

    public PortfolioImportController(PortfolioImportAppService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<PortfolioImportTaskDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(service.upload(file, idempotencyKey));
    }

    @GetMapping
    public Result<PortfolioImportTaskPageDTO> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return Result.success(service.list(page, pageSize));
    }

    @GetMapping("/{taskId}")
    public Result<PortfolioImportTaskDTO> get(@PathVariable long taskId) {
        return Result.success(service.get(taskId));
    }

    @PostMapping("/{taskId}/retry")
    public Result<PortfolioImportTaskDTO> retry(@PathVariable long taskId) {
        return Result.success(service.retry(taskId));
    }

    @GetMapping("/{taskId}/review")
    public Result<PortfolioImportReviewDTO> review(@PathVariable long taskId) {
        return Result.success(service.review(taskId));
    }

    @GetMapping("/{taskId}/history")
    public Result<PortfolioImportProcessHistoryDTO> history(@PathVariable long taskId) {
        return Result.success(service.history(taskId));
    }

    @PatchMapping("/{taskId}/candidates/{candidateId}")
    public Result<PortfolioImportCandidateDTO> editCandidate(@PathVariable long taskId,
                                                              @PathVariable long candidateId,
                                                              @RequestBody PortfolioCandidateEditRequest request) {
        return Result.success(service.editCandidate(taskId, candidateId, request));
    }

    @PostMapping("/{taskId}/confirm")
    public Result<PortfolioImportTaskDTO> confirm(@PathVariable long taskId,
                                                 @RequestBody(required = false) PortfolioImportConfirmRequest request) {
        return Result.success(service.confirm(taskId, request));
    }
}
