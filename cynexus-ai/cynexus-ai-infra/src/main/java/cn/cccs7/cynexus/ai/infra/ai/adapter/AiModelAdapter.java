package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.agent.AgentModelTurnResponse;
import java.util.List;
import java.util.function.Consumer;

public interface AiModelAdapter {

    String providerCode();

    AiCompletionResultBO complete(AiModelBO model, String prompt);

    default AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images) {
        return complete(model, prompt);
    }

    default AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request) {
        String prompt = request == null ? "" : request.getSystemPrompt() + "\n" +
                (request.getMessages() == null ? "" : request.getMessages().stream()
                        .map(message -> message.getRole() + ": " + message.getContent())
                        .reduce((left, right) -> left + "\n" + right).orElse(""));
        AiCompletionResultBO completion = complete(model, prompt);
        return AgentModelTurnResponse.builder()
                .type("FINAL")
                .content(completion.getContent())
                .toolCalls(List.of())
                .inputTokens(completion.getInputTokens())
                .outputTokens(completion.getOutputTokens())
                .totalTokens(completion.getTotalTokens())
                .rawResponse(completion.getRawResponse())
                .build();
    }

    default AgentModelTurnResponse streamAgent(AiModelBO model, AgentModelTurnRequest request,
                                               Consumer<String> textDeltaConsumer) {
        return completeAgent(model, request);
    }
}
