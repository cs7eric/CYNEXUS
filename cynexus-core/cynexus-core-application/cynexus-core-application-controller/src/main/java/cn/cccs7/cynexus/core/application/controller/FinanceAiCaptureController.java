package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCaptureDTO;
import cn.cccs7.cynexus.core.api.dto.finance.FinanceAiCapturePageDTO;
import cn.cccs7.cynexus.core.application.request.finance.FinanceAiCaptureConfirmRequest;
import cn.cccs7.cynexus.core.application.service.FinanceAiCaptureAppService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public Result<FinanceAiCaptureDTO> parse(@RequestParam(value = "image", required = false) MultipartFile image,
                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(service.parse(image, "auto", idempotencyKey));
    }

    @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FinanceAiCaptureDTO> review(@RequestParam(value = "image", required = false) MultipartFile image,
                                              @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(service.parse(image, "review", idempotencyKey));
    }

    @GetMapping("/{id}")
    public Result<FinanceAiCaptureDTO> get(@PathVariable Long id) { return Result.success(service.get(id)); }

    @GetMapping("/{id}/images/{imageIndex}")
    public ResponseEntity<byte[]> image(@PathVariable Long id, @PathVariable int imageIndex) {
        var image = service.image(id, imageIndex);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.content().length).body(image.content());
    }

    @GetMapping
    public Result<FinanceAiCapturePageDTO> history(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String status) {
        return Result.success(service.history(limit, offset, status));
    }

    @PostMapping("/{id}/confirm")
    public Result<FinanceAiCaptureDTO> confirm(@PathVariable Long id, @RequestBody FinanceAiCaptureConfirmRequest request) { return Result.success(service.confirm(id, request)); }

    @PostMapping("/{id}/retry")
    public Result<FinanceAiCaptureDTO> retry(@PathVariable Long id) { return Result.success(service.retry(id)); }

    @DeleteMapping("/{id}")
    public Result<Boolean> archive(@PathVariable Long id) { return Result.success(service.archive(id)); }
}
