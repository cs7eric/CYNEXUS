package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.data.message.UserMessage;

/** Typed multimodal boundary; domain policy validates amounts, IDs and categories. */
public interface ReceiptVisionAnalyzer {
    @dev.langchain4j.service.UserMessage UserReceiptOutput analyze(UserMessage message);
}
