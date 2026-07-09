package cn.cccs7.cynexus.core.domain.client;

import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionAskResultBO;
import cn.cccs7.cynexus.core.domain.entity.MediaTranscriptionMetadataBO;

public interface MediaTranscriptionAiClient {

    String polish(String text);

    String summarize(String text, String title, String platform, Integer duration);

    MediaTranscriptionMetadataBO metadata(String polishedText, String summaryText);

    MediaTranscriptionAskResultBO ask(String polishedText, String summaryText, String question);
}
