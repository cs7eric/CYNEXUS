package cn.cccs7.cynexus.core.domain.client;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAiResultBO;

public interface KnowledgeBaseDocumentWriter {
    Long createDraft(String taskId, String transcript, MediaTranscriptionAiResultBO result, String sourceUrl);
}
