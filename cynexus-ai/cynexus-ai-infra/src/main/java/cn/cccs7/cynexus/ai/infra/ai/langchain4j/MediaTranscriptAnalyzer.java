package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import dev.langchain4j.service.UserMessage;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionEnrichmentBO;

/** Typed facade used after Cynexus has resolved capability, prompt and model. */
public interface MediaTranscriptAnalyzer {
    // The complete, DB-resolved Cynexus prompt is supplied as the argument.
    // Do not add a static system prompt that would override prompt versions.
    @UserMessage("{{text}}")
    MediaTranscriptionEnrichmentBO analyze(String text);
}
