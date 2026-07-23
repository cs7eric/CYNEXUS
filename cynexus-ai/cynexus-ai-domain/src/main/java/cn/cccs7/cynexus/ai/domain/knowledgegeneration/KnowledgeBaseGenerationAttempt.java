package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.time.Duration;
import java.time.Instant;

public record KnowledgeBaseGenerationAttempt(String executionNo, AiModelBO model,
                                             KnowledgeBaseGenerationPrompt prompt, String status,
                                             long latencyMs, AiCompletionResultBO completion,
                                             String errorMessage) {

    public static KnowledgeBaseGenerationAttempt succeeded(String executionNo, AiModelBO model,
                                                           KnowledgeBaseGenerationPrompt prompt,
                                                           Instant startedAt, AiCompletionResultBO completion) {
        return new KnowledgeBaseGenerationAttempt(executionNo, model, prompt, "SUCCEEDED",
                elapsed(startedAt), completion, null);
    }

    public static KnowledgeBaseGenerationAttempt failed(String executionNo, AiModelBO model,
                                                        KnowledgeBaseGenerationPrompt prompt,
                                                        Instant startedAt, AiCompletionResultBO completion,
                                                        Exception failure) {
        return new KnowledgeBaseGenerationAttempt(executionNo, model, prompt, "FAILED",
                elapsed(startedAt), completion, message(failure));
    }

    private static long elapsed(Instant startedAt) {
        return startedAt == null ? 0 : Math.max(0, Duration.between(startedAt, Instant.now()).toMillis());
    }

    private static String message(Exception failure) {
        if (failure == null || failure.getMessage() == null || failure.getMessage().isBlank()) {
            return failure == null ? "generation failed" : failure.getClass().getSimpleName();
        }
        return failure.getMessage();
    }
}
