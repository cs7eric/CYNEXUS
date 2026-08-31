package cn.cccs7.cynexus.core.domain.mediatranscription;

import cn.cccs7.cynexus.core.domain.client.MediaTranscriptionAiClient;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionBinaryBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionTaskBO;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class MediaTranscriptionTaskInteractionDomainService {

    private final MediaTranscriptionTaskDomainService taskService;
    private final MediaTranscriptionAiClient aiClient;
    private final MediaTranscriptionDomainPolicy policy = new MediaTranscriptionDomainPolicy();

    public MediaTranscriptionTaskInteractionDomainService(MediaTranscriptionTaskDomainService taskService,
                                                          MediaTranscriptionAiClient aiClient) {
        this.taskService = taskService;
        this.aiClient = aiClient;
    }

    public MediaTranscriptionTaskBO getTask(String taskId) {
        MediaTranscriptionTaskBO task = taskService.getTask(taskId);
        if (task == null || !policy.isDone(task)) {
            return task;
        }
        return enrich(task);
    }

    public MediaTranscriptionAskResultBO ask(String taskId, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        MediaTranscriptionTaskBO task = getTask(taskId);
        if (!policy.isDone(task)) {
            throw new IllegalArgumentException("任务尚未完成");
        }
        return aiClient.ask(policy.firstText(task.getPolished(), task.getTranscript()), task.getSummary(), question);
    }

    public MediaTranscriptionBinaryBO exportMarkdown(String taskId) {
        MediaTranscriptionTaskBO task = getTask(taskId);
        if (!policy.isDone(task)) {
            throw new IllegalArgumentException("任务尚未完成");
        }
        MediaTranscriptionMetadataBO metadata = task.getMetadata();
        String title = metadata == null || metadata.getTitle() == null || metadata.getTitle().isBlank()
                ? policy.safeStem(task.getFileName()) : metadata.getTitle();
        String markdown = policy.buildMarkdown(title, task,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        return MediaTranscriptionBinaryBO.builder()
                .fileName(policy.safeFileName(title) + ".md")
                .contentType("text/markdown; charset=utf-8")
                .content(markdown.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private MediaTranscriptionTaskBO enrich(MediaTranscriptionTaskBO task) {
        String transcript = policy.normalize(task.getTranscript());
        String polished = policy.normalize(task.getPolished());
        String summary = policy.normalize(task.getSummary());
        MediaTranscriptionMetadataBO metadata = task.getMetadata();
        try {
            if (!transcript.isBlank() && (polished.isBlank() || polished.equals(transcript))) {
                var ai = aiClient.enrich(transcript,
                        metadata == null ? null : metadata.getTitle(),
                        metadata == null ? null : metadata.getPlatform(),
                        metadata == null ? null : metadata.getDuration());
                polished = policy.normalize(ai.polishedText());
                summary = policy.normalize(ai.summary());
                MediaTranscriptionMetadataBO aiMetadata = MediaTranscriptionMetadataBO.builder().title(ai.title()).build();
                metadata = policy.mergeMetadata(metadata, aiMetadata);
            }
            return policy.copyWithAiResult(task, policy.firstText(polished, transcript), summary, metadata, null);
        } catch (Exception exception) {
            return policy.copyWithAiResult(task, policy.firstText(polished, transcript), summary, metadata,
                    "AI polish/summary/metadata failed: " + exception.getMessage());
        }
    }
}
