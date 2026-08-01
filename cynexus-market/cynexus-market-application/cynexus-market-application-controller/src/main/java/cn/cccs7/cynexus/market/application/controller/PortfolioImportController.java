package cn.cccs7.cynexus.market.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportTaskDTO;
import cn.cccs7.cynexus.market.api.dto.PortfolioImportReviewDTO;
import cn.cccs7.cynexus.market.api.request.PortfolioImportConfirmRequest;
import cn.cccs7.cynexus.market.application.service.PortfolioImportAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{taskId}")
    public Result<PortfolioImportTaskDTO> get(@PathVariable long taskId) {
        return Result.success(service.get(taskId));
    }

    @GetMapping("/{taskId}/review")
    public Result<PortfolioImportReviewDTO> review(@PathVariable long taskId) {
        return Result.success(service.review(taskId));
    }

    @PostMapping("/{taskId}/confirm")
    public Result<PortfolioImportTaskDTO> confirm(@PathVariable long taskId,
                                                 @RequestBody(required = false) PortfolioImportConfirmRequest request) {
        return Result.success(service.confirm(taskId, request));
    }
}
