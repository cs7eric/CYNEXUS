package cn.cccs7.cynexus.ai.domain.service.scenario;

import cn.cccs7.cynexus.ai.common.constant.AiConstants;
import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import cn.cccs7.cynexus.ai.domain.policy.prompt.PromptRenderPolicy;
import cn.cccs7.cynexus.ai.domain.policy.scenario.ScenarioPromptPolicy;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.service.routing.ModelRouteDomainService;
import cn.cccs7.cynexus.ai.domain.validation.OutputValidationDomainService;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioTextExecution;
import cn.cccs7.cynexus.ai.domain.valueobject.scenario.PreparedScenarioVisionExecution;
import cn.cccs7.cynexus.ai.domain.workspacechat.WorkspaceChatDomainPolicy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ScenarioExecutionDomainServiceImpl implements ScenarioExecutionDomainService {

    private final ApplicationAnalysisRoutingDomainService routingDomainService;
    private final AiPromptRepository promptRepository;
    private final ModelRouteDomainService modelRouteDomainService;
    private final OutputValidationDomainService outputValidationDomainService;

    public ScenarioExecutionDomainServiceImpl(
            ApplicationAnalysisRoutingDomainService routingDomainService,
            AiPromptRepository promptRepository,
            ModelRouteDomainService modelRouteDomainService,
            OutputValidationDomainService outputValidationDomainService) {
        this.routingDomainService = Objects.requireNonNull(routingDomainService, "routingDomainService");
        this.promptRepository = Objects.requireNonNull(promptRepository, "promptRepository");
        this.modelRouteDomainService = Objects.requireNonNull(modelRouteDomainService, "modelRouteDomainService");
        this.outputValidationDomainService = Objects.requireNonNull(outputValidationDomainService, "outputValidationDomainService");
    }

    @Override
    public PreparedScenarioTextExecution prepareTextExecution(String scenarioCode,
                                                              String preferredModelCode,
                                                              String userInput,
                                                              Map<String, Object> variables,
                                                              List<String> enabledToolCodes) {
        String resolvedCode = scenarioCode == null || scenarioCode.isBlank()
                ? AiConstants.DEFAULT_SCENARIO : scenarioCode;
        boolean workspaceChat = WorkspaceChatDomainPolicy.supports(resolvedCode);
        AiScenarioBO scenario = resolveScenario(resolvedCode, workspaceChat);
        AiModelBO model = resolveTextModel(resolvedCode, preferredModelCode, workspaceChat);
        String prompt = PromptRenderPolicy.render(
                ScenarioPromptPolicy.systemPrompt(resolvedCode),
                userInput,
                variables);
        List<String> toolCodes = "stock_information_analysis".equals(resolvedCode) || workspaceChat
                ? List.of()
                : (enabledToolCodes == null ? List.of() : enabledToolCodes);
        return PreparedScenarioTextExecution.builder()
                .scenario(scenario)
                .model(model)
                .prompt(prompt)
                .toolCodes(toolCodes)
                .build();
    }

    @Override
    public PreparedScenarioVisionExecution prepareVisionExecution(String scenarioCode,
                                                                  String preferredModelCode,
                                                                  String userInput) {
        String resolvedCode = scenarioCode == null || scenarioCode.isBlank()
                ? "accounting_receipt_parse" : scenarioCode;
        String capability = "accounting_receipt_parse";
        AiScenarioBO scenario = AiScenarioBO.builder()
                .scenarioCode(resolvedCode)
                .scenarioName("Accounting Receipt Recognition")
                .defaultModelCode(null)
                .maxToolCalls(0)
                .toolCallRequired(false)
                .jsonOutputRequired(true)
                .enabled(true)
                .build();
        AiModelBO model = routeVisionModel(capability, preferredModelCode);
        String prompt = resolveVisionPrompt(capability, userInput == null ? "" : userInput);
        return PreparedScenarioVisionExecution.builder()
                .scenario(scenario)
                .model(model)
                .prompt(prompt)
                .capability(capability)
                .build();
    }

    @Override
    public AiExecutionBO finalizeExecution(PreparedScenarioTextExecution prepared, String content) {
        Objects.requireNonNull(prepared, "prepared");
        boolean valid = outputValidationDomainService.validateJsonRequired(
                prepared.getScenario().isJsonOutputRequired(), content);
        AiExecutionStatus status = valid ? AiExecutionStatus.SUCCEEDED : AiExecutionStatus.FAILED;
        return new AiExecutionBO(
                nextExecutionNo(),
                prepared.getScenario(),
                prepared.getModel().getModelCode(),
                status,
                content,
                prepared.getToolCodes());
    }

    @Override
    public AiExecutionBO finalizeVisionExecution(PreparedScenarioVisionExecution prepared, String content) {
        Objects.requireNonNull(prepared, "prepared");
        boolean valid = outputValidationDomainService.validateJsonRequired(true, content);
        AiExecutionStatus status = valid ? AiExecutionStatus.SUCCEEDED : AiExecutionStatus.FAILED;
        return new AiExecutionBO(
                nextExecutionNo(),
                prepared.getScenario(),
                prepared.getModel().getModelCode(),
                status,
                content,
                List.of());
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

    private AiScenarioBO resolveScenario(String scenarioCode, boolean workspaceChat) {
        if (workspaceChat) {
            return WorkspaceChatDomainPolicy.scenario();
        }
        return new AiScenarioBO(scenarioCode, "Market Daily Review", null, null, 6, true, true, true);
    }

    private AiModelBO resolveTextModel(String scenarioCode, String preferredModelCode, boolean workspaceChat) {
        if ("stock_information_analysis".equals(scenarioCode) || workspaceChat) {
            return routeInformationModel(
                    preferredModelCode,
                    workspaceChat
                            ? WorkspaceChatDomainPolicy.bindingCapabilities()
                            : List.of("stock_information_analysis", "application_analysis", "text_summary"),
                    workspaceChat);
        }
        return modelRouteDomainService.route(
                resolveScenario(scenarioCode, false),
                preferredModelCode);
    }

    private String nextExecutionNo() {
        return "AI-" + UUID.randomUUID().toString().replace("-", "");
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