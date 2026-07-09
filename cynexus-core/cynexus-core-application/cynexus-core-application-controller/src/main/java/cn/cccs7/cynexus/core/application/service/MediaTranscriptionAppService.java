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
import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionAiClient;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionHistoryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProgressBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionRemoteParseRequestBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionStreamBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskRefBO;
import cn.cccs7.cynexus.core.domain.repository.MediaTranscriptionHistoryRepository;
import cn.cccs7.cynexus.core.domain.service.MediaTranscriptionTaskDomainService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class MediaTranscriptionAppService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MediaTranscriptionTaskDomainService mediaTranscriptionTaskDomainService;
    private final MediaTranscriptionAiClient mediaTranscriptionAiClient;
    private final MediaTranscriptionHistoryRepository mediaTranscriptionHistoryRepository;
    private final ConcurrentMap<String, MediaTranscriptionTaskBO> aiEnrichedTasks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaTranscriptionAppService(MediaTranscriptionTaskDomainService mediaTranscriptionTaskDomainService,
                                        MediaTranscriptionAiClient mediaTranscriptionAiClient,
                                        MediaTranscriptionHistoryRepository mediaTranscriptionHistoryRepository) {
        this.mediaTranscriptionTaskDomainService = mediaTranscriptionTaskDomainService;
        this.mediaTranscriptionAiClient = mediaTranscriptionAiClient;
        this.mediaTranscriptionHistoryRepository = mediaTranscriptionHistoryRepository;
    }

    public MediaTranscriptionTaskRefDTO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "media-upload.bin";
        }
        String suffix = safeSuffix(originalFilename);
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("cynexus-media-transcription-", suffix);
            file.transferTo(tempFile);
            MediaTranscriptionTaskRefBO result = mediaTranscriptionTaskDomainService.upload(
                    tempFile,
                    originalFilename,
                    file.getContentType());
            return toTaskRef(result);
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "上传转写任务创建失败: " + exception.getMessage());
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public MediaTranscriptionTaskRefDTO remoteParse(MediaTranscriptionRemoteParseRequest request) {
        MediaTranscriptionRemoteParseRequestBO result = toRemoteParseRequest(request);
        return toTaskRef(mediaTranscriptionTaskDomainService.remoteParse(result));
    }

    public MediaTranscriptionTaskDetailDTO getTask(String taskId) {
        return toTaskDetail(getAiEnrichedTask(taskId));
    }

    public MediaTranscriptionAskResponseDTO ask(String taskId, String question) {
        if (question == null || question.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "问题不能为空");
        }
        MediaTranscriptionTaskBO task = getAiEnrichedTask(taskId);
        if (task == null || !"done".equals(task.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务尚未完成");
        }
        MediaTranscriptionAskResultBO result = mediaTranscriptionAiClient.ask(
                firstText(task.getPolished(), task.getTranscript()),
                task.getSummary(),
                question);
        return MediaTranscriptionAskResponseDTO.builder()
                .answer(result == null ? null : result.getAnswer())
                .build();
    }

    public MediaTranscriptionBinaryBO exportMarkdown(String taskId) {
        MediaTranscriptionTaskBO task = getAiEnrichedTask(taskId);
        if (task == null || !"done".equals(task.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务尚未完成");
        }
        String title = task.getMetadata() == null || task.getMetadata().getTitle() == null || task.getMetadata().getTitle().isBlank()
                ? safeStem(task.getFileName())
                : task.getMetadata().getTitle();
        String markdown = buildMarkdown(title, task);
        return MediaTranscriptionBinaryBO.builder()
                .fileName(safeFileName(title) + ".md")
                .contentType("text/markdown; charset=utf-8")
                .content(markdown.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    public MediaTranscriptionStreamBO openStream(String taskId) {
        return mediaTranscriptionTaskDomainService.openStream(taskId);
    }

    public MediaTranscriptionHistoryListDTO listHistory() {
        List<MediaTranscriptionHistoryItemDTO> items = mediaTranscriptionHistoryRepository.listActive().stream()
                .map(this::toHistoryItem)
                .toList();
        return MediaTranscriptionHistoryListDTO.builder()
                .items(items)
                .build();
    }

    public Map<String, Object> getHistory(String historyId) {
        MediaTranscriptionHistoryBO history = requireHistory(historyId);
        return parseRecord(history.getRecordJson());
    }

    public MediaTranscriptionHistoryItemDTO saveHistory(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "taskId is required");
        }
        MediaTranscriptionTaskBO task = getAiEnrichedTask(taskId);
        if (task == null || !"done".equals(task.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务尚未完成");
        }
        MediaTranscriptionHistoryBO existing = mediaTranscriptionHistoryRepository.findByTaskId(taskId);
        Map<String, Object> record = existing == null
                ? buildHistoryRecord(null, task, null)
                : buildHistoryRecord(existing.getId(), task, readQaItems(parseRecord(existing.getRecordJson())));
        String recordJson = toJson(record);
        String title = asString(record.get("title"));
        String status = normalize(task.getStatus());
        if (existing == null) {
            Long id = mediaTranscriptionHistoryRepository.create(MediaTranscriptionHistoryBO.builder()
                    .taskId(taskId)
                    .title(title)
                    .status(status)
                    .fileName(task.getFileName())
                    .recordJson(recordJson)
                    .sortOrder((int) (System.currentTimeMillis() / 1000L))
                    .build());
            MediaTranscriptionHistoryBO saved = mediaTranscriptionHistoryRepository.findById(id);
            if (saved != null) {
                Map<String, Object> savedRecord = parseRecord(saved.getRecordJson());
                savedRecord.put("id", String.valueOf(saved.getId()));
                saved.setRecordJson(toJson(savedRecord));
                mediaTranscriptionHistoryRepository.update(saved);
                saved = mediaTranscriptionHistoryRepository.findById(id);
            }
            return toHistoryItem(saved);
        }
        existing.setTitle(title);
        existing.setStatus(status);
        existing.setFileName(task.getFileName());
        existing.setRecordJson(recordJson);
        mediaTranscriptionHistoryRepository.update(existing);
        return toHistoryItem(mediaTranscriptionHistoryRepository.findById(existing.getId()));
    }

    public MediaTranscriptionHistoryListDTO reorderHistory(List<String> orderedIds) {
        if (orderedIds != null) {
            int sortOrder = orderedIds.size();
            for (String orderedId : orderedIds) {
                Long id = parseLong(orderedId);
                if (id != null) {
                    mediaTranscriptionHistoryRepository.updateSortOrder(id, sortOrder--);
                }
            }
        }
        return listHistory();
    }

    public Map<String, Object> deleteHistory(String historyId) {
        MediaTranscriptionHistoryBO history = requireHistory(historyId);
        mediaTranscriptionHistoryRepository.delete(history.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(history.getId()));
        result.put("title", history.getTitle());
        return result;
    }

    public MediaTranscriptionHistoryQaItemDTO askHistory(String historyId, String question) {
        if (question == null || question.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "问题不能为空");
        }
        MediaTranscriptionHistoryBO history = requireHistory(historyId);
        Map<String, Object> record = parseRecord(history.getRecordJson());
        Map<String, Object> task = asMap(record.get("task"));
        String answer = mediaTranscriptionAiClient.ask(
                firstText(asString(task.get("polished")), asString(task.get("transcript"))),
                asString(task.get("summary")),
                question).getAnswer();
        MediaTranscriptionHistoryQaItemDTO item = MediaTranscriptionHistoryQaItemDTO.builder()
                .id("qa-" + UUID.randomUUID().toString().replace("-", ""))
                .question(question)
                .answer(answer)
                .createdAt(OffsetDateTime.now())
                .build();
        List<Map<String, Object>> qaItems = readQaItems(record);
        qaItems.add(0, qaItemToMap(item));
        task.put("qa_items", qaItems);
        record.put("task", task);
        record.put("updated_at", OffsetDateTime.now().toString());
        history.setRecordJson(toJson(record));
        mediaTranscriptionHistoryRepository.update(history);
        return item;
    }

    private MediaTranscriptionTaskBO getAiEnrichedTask(String taskId) {
        MediaTranscriptionTaskBO task = mediaTranscriptionTaskDomainService.getTask(taskId);
        if (task == null || !"done".equals(task.getStatus())) {
            return task;
        }
        MediaTranscriptionTaskBO cached = aiEnrichedTasks.get(taskId);
        if (cached != null) {
            return cached;
        }
        MediaTranscriptionTaskBO enriched = enrichTask(task);
        if (enriched.getError() == null || !enriched.getError().contains("AI polish/summary/metadata failed")) {
            aiEnrichedTasks.put(taskId, enriched);
        }
        return enriched;
    }

    private MediaTranscriptionHistoryBO requireHistory(String historyId) {
        Long id = parseLong(historyId);
        if (id == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "historyId is invalid");
        }
        MediaTranscriptionHistoryBO history = mediaTranscriptionHistoryRepository.findById(id);
        if (history == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "历史记录不存在");
        }
        return history;
    }

    private MediaTranscriptionHistoryItemDTO toHistoryItem(MediaTranscriptionHistoryBO history) {
        if (history == null) {
            return null;
        }
        return MediaTranscriptionHistoryItemDTO.builder()
                .id(String.valueOf(history.getId()))
                .title(history.getTitle())
                .createdAt(history.getCreatedAt())
                .taskId(history.getTaskId())
                .status(history.getStatus())
                .fileName(history.getFileName())
                .dataFile("postgres:media_transcription_history/" + history.getId())
                .build();
    }

    private Map<String, Object> buildHistoryRecord(Long existingId, MediaTranscriptionTaskBO task, List<Map<String, Object>> qaItems) {
        OffsetDateTime now = OffsetDateTime.now();
        String id = existingId == null ? "" : String.valueOf(existingId);
        Map<String, Object> taskMap = new LinkedHashMap<>();
        taskMap.put("task_id", task.getTaskId());
        taskMap.put("status", task.getStatus());
        taskMap.put("transcript", normalize(task.getTranscript()));
        taskMap.put("polished", normalize(task.getPolished()));
        taskMap.put("summary", normalize(task.getSummary()));
        taskMap.put("metadata", metadataToMap(task.getMetadata()));
        taskMap.put("file_name", task.getFileName());
        taskMap.put("error", task.getError());
        taskMap.put("download_progress", progressToMap(task.getDownloadProgress()));
        taskMap.put("intake_progress", progressToMap(task.getIntakeProgress()));
        taskMap.put("qa_items", qaItems == null ? List.of() : qaItems);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", id);
        record.put("type", "mp4_parse");
        record.put("version", 1);
        record.put("created_at", now.toString());
        record.put("updated_at", now.toString());
        record.put("title", historyTitle(task));
        record.put("task", taskMap);
        return record;
    }

    private String historyTitle(MediaTranscriptionTaskBO task) {
        MediaTranscriptionMetadataBO metadata = task.getMetadata();
        return firstText(metadata == null ? null : metadata.getTitle(), safeStem(task.getFileName()));
    }

    private Map<String, Object> metadataToMap(MediaTranscriptionMetadataBO metadata) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (metadata == null) {
            return map;
        }
        map.put("title", metadata.getTitle());
        map.put("categories", metadata.getCategories() == null ? List.of() : metadata.getCategories());
        map.put("tags", metadata.getTags() == null ? List.of() : metadata.getTags());
        map.put("source_url", metadata.getSourceUrl());
        map.put("download_url", metadata.getDownloadUrl());
        map.put("platform", metadata.getPlatform());
        map.put("duration", metadata.getDuration());
        map.put("noteType", metadata.getNoteType());
        map.put("download_audio_url", metadata.getAudioUrl());
        map.put("original_url", metadata.getOriginalUrl());
        return map;
    }

    private Map<String, Object> progressToMap(MediaTranscriptionProgressBO progress) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (progress == null) {
            return map;
        }
        map.put("phase", progress.getPhase());
        map.put("progress", progress.getProgress());
        map.put("downloaded_bytes", progress.getDownloadedBytes());
        map.put("total_bytes", progress.getTotalBytes());
        map.put("processed_bytes", progress.getProcessedBytes());
        map.put("eta_seconds", progress.getEtaSeconds());
        map.put("speed_bytes_per_sec", progress.getSpeedBytesPerSec());
        return map;
    }

    private Map<String, Object> parseRecord(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "历史记录序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readQaItems(Map<String, Object> record) {
        Map<String, Object> task = asMap(record.get("task"));
        Object value = task.get("qa_items");
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((key, entry) -> normalized.put(String.valueOf(key), entry));
                items.add(normalized);
            }
        }
        return items;
    }

    private Map<String, Object> qaItemToMap(MediaTranscriptionHistoryQaItemDTO item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("question", item.getQuestion());
        map.put("answer", item.getAnswer());
        map.put("created_at", item.getCreatedAt().toString());
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private MediaTranscriptionTaskBO enrichTask(MediaTranscriptionTaskBO task) {
        String transcript = normalize(task.getTranscript());
        String polished = normalize(task.getPolished());
        String summary = normalize(task.getSummary());
        MediaTranscriptionMetadataBO metadata = task.getMetadata();
        try {
            if (!transcript.isBlank() && (polished.isBlank() || polished.equals(transcript))) {
                polished = normalize(mediaTranscriptionAiClient.polish(transcript));
            }
            if (!polished.isBlank()) {
                summary = normalize(mediaTranscriptionAiClient.summarize(
                        polished,
                        metadata == null ? null : metadata.getTitle(),
                        metadata == null ? null : metadata.getPlatform(),
                        metadata == null ? null : metadata.getDuration()));
            }
            MediaTranscriptionMetadataBO aiMetadata = mediaTranscriptionAiClient.metadata(firstText(polished, transcript), summary);
            metadata = mergeMetadata(metadata, aiMetadata);
            return copyTask(task, firstText(polished, transcript), summary, metadata, null);
        } catch (Exception exception) {
            return copyTask(task, firstText(polished, transcript), summary, metadata,
                    "AI polish/summary/metadata failed: " + exception.getMessage());
        }
    }

    private MediaTranscriptionTaskBO copyTask(MediaTranscriptionTaskBO source,
                                              String polished,
                                              String summary,
                                              MediaTranscriptionMetadataBO metadata,
                                              String warning) {
        String error = source.getError();
        if (warning != null && !warning.isBlank()) {
            error = error == null || error.isBlank() ? warning : error + "; " + warning;
        }
        return MediaTranscriptionTaskBO.builder()
                .taskId(source.getTaskId())
                .status(source.getStatus())
                .transcript(source.getTranscript())
                .polished(polished)
                .summary(summary)
                .metadata(metadata)
                .fileName(source.getFileName())
                .error(error)
                .downloadProgress(source.getDownloadProgress())
                .intakeProgress(source.getIntakeProgress())
                .build();
    }

    private MediaTranscriptionMetadataBO mergeMetadata(MediaTranscriptionMetadataBO original, MediaTranscriptionMetadataBO ai) {
        if (original == null && ai == null) {
            return null;
        }
        if (original == null) {
            original = MediaTranscriptionMetadataBO.builder().build();
        }
        return MediaTranscriptionMetadataBO.builder()
                .title(firstText(ai == null ? null : ai.getTitle(), original.getTitle()))
                .categories(mergeList(ai == null ? null : ai.getCategories(), original.getCategories()))
                .tags(mergeList(ai == null ? null : ai.getTags(), original.getTags()))
                .sourceUrl(original.getSourceUrl())
                .downloadUrl(original.getDownloadUrl())
                .platform(original.getPlatform())
                .duration(original.getDuration())
                .noteType(original.getNoteType())
                .audioUrl(original.getAudioUrl())
                .originalUrl(original.getOriginalUrl())
                .build();
    }

    private List<String> mergeList(List<String> preferred, List<String> fallback) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addAll(values, preferred);
        addAll(values, fallback);
        return new ArrayList<>(values);
    }

    private void addAll(LinkedHashSet<String> values, List<String> items) {
        if (items == null) {
            return;
        }
        for (String item : items) {
            String normalized = normalize(item);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
    }

    private String buildMarkdown(String title, MediaTranscriptionTaskBO task) {
        MediaTranscriptionMetadataBO metadata = task.getMetadata();
        StringBuilder builder = new StringBuilder();
        builder.append("---\n")
                .append("title: ").append(title).append('\n')
                .append("date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))).append('\n')
                .append("categories: ").append(String.join(",", metadata == null || metadata.getCategories() == null ? List.of() : metadata.getCategories())).append('\n')
                .append("tags: ").append(String.join(",", metadata == null || metadata.getTags() == null ? List.of() : metadata.getTags())).append('\n')
                .append("---\n\n")
                .append("## Transcript\n")
                .append(normalize(task.getTranscript())).append("\n\n")
                .append("## Polished\n")
                .append(normalize(task.getPolished())).append("\n\n")
                .append("## Summary\n")
                .append(normalize(task.getSummary())).append('\n');
        return builder.toString();
    }

    private static MediaTranscriptionTaskRefDTO toTaskRef(MediaTranscriptionTaskRefBO value) {
        return MediaTranscriptionTaskRefDTO.builder()
                .taskId(value == null ? null : value.getTaskId())
                .fileName(value == null ? null : value.getFileName())
                .build();
    }

    private static MediaTranscriptionTaskDetailDTO toTaskDetail(MediaTranscriptionTaskBO value) {
        return MediaTranscriptionTaskDetailDTO.builder()
                .taskId(value == null ? null : value.getTaskId())
                .status(value == null ? null : value.getStatus())
                .transcript(value == null ? null : value.getTranscript())
                .polished(value == null ? null : value.getPolished())
                .summary(value == null ? null : value.getSummary())
                .metadata(toMetadata(value == null ? null : value.getMetadata()))
                .fileName(value == null ? null : value.getFileName())
                .error(value == null ? null : value.getError())
                .downloadProgress(toProgress(value == null ? null : value.getDownloadProgress()))
                .intakeProgress(toProgress(value == null ? null : value.getIntakeProgress()))
                .build();
    }

    private static MediaTranscriptionMetadataDTO toMetadata(MediaTranscriptionMetadataBO value) {
        if (value == null) {
            return null;
        }
        return MediaTranscriptionMetadataDTO.builder()
                .title(value.getTitle())
                .categories(value.getCategories())
                .tags(value.getTags())
                .sourceUrl(value.getSourceUrl())
                .downloadUrl(value.getDownloadUrl())
                .platform(value.getPlatform())
                .duration(value.getDuration())
                .noteType(value.getNoteType())
                .audioUrl(value.getAudioUrl())
                .originalUrl(value.getOriginalUrl())
                .build();
    }

    private static MediaTranscriptionProgressDTO toProgress(MediaTranscriptionProgressBO value) {
        if (value == null) {
            return null;
        }
        return MediaTranscriptionProgressDTO.builder()
                .phase(value.getPhase())
                .progress(value.getProgress())
                .downloadedBytes(value.getDownloadedBytes())
                .totalBytes(value.getTotalBytes())
                .processedBytes(value.getProcessedBytes())
                .etaSeconds(value.getEtaSeconds())
                .speedBytesPerSec(value.getSpeedBytesPerSec())
                .build();
    }

    private static MediaTranscriptionRemoteParseRequestBO toRemoteParseRequest(MediaTranscriptionRemoteParseRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "远程解析参数不能为空");
        }
        return MediaTranscriptionRemoteParseRequestBO.builder()
                .downloadUrl(request.getDownloadUrl())
                .title(request.getTitle())
                .sourceUrl(request.getSourceUrl())
                .platform(request.getPlatform())
                .duration(request.getDuration())
                .noteType(request.getNoteType())
                .audioUrl(request.getAudioUrl())
                .originalUrl(request.getOriginalUrl())
                .build();
    }

    private static String safeSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return ".bin";
        }
        String suffix = fileName.substring(index);
        return suffix.length() > 16 ? ".bin" : suffix;
    }

    private static String safeStem(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "transcription";
        }
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private static String safeFileName(String value) {
        String normalized = normalize(value).replaceAll("[<>:\"/\\\\|?*\\x00-\\x1f]+", "_");
        return normalized.isBlank() ? "transcription" : normalized;
    }

    private static String firstText(String preferred, String fallback) {
        String normalized = normalize(preferred);
        return normalized.isBlank() ? normalize(fallback) : normalized;
    }

    private static String asString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private static Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
