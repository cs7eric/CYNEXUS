package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.prompt.StructuredJsonExtractor;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionStructuredOutputPort;
import org.springframework.stereotype.Component;

@Component
public class MediaTranscriptionStructuredOutputAdapter implements MediaTranscriptionStructuredOutputPort {

    private final StructuredJsonExtractor extractor;

    public MediaTranscriptionStructuredOutputAdapter(StructuredJsonExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public String extractObject(String content) {
        return extractor.extractObject(content);
    }
}
