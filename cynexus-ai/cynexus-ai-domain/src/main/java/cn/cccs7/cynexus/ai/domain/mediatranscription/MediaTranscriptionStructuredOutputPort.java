package cn.cccs7.cynexus.ai.domain.mediatranscription;

import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;

public interface MediaTranscriptionStructuredOutputPort extends AiStructuredOutputParser {

    String extractObject(String content);
}
