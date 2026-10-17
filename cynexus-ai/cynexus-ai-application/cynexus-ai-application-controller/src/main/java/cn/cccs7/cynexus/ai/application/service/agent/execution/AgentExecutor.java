package cn.cccs7.cynexus.ai.application.service.agent.execution;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnRequest;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelTurnResponse;
import cn.cccs7.cynexus.ai.domain.gateway.modelturn.AiCompletionGateway;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import cn.cccs7.cynexus.ai.domain.port.LangChain4jCompletionPort;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.cccs7.cynexus.common.core.constant.TraceId;

@Service
public class AgentExecutor {
    private static final Logger log = LoggerFactory.getLogger(AgentExecutor.class);

    private final AiCompletionGateway completionGateway;
    private final LangChain4jCompletionPort langChain4j;

    public AgentExecutor(AiCompletionGateway completionGateway) {
        this.completionGateway = completionGateway;
        this.langChain4j = null;
    }

    @Autowired
    public AgentExecutor(AiCompletionGateway completionGateway, LangChain4jCompletionPort langChain4j) {
        this.completionGateway = completionGateway;
        this.langChain4j = langChain4j;
    }

    public String execute(AiScenarioBO scenario, AiModelBO model, String prompt, List<String> toolCodes) {
        if (toolCodes == null || toolCodes.isEmpty()) {
            return complete(model, prompt);
        }
        throw new IllegalStateException("Tool-enabled generic execution is unsupported; use the autonomous Agent workflow.");
    }

    public String complete(AiModelBO model, String prompt) {
        return completeDetailed(model, prompt).getContent();
    }

    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt) {
        return completeDetailed(model, prompt, "");
    }

    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, String capability) {
        String traceId = TraceId.current();
        AiExecutionContext context = new AiExecutionContext("EXEC-" + java.util.UUID.randomUUID(), capability,
                null, null, model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode(),
                null, null, traceId, traceId);
        if (langChain4j != null) {
            try {
                return langChain4j.complete(model, prompt, context);
            } catch (RuntimeException exception) {
                // Preserve the existing provider gateway as a rollback path.
                log.warn("LangChain4j text execution failed; falling back to legacy gateway, provider={}, model={}, capability={}: {}",
                        model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode(), capability, exception.getMessage());
            }
        }
        return completionGateway.complete(model, prompt, context);
    }

    public AiCompletionResultBO completeDetailed(AiModelBO model, String prompt, AiExecutionContext context) {
        if (langChain4j != null) {
            try { return langChain4j.complete(model, prompt, context); }
            catch (RuntimeException exception) {
                log.warn("LangChain4j context execution failed; falling back to legacy gateway executionNo={}, provider={}, model={}, capability={}: {}",
                        context == null ? null : context.executionNo(), model == null ? null : model.getProviderCode(),
                        model == null ? null : model.getModelCode(), context == null ? null : context.capability(), exception.getMessage());
            }
        }
        return completionGateway.complete(model, prompt, context);
    }

    public AiCompletionResultBO completeVisionDetailed(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability) {
        String traceId = TraceId.current();
        AiExecutionContext context = new AiExecutionContext("EXEC-" + java.util.UUID.randomUUID(), capability,
                null, null, model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode(),
                null, null, traceId, traceId);
        if (langChain4j != null) {
            try {
                return langChain4j.completeVision(model, prompt, images, context);
            } catch (RuntimeException exception) {
                // Preserve the existing vision gateway as a rollback path.
                log.warn("LangChain4j vision execution failed; falling back to legacy gateway, provider={}, model={}, capability={}: {}",
                        model == null ? null : model.getProviderCode(), model == null ? null : model.getModelCode(), capability, exception.getMessage());
            }
        }
        return completionGateway.completeVision(model, prompt, images, context);
    }

    public AiCompletionResultBO completeVisionDetailed(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                       AiExecutionContext context) {
        if (langChain4j != null) {
            try { return langChain4j.completeVision(model, prompt, images, context); }
            catch (RuntimeException exception) {
                log.warn("LangChain4j vision context execution failed; falling back to legacy gateway executionNo={}, provider={}, model={}, capability={}: {}",
                        context == null ? null : context.executionNo(), model == null ? null : model.getProviderCode(),
                        model == null ? null : model.getModelCode(), context == null ? null : context.capability(), exception.getMessage());
            }
        }
        return completionGateway.completeVision(model, prompt, images, context);
    }

    public AgentModelTurnResponse completeAgent(AiModelBO model, AgentModelTurnRequest request, String capability) {
        return completionGateway.completeAgent(model, request, capability);
    }
}
