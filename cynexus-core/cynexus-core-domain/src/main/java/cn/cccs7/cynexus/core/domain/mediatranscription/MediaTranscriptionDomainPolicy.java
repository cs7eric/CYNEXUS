package cn.cccs7.cynexus.core.domain.mediatranscription;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionProgressBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class MediaTranscriptionDomainPolicy {

    public boolean isDone(MediaTranscriptionTaskBO task) { return task != null && "done".equalsIgnoreCase(task.getStatus()); }
    public String normalize(String value) { return value == null ? "" : value.trim(); }
    public String firstText(String preferred, String fallback) {
        String value = normalize(preferred);
        return value.isBlank() ? normalize(fallback) : value;
    }
    public String safeStem(String fileName) {
        String value = fileName == null ? "media-transcription" : fileName.trim();
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        String stem = slash >= 0 ? value.substring(slash + 1) : value;
        int dot = stem.lastIndexOf('.');
        return normalize(dot > 0 ? stem.substring(0, dot) : stem).isBlank() ? "media-transcription" : (dot > 0 ? stem.substring(0, dot) : stem);
    }
    public String safeFileName(String value) {
        String normalized = normalize(value).replaceAll("[\\\\/:*?\"<>|]", "_");
        return normalized.isBlank() ? "media-transcription" : normalized;
    }
    public MediaTranscriptionTaskBO copyWithAiResult(MediaTranscriptionTaskBO source, String polished,
                                                      String summary, MediaTranscriptionMetadataBO metadata,
                                                      String warning) {
        String error = source.getError();
        if (warning != null && !warning.isBlank()) error = error == null || error.isBlank() ? warning : error + "; " + warning;
        return MediaTranscriptionTaskBO.builder().taskId(source.getTaskId()).status(source.getStatus())
                .transcript(source.getTranscript()).polished(polished).summary(summary).metadata(metadata)
                .fileName(source.getFileName()).error(error).downloadProgress(source.getDownloadProgress())
                .intakeProgress(source.getIntakeProgress()).build();
    }
    public MediaTranscriptionMetadataBO mergeMetadata(MediaTranscriptionMetadataBO original, MediaTranscriptionMetadataBO ai) {
        if (original == null && ai == null) return null;
        MediaTranscriptionMetadataBO base = original == null ? MediaTranscriptionMetadataBO.builder().build() : original;
        return MediaTranscriptionMetadataBO.builder().title(firstText(ai == null ? null : ai.getTitle(), base.getTitle()))
                .categories(mergeList(ai == null ? null : ai.getCategories(), base.getCategories()))
                .tags(mergeList(ai == null ? null : ai.getTags(), base.getTags())).sourceUrl(base.getSourceUrl())
                .downloadUrl(base.getDownloadUrl()).platform(base.getPlatform()).duration(base.getDuration())
                .noteType(base.getNoteType()).audioUrl(base.getAudioUrl()).originalUrl(base.getOriginalUrl()).build();
    }
    public Map<String, Object> buildHistoryRecord(Long existingId, MediaTranscriptionTaskBO task,
                                                   List<Map<String, Object>> qaItems, OffsetDateTime now) {
        Map<String, Object> taskMap = new LinkedHashMap<>();
        taskMap.put("task_id", task.getTaskId()); taskMap.put("status", task.getStatus());
        taskMap.put("transcript", normalize(task.getTranscript())); taskMap.put("polished", normalize(task.getPolished()));
        taskMap.put("summary", normalize(task.getSummary())); taskMap.put("metadata", metadataToMap(task.getMetadata()));
        taskMap.put("file_name", task.getFileName()); taskMap.put("error", task.getError());
        taskMap.put("download_progress", progressToMap(task.getDownloadProgress()));
        taskMap.put("intake_progress", progressToMap(task.getIntakeProgress())); taskMap.put("qa_items", qaItems == null ? List.of() : qaItems);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", existingId == null ? "" : String.valueOf(existingId)); record.put("type", "mp4_parse");
        record.put("version", 1); record.put("created_at", now.toString()); record.put("updated_at", now.toString());
        record.put("title", historyTitle(task)); record.put("task", taskMap); return record;
    }
    public String buildMarkdown(String title, MediaTranscriptionTaskBO task, String date) {
        MediaTranscriptionMetadataBO metadata = task.getMetadata();
        return "---\n" + "title: " + title + "\n" + "date: " + date + "\n"
                + "categories: " + String.join(",", metadata == null || metadata.getCategories() == null ? List.of() : metadata.getCategories()) + "\n"
                + "tags: " + String.join(",", metadata == null || metadata.getTags() == null ? List.of() : metadata.getTags()) + "\n"
                + "---\n\n## Transcript\n" + normalize(task.getTranscript()) + "\n\n## Polished\n"
                + normalize(task.getPolished()) + "\n\n## Summary\n" + normalize(task.getSummary()) + '\n';
    }
    private String historyTitle(MediaTranscriptionTaskBO task) { return firstText(task.getMetadata() == null ? null : task.getMetadata().getTitle(), safeStem(task.getFileName())); }
    private Map<String, Object> metadataToMap(MediaTranscriptionMetadataBO value) {
        Map<String, Object> map = new LinkedHashMap<>(); if (value == null) return map;
        map.put("title", value.getTitle()); map.put("categories", value.getCategories() == null ? List.of() : value.getCategories());
        map.put("tags", value.getTags() == null ? List.of() : value.getTags()); map.put("source_url", value.getSourceUrl());
        map.put("download_url", value.getDownloadUrl()); map.put("platform", value.getPlatform()); map.put("duration", value.getDuration());
        map.put("noteType", value.getNoteType()); map.put("download_audio_url", value.getAudioUrl()); map.put("original_url", value.getOriginalUrl()); return map;
    }
    private Map<String, Object> progressToMap(MediaTranscriptionProgressBO value) {
        Map<String, Object> map = new LinkedHashMap<>(); if (value == null) return map;
        map.put("phase", value.getPhase()); map.put("progress", value.getProgress()); map.put("downloaded_bytes", value.getDownloadedBytes());
        map.put("total_bytes", value.getTotalBytes()); map.put("processed_bytes", value.getProcessedBytes()); map.put("eta_seconds", value.getEtaSeconds());
        map.put("speed_bytes_per_sec", value.getSpeedBytesPerSec()); return map;
    }
    private List<String> mergeList(List<String> preferred, List<String> fallback) {
        LinkedHashSet<String> values = new LinkedHashSet<>(); addAll(values, preferred); addAll(values, fallback); return new ArrayList<>(values);
    }
    private void addAll(LinkedHashSet<String> values, List<String> items) { if (items != null) for (String item : items) if (!normalize(item).isBlank()) values.add(normalize(item)); }
}
