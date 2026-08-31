package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionAskDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionEnrichmentDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionMetadataDTO;
import cn.cccs7.cynexus.ai.api.dto.MediaTranscriptionTextDTO;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionAskRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionMetadataRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionPolishRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionSummaryRequest;
import cn.cccs7.cynexus.ai.api.request.MediaTranscriptionEnrichmentRequest;
import cn.cccs7.cynexus.ai.application.service.MediaTranscriptionAiAppService;
import cn.cccs7.cynexus.common.core.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/media-transcription")
public class MediaTranscriptionAiController {

    private final MediaTranscriptionAiAppService appService;

    public MediaTranscriptionAiController(MediaTranscriptionAiAppService appService) {
        this.appService = appService;
    }

    @PostMapping("/polish")
    public Result<MediaTranscriptionTextDTO> polish(@RequestBody MediaTranscriptionPolishRequest request) {
        return Result.success(appService.polish(request));
    }

    @PostMapping("/enrich")
    public Result<MediaTranscriptionEnrichmentDTO> enrich(@RequestBody MediaTranscriptionEnrichmentRequest request) {
        return Result.success(appService.enrich(request));
    }

    @PostMapping("/summary")
    public Result<MediaTranscriptionTextDTO> summary(@RequestBody MediaTranscriptionSummaryRequest request) {
        return Result.success(appService.summary(request));
    }

    @PostMapping("/metadata")
    public Result<MediaTranscriptionMetadataDTO> metadata(@RequestBody MediaTranscriptionMetadataRequest request) {
        return Result.success(appService.metadata(request));
    }

    @PostMapping("/ask")
    public Result<MediaTranscriptionAskDTO> ask(@RequestBody MediaTranscriptionAskRequest request) {
        return Result.success(appService.ask(request));
    }
}
