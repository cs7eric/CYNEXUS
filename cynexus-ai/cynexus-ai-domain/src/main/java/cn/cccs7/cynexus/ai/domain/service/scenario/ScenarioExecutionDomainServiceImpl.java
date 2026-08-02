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
        String capability = "portfolio_trade_screenshot_parse".equals(resolvedCode)
                ? "portfolio_trade_screenshot_parse" : "accounting_receipt_parse";
        AiScenarioBO scenario = AiScenarioBO.builder()
                .scenarioCode(resolvedCode)
                .scenarioName("portfolio_trade_screenshot_parse".equals(capability)
                        ? "Portfolio Trade Screenshot Recognition" : "Accounting Receipt Recognition")
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
            if (prompt != null && AiConstants.STATUS_ACTIVE.equalsIgnoreCase(prompt.getStatus())) {
                String userPrompt = prompt.getUserPromptTemplate() == null
                        || prompt.getUserPromptTemplate().isBlank()
                        ? candidateContext
                        : prompt.getUserPromptTemplate().replace(
                                "{{candidate_context}}", candidateContext == null ? "" : candidateContext);
                return prompt.getSystemPrompt() + System.lineSeparator() + userPrompt;
            }
        }
        return "portfolio_trade_screenshot_parse".equals(capability)
                ? defaultPortfolioTradePrompt(candidateContext == null ? "" : candidateContext)
                : defaultVisionPrompt(candidateContext);
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

    private String defaultPortfolioTradePrompt(String candidateContext) {
        return """
                你是 Cynexus 个人持仓交易截图解析器。提取券商、银行或交易软件中明确可见的成交事实，支持沪深北 A 股股票、交易所上市 ETF、LOF 和基金份额。
                标题或标签包含历史成交、成交明细、成交记录、当日成交、交易记录、委托成交，或图片中出现多行证券名称/代码、成交日期或时间、买入/卖出、成交价、成交量/数量、成交额/金额等组合，都属于交易截图。
                只要图片中存在一行或多行可辨认的股票或 ETF 成交记录，isTradeScreenshot 必须为 true；证券代码缺失、只有证券名称、名称包含 ETF、日期只有部分或费用未展示，都不能把图片判为非交易截图。
                一张图片可能包含多笔交易，必须拆分为 candidates 数组。每条都必须给出 instrumentType：普通 A 股股票为 STOCK；交易所上市 ETF（即使名称没有“ETF”字样，例如科创、消费电子、电池等主题 ETF）为 ETF。不要根据六位代码前缀猜测类别，优先使用证券名称和券商页面的基金/ETF 标识；无法可靠判断时使用 null 并在 warnings 中说明。
                将买入、买、买进映射为 BUY，将卖出、卖、卖出映射为 SELL。字段不可辨认时返回 null 并在 warnings 中说明，不要猜测、补全或计算持仓收益。
                只输出合法 JSON，不要 Markdown、思考过程或额外文字。格式：{"isTradeScreenshot":true,"candidates":[{"rowNo":1,"symbol":null,"securityName":null,"instrumentType":"STOCK","side":null,"tradedAt":null,"quantity":null,"price":null,"fee":null,"externalTradeNo":null,"confidence":0.0,"evidence":{},"warnings":[]}],"warnings":[]}。
                """ + (candidateContext == null ? "" : candidateContext);
    }
}
