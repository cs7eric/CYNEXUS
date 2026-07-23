package cn.cccs7.cynexus.ai.domain.service.scenario;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import java.util.List;
import java.util.Objects;

public class ScenarioExecutionDomainServiceImpl implements ScenarioExecutionDomainService {

    private final ApplicationAnalysisRoutingDomainService routingDomainService;
    private final AiPromptRepository promptRepository;

    public ScenarioExecutionDomainServiceImpl(
            ApplicationAnalysisRoutingDomainService routingDomainService,
            AiPromptRepository promptRepository) {
        this.routingDomainService = Objects.requireNonNull(routingDomainService, "routingDomainService");
        this.promptRepository = Objects.requireNonNull(promptRepository, "promptRepository");
    }

    @Override
    public AiModelBO routeInformationModel(String preferredModelCode,
                                           List<String> bindingCapabilities,
                                           boolean workspaceChat) {
        AiUsageBindingBO binding = bindingCapabilities == null ? null : bindingCapabilities.stream()
                .map(routingDomainService::findBinding)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (binding == null) {
            throw new IllegalStateException(workspaceChat
                    ? "No enabled AI provider is bound to workspace chat."
                    : "No enabled AI provider is bound to stock information analysis.");
        }
        return routingDomainService.routeModels(binding.getCapability(), preferredModelCode).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(workspaceChat
                        ? "The AI provider bound to workspace chat is unavailable."
                        : "The AI provider bound to stock information analysis is unavailable."));
    }

    @Override
    public AiModelBO routeVisionModel(String capability, String preferredModelCode) {
        return routingDomainService.routeModels(capability, preferredModelCode).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No AI model available for accounting receipt recognition"));
    }

    @Override
    public String resolveVisionPrompt(String capability, String candidateContext) {
        AiUsageBindingBO binding = routingDomainService.findBinding(capability);
        if (binding != null && binding.getPromptId() != null) {
            AiPromptBO prompt = promptRepository.findById(binding.getPromptId());
            if (prompt != null && "active".equalsIgnoreCase(prompt.getStatus())) {
                String userPrompt = prompt.getUserPromptTemplate() == null
                        || prompt.getUserPromptTemplate().isBlank()
                        ? candidateContext
                        : prompt.getUserPromptTemplate().replace(
                                "{{candidate_context}}", candidateContext == null ? "" : candidateContext);
                return prompt.getSystemPrompt() + System.lineSeparator() + userPrompt;
            }
        }
        return defaultVisionPrompt(candidateContext);
    }

    private String defaultVisionPrompt(String candidateContext) {
        return """
                你是记账单据识别助手。只从用户上传的图片中提取可见事实，不要猜测；金额必须是正数，无法确认的字段使用 null。
                一张图片可能包含多笔消费，必须拆成多条 items；多张图片中的重复小票只保留一次。transactionType 只能是 expense 或 income。
                用户输入会提供当前可用账户和分类候选。必须优先根据小票、支付方式、商户、备注等可见信息从候选中选择 accountId 和 categoryId，不得编造候选之外的 ID；无法可靠判断时返回 null，并填写 accountHint 或 categoryHint 供后端处理。
                微信转账详情中的 Payment Method: Balance 表示微信余额，优先选择名称或备注包含微信、微信余额、微信钱包的账户；如果候选中没有对应账户，accountHint 必须返回“微信余额”。
                只输出合法 JSON：{"items":[{"imageIndex":0,"transactionType":"expense","occurredAt":"2026-01-01T12:00:00+08:00","amount":12.50,"currency":"CNY","merchant":"","accountId":null,"accountHint":"","categoryId":null,"categoryHint":"餐饮","note":"","installmentCount":null,"installmentFee":null,"confidence":0.8,"rawText":""}]}。
                """ + (candidateContext == null ? "" : candidateContext);
    }
}
