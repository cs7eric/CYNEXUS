package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.service.UserMessage;

/** Runtime-governed text completion facade. The supplied value is the fully rendered Cynexus prompt. */
public interface GenericTextAnalyzer {
    @UserMessage("{{text}}")
    String complete(String text);
}
