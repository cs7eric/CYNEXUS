package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.FinanceAiCaptureEnqueueDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionEnqueueDTO;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionProcessRequest;
import cn.cccs7.cynexus.core.application.service.FinanceAiCaptureAppService;
import cn.cccs7.cynexus.core.application.service.MediaTranscriptionProcessAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/workflows")
public class ApiWorkflowIngressController {

    private final MediaTranscriptionProcessAppService mediaTranscriptionProcessAppService;
    private final FinanceAiCaptureAppService financeAiCaptureAppService;

    public ApiWorkflowIngressController(MediaTranscriptionProcessAppService mediaTranscriptionProcessAppService,
                                        FinanceAiCaptureAppService financeAiCaptureAppService) {
        this.mediaTranscriptionProcessAppService = mediaTranscriptionProcessAppService;
        this.financeAiCaptureAppService = financeAiCaptureAppService;
    }

    @PostMapping("/media-transcription")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<MediaTranscriptionEnqueueDTO> mediaTranscription(
            @RequestBody MediaTranscriptionProcessRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(mediaTranscriptionProcessAppService.enqueue(request, idempotencyKey));
    }

    @PostMapping(value = "/finance-ai-capture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<FinanceAiCaptureEnqueueDTO> financeAiCapture(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(financeAiCaptureAppService.enqueue(image, "auto", idempotencyKey));
    }
}
