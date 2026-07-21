package cn.cccs7.cynexus.core.application.controller;

import cn.cccs7.cynexus.common.core.result.Result;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionAskResponseDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryItemDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryListDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryQaItemDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionTaskDetailDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionTaskRefDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionProcessDTO;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionAskRequest;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionHistoryReorderRequest;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionHistorySaveRequest;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionRemoteParseRequest;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionProcessRequest;
import cn.cccs7.cynexus.core.application.service.MediaTranscriptionAppService;
import cn.cccs7.cynexus.core.application.service.MediaTranscriptionProcessAppService;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionStreamBO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/core/media-transcription")
public class MediaTranscriptionController {

    private final MediaTranscriptionAppService mediaTranscriptionAppService;
    private final MediaTranscriptionProcessAppService mediaTranscriptionProcessAppService;

    public MediaTranscriptionController(MediaTranscriptionAppService mediaTranscriptionAppService,
                                        MediaTranscriptionProcessAppService mediaTranscriptionProcessAppService) {
        this.mediaTranscriptionAppService = mediaTranscriptionAppService;
        this.mediaTranscriptionProcessAppService = mediaTranscriptionProcessAppService;
    }

    @PostMapping(value = "/tasks/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<MediaTranscriptionTaskRefDTO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(mediaTranscriptionAppService.upload(file));
    }

    @PostMapping("/tasks/remote-parse")
    public Result<MediaTranscriptionTaskRefDTO> remoteParse(@RequestBody MediaTranscriptionRemoteParseRequest request) {
        return Result.success(mediaTranscriptionAppService.remoteParse(request));
    }

    @PostMapping("/processes")
    public Result<MediaTranscriptionProcessDTO> submitProcess(
            @RequestBody MediaTranscriptionProcessRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(mediaTranscriptionProcessAppService.submit(request, idempotencyKey));
    }

    @GetMapping("/processes")
    public Result<Map<String, Object>> listProcesses(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(Map.of("items", mediaTranscriptionProcessAppService.list(limit)));
    }

    @GetMapping("/processes/{processId}")
    public Result<MediaTranscriptionProcessDTO> getProcess(@PathVariable String processId) {
        return Result.success(mediaTranscriptionProcessAppService.get(processId));
    }

    @PostMapping("/processes/{processId}/retry")
    public Result<MediaTranscriptionProcessDTO> retryProcess(@PathVariable String processId) {
        return Result.success(mediaTranscriptionProcessAppService.retry(processId));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<MediaTranscriptionTaskDetailDTO> getTask(@PathVariable("taskId") String taskId) {
        return Result.success(mediaTranscriptionAppService.getTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/ask")
    public Result<MediaTranscriptionAskResponseDTO> ask(
            @PathVariable("taskId") String taskId,
            @RequestBody MediaTranscriptionAskRequest request) {
        return Result.success(mediaTranscriptionAppService.ask(taskId, request == null ? null : request.getQuestion()));
    }

    @GetMapping(value = "/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable("taskId") String taskId) {
        MediaTranscriptionStreamBO stream = mediaTranscriptionAppService.openStream(taskId);
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = stream.getInputStream()) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.parseMediaType(
                        stream.getContentType() == null || stream.getContentType().isBlank()
                                ? MediaType.TEXT_EVENT_STREAM_VALUE
                                : stream.getContentType()))
                .body(body);
    }

    @GetMapping("/tasks/{taskId}/export-markdown")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable("taskId") String taskId) {
        MediaTranscriptionBinaryBO payload = mediaTranscriptionAppService.exportMarkdown(taskId);
        String fileName = payload.getFileName() == null || payload.getFileName().isBlank()
                ? "transcription.md"
                : payload.getFileName();
        String contentType = payload.getContentType() == null || payload.getContentType().isBlank()
                ? "text/markdown; charset=utf-8"
                : payload.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(payload.getContent() == null ? new byte[0] : payload.getContent());
    }

    @GetMapping("/history")
    public Result<MediaTranscriptionHistoryListDTO> listHistory() {
        return Result.success(mediaTranscriptionAppService.listHistory());
    }

    @PostMapping("/history")
    public Result<MediaTranscriptionHistoryItemDTO> saveHistory(@RequestBody MediaTranscriptionHistorySaveRequest request) {
        return Result.success(mediaTranscriptionAppService.saveHistory(request == null ? null : request.getTaskId()));
    }

    @PostMapping("/history/reorder")
    public Result<MediaTranscriptionHistoryListDTO> reorderHistory(@RequestBody MediaTranscriptionHistoryReorderRequest request) {
        return Result.success(mediaTranscriptionAppService.reorderHistory(request == null ? null : request.getOrderedIds()));
    }

    @GetMapping("/history/{historyId}")
    public Result<Map<String, Object>> getHistory(@PathVariable("historyId") String historyId) {
        return Result.success(mediaTranscriptionAppService.getHistory(historyId));
    }

    @DeleteMapping("/history/{historyId}")
    public Result<Map<String, Object>> deleteHistory(@PathVariable("historyId") String historyId) {
        return Result.success(mediaTranscriptionAppService.deleteHistory(historyId));
    }

    @PostMapping("/history/{historyId}/ask")
    public Result<Map<String, MediaTranscriptionHistoryQaItemDTO>> askHistory(
            @PathVariable("historyId") String historyId,
            @RequestBody MediaTranscriptionAskRequest request) {
        return Result.success(Map.of("item", mediaTranscriptionAppService.askHistory(
                historyId,
                request == null ? null : request.getQuestion())));
    }
}
