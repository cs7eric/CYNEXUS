package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.application.prompt.StructuredJsonExtractor;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisStructuredOutputPort;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAnalysisStructuredOutputAdapter implements ApplicationAnalysisStructuredOutputPort {

    private final StructuredJsonExtractor extractor;

    public ApplicationAnalysisStructuredOutputAdapter(StructuredJsonExtractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public String extractObject(String rawResponse) {
        return extractor.extractObject(rawResponse);
    }
}
