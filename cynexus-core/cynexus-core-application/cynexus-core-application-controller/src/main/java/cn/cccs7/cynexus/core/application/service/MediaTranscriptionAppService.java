package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.common.core.exception.BizException;
import cn.cccs7.cynexus.common.core.exception.ErrorCode;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionAskResponseDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryItemDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryListDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionHistoryQaItemDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionMetadataDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionProgressDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionTaskDetailDTO;
import cn.cccs7.cynexus.core.application.dto.MediaTranscriptionTaskRefDTO;
import cn.cccs7.cynexus.core.application.request.MediaTranscriptionRemoteParseRequest;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProgressBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionStreamBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionHistoryDomainService;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionHistoryQaBO;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionTaskDomainService;
import cn.cccs7.cynexus.core.domain.mediatranscription.MediaTranscriptionTaskInteractionDomainService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaTranscriptionAppService {

    private final MediaTranscriptionTaskDomainService taskDomainService;
    private final MediaTranscriptionTaskInteractionDomainService taskInteractionDomainService;
    private final MediaTranscriptionHistoryDomainService historyDomainService;

    public MediaTranscriptionAppService(MediaTranscriptionTaskDomainService taskDomainService,
                                        MediaTranscriptionTaskInteractionDomainService taskInteractionDomainService,
                                        MediaTranscriptionHistoryDomainService historyDomainService) {
        this.taskDomainService = taskDomainService;
        this.taskInteractionDomainService = taskInteractionDomainService;
        this.historyDomainService = historyDomainService;
    }

    public MediaTranscriptionTaskRefDTO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "media-upload.bin";
        }
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("cynexus-media-transcription-", safeSuffix(originalFilename));
            file.transferTo(tempFile);
            return toTaskRef(taskDomainService.upload(tempFile, originalFilename, file.getContentType()));
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "上传转写任务创建失败: " + exception.getMessage());
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public MediaTranscriptionTaskRefDTO remoteParse(MediaTranscriptionRemoteParseRequest request) {
        return toTaskRef(taskDomainService.remoteParse(toRemoteParseRequest(request)));
    }

    public MediaTranscriptionTaskDetailDTO getTask(String taskId) {
        return toTaskDetail(taskInteractionDomainService.getTask(taskId));
    }

    public MediaTranscriptionAskResponseDTO ask(String taskId, String question) {
        requireQuestion(question);
        MediaTranscriptionAskResultBO result = taskInteractionDomainService.ask(taskId, question);
        return MediaTranscriptionAskResponseDTO.builder().answer(result == null ? null : result.getAnswer()).build();
    }

    public MediaTranscriptionBinaryBO exportMarkdown(String taskId) {
        return taskInteractionDomainService.exportMarkdown(taskId);
    }

    public MediaTranscriptionStreamBO openStream(String taskId) {
        return taskDomainService.openStream(taskId);
    }

    public MediaTranscriptionHistoryListDTO listHistory() {
        return MediaTranscriptionHistoryListDTO.builder()
                .items(historyDomainService.list().stream().map(this::toHistoryItem).toList())
                .build();
    }

    public Map<String, Object> getHistory(String historyId) {
        return historyDomainService.record(historyId);
    }

    public MediaTranscriptionHistoryItemDTO saveHistory(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "taskId is required");
        }
        return toHistoryItem(historyDomainService.saveCompletedTask(taskInteractionDomainService.getTask(taskId)));
    }

    public MediaTranscriptionHistoryItemDTO saveCompletedTask(MediaTranscriptionTaskBO task) {
        return toHistoryItem(historyDomainService.saveCompletedTask(task));
    }

    public MediaTranscriptionHistoryListDTO reorderHistory(List<String> orderedIds) {
        return MediaTranscriptionHistoryListDTO.builder()
                .items(historyDomainService.reorder(orderedIds).stream().map(this::toHistoryItem).toList())
                .build();
    }

    public Map<String, Object> deleteHistory(String historyId) {
        MediaTranscriptionHistoryBO history = historyDomainService.delete(historyId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(history.getId()));
        result.put("title", history.getTitle());
        return result;
    }

    public MediaTranscriptionHistoryQaItemDTO askHistory(String historyId, String question) {
        requireQuestion(question);
        return toQaItem(historyDomainService.ask(historyId, question));
    }

    private void requireQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "问题不能为空");
        }
    }

    private MediaTranscriptionHistoryItemDTO toHistoryItem(MediaTranscriptionHistoryBO history) {
        if (history == null) return null;
        return MediaTranscriptionHistoryItemDTO.builder()
                .id(String.valueOf(history.getId())).title(history.getTitle()).createdAt(history.getCreatedAt())
                .taskId(history.getTaskId()).status(history.getStatus()).fileName(history.getFileName())
                .dataFile("postgres:media_transcription_history/" + history.getId()).build();
    }

    private MediaTranscriptionHistoryQaItemDTO toQaItem(MediaTranscriptionHistoryQaBO item) {
        return MediaTranscriptionHistoryQaItemDTO.builder().id(item.getId()).question(item.getQuestion())
                .answer(item.getAnswer()).createdAt(item.getCreatedAt()).build();
    }

    private static MediaTranscriptionTaskRefDTO toTaskRef(MediaTranscriptionTaskRefBO value) {
        return MediaTranscriptionTaskRefDTO.builder().taskId(value == null ? null : value.getTaskId())
                .fileName(value == null ? null : value.getFileName()).build();
    }

    private static MediaTranscriptionTaskDetailDTO toTaskDetail(MediaTranscriptionTaskBO value) {
        return MediaTranscriptionTaskDetailDTO.builder().taskId(value == null ? null : value.getTaskId())
                .status(value == null ? null : value.getStatus()).transcript(value == null ? null : value.getTranscript())
                .polished(value == null ? null : value.getPolished()).summary(value == null ? null : value.getSummary())
                .metadata(toMetadata(value == null ? null : value.getMetadata())).fileName(value == null ? null : value.getFileName())
                .error(value == null ? null : value.getError()).downloadProgress(toProgress(value == null ? null : value.getDownloadProgress()))
                .intakeProgress(toProgress(value == null ? null : value.getIntakeProgress())).build();
    }

    private static MediaTranscriptionMetadataDTO toMetadata(MediaTranscriptionMetadataBO value) {
        if (value == null) return null;
        return MediaTranscriptionMetadataDTO.builder().title(value.getTitle()).categories(value.getCategories())
                .tags(value.getTags()).sourceUrl(value.getSourceUrl()).downloadUrl(value.getDownloadUrl())
                .platform(value.getPlatform()).duration(value.getDuration()).noteType(value.getNoteType())
                .audioUrl(value.getAudioUrl()).originalUrl(value.getOriginalUrl()).build();
    }

    private static MediaTranscriptionProgressDTO toProgress(MediaTranscriptionProgressBO value) {
        if (value == null) return null;
        return MediaTranscriptionProgressDTO.builder().phase(value.getPhase()).progress(value.getProgress())
                .downloadedBytes(value.getDownloadedBytes()).totalBytes(value.getTotalBytes()).processedBytes(value.getProcessedBytes())
                .etaSeconds(value.getEtaSeconds()).speedBytesPerSec(value.getSpeedBytesPerSec()).build();
    }

    private static MediaTranscriptionRemoteParseRequestBO toRemoteParseRequest(MediaTranscriptionRemoteParseRequest request) {
        if (request == null) throw new BizException(ErrorCode.BAD_REQUEST, "远程解析参数不能为空");
        return MediaTranscriptionRemoteParseRequestBO.builder().downloadUrl(request.getDownloadUrl()).title(request.getTitle())
                .sourceUrl(request.getSourceUrl()).platform(request.getPlatform()).duration(request.getDuration())
                .noteType(request.getNoteType()).audioUrl(request.getAudioUrl()).originalUrl(request.getOriginalUrl()).build();
    }

    private static String safeSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) return ".bin";
        String suffix = fileName.substring(index);
        return suffix.length() > 16 ? ".bin" : suffix;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
