package cn.cccs7.cynexus.ai.domain.stockselection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.stockselection.impl.StockSelectionAiDomainServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StockSelectionAiDomainServiceTest {

    @Test
    void fallsBackWhenTheFirstModelReturnsCandidateOutsideTrustedSnapshot() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_result":{"summary":"x","candidates":[{"code":"999999","thesis":"x"}]}}
                """, "provider-a", "model-a"));
        completion.responses.add(completion("""
                {"screening_result":{"summary":"Review the current momentum data.","market_context":"Daily close.","data_quality":"Snapshot only.","candidates":[{"code":"000001","thesis":"The supplied 30D return and liquidity fields warrant follow-up.","evidence":["30D return is present in the snapshot."],"risks":["No external catalyst data is included."],"watch":["Track whether daily liquidity remains available."]}],"disclaimer":"x"}}
                """, "provider-b", "model-b"));
        LogRepository logs = new LogRepository();
        StockSelectionAiDomainService service = service(completion, logs, List.of(model("provider-a", "model-a"), model("provider-b", "model-b")));

        StockSelectionAiDomainService.AnalysisResult result = service.analyze(
                new StockSelectionAiDomainService.AnalysisCommand("Review momentum", 10, Map.of("universe", "ALL")));

        assertThat(result.analysis().path("candidates").get(0).path("code").asText()).isEqualTo("000001");
        assertThat(result.analysis().path("candidates").get(0).path("name").asText()).isEqualTo("Ping An Bank");
        assertThat(result.providerCode()).isEqualTo("provider-b");
        assertThat(completion.calls).isEqualTo(2);
        assertThat(logs.logs).extracting(AiExecutionLogBO::getStatus).containsExactly("FAILED", "SUCCEEDED");
    }

    @Test
    void executesTheUserConfirmedCriteriaWithoutCallingIntentPlannerAgain() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_result":{"summary":"x","candidates":[{"code":"000001","thesis":"x"}]}}
                """, "provider", "model"));
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        service.analyze(new StockSelectionAiDomainService.AnalysisCommand(
                "Review the selected universe", 10, Map.of("universe", "ALL", "minVolumeRatio", 1.5)));

        assertThat(completion.calls).isOne();
    }

    @Test
    void rejectsUnsupportedCriteriaBeforeCallingMarketOrModel() {
        CompletionPort completion = new CompletionPort();
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        assertThatThrownBy(() -> service.analyze(new StockSelectionAiDomainService.AnalysisCommand(
                "", 10, Map.of("arbitrarySql", "select *"))))
                .hasMessageContaining("Unsupported screening criterion");
        assertThat(completion.calls).isZero();
    }

    @Test
    void validatesModelGeneratedEditablePriceVolumePlan() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_plan":{"criteria":{"volumeContractionDays":4,"minBreakoutVolumeMultiple":2.5,"minBullishBodyPct":4,"minCloseAboveMa20Pct":3,"breakoutHighDays":20,"maxAtrPct20":4},"applied_rules":["连续缩量后强放量阳线"],"assumptions":["持续缩量按至少 4 个交易日处理，强放量按至少 2.5 倍处理；均可编辑。"],"unsupported_phrases":[],"warnings":["仅支持已有日频指标。"]}}
                """, "provider", "model"));
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        StockSelectionAiDomainService.IntentPlan plan = service.parseIntent(new StockSelectionAiDomainService.IntentCommand(
                "持续缩量然后三倍爆量大阳线", Map.of("universe", "ALL")));

        assertThat(plan.criteria()).containsEntry("volumeContractionDays", new java.math.BigDecimal("4"))
                .containsEntry("minBreakoutVolumeMultiple", new java.math.BigDecimal("2.1"))
                .containsEntry("maxBreakoutVolumeMultiple", new java.math.BigDecimal("4.5"))
                .containsEntry("minBullishBodyPct", new java.math.BigDecimal("4"))
                .containsEntry("minChangePct", new java.math.BigDecimal("5"))
                .doesNotContainKeys("minCloseAboveMa20Pct", "breakoutHighDays", "maxAtrPct20");
        assertThat(plan.assumptions()).contains("未给出缩量天数时，按前序至少 3 个交易日连续缩量执行；该最小天数可编辑。",
                "明确倍数作为区间中心处理，而非精确等值；未给出倍数时使用宽松放量区间，均可编辑。",
                "未给出实体或涨幅时，按实体至少 4%、当日涨幅至少 5% 执行；两个阈值均可编辑。");
        assertThat(plan.providerCode()).isEqualTo("provider");
        assertThat(completion.calls).isOne();
    }

    @Test
    void preservesRichResearchConditionsWithoutSendingThemToTheTrustedExecutor() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_plan":{"criteria":{},"proposed_conditions":[
                {"category":"MOMENTUM","source_phrase":"MACD金叉","label":"MACD bullish crossover","definition":"MACD line crosses above signal line within the selected lookback.","execution_status":"RESEARCH_ONLY","data_requirements":["MACD line","signal line"]},
                {"category":"INDUSTRY","source_phrase":"行业强于大盘","label":"Industry relative strength","definition":"The stock's industry ranks above the broad market over the same period.","execution_status":"NEEDS_DATA","data_requirements":["industry benchmark return","market benchmark return"]}
                ],"applied_rules":[],"assumptions":[],"unsupported_phrases":[],"warnings":[]}}
                """, "provider", "model"));
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        StockSelectionAiDomainService.IntentPlan plan = service.parseIntent(new StockSelectionAiDomainService.IntentCommand(
                "MACD金叉且行业强于大盘", Map.of()));

        assertThat(plan.criteria()).isEmpty();
        assertThat(plan.proposedConditions()).hasSize(2);
        assertThat(plan.proposedConditions().get(0).executionStatus()).isEqualTo("RESEARCH_ONLY");
        assertThat(plan.proposedConditions().get(1).executionStatus()).isEqualTo("NEEDS_DATA");
        assertThat(completion.calls).isOne();
    }

    @Test
    void reconcilesSupportedNaturalLanguagePhrasesWhenTheModelMislabelsThem() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_plan":{"criteria":{"volumeContractionDays":2,"minBreakoutVolumeMultiple":3,"minBullishBodyPct":3},"applied_rules":[],"assumptions":[],"unsupported_phrases":["持续缩量","三倍爆量","大阳线"],"warnings":[]}}
                """, "provider", "model"));
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        StockSelectionAiDomainService.IntentPlan plan = service.parseIntent(new StockSelectionAiDomainService.IntentCommand(
                "持续缩量然后三倍爆量大阳线", Map.of("universe", "ALL")));

        assertThat(plan.criteria()).containsEntry("volumeContractionDays", new java.math.BigDecimal("3"))
                .containsEntry("minBreakoutVolumeMultiple", new java.math.BigDecimal("2.1"))
                .containsEntry("maxBreakoutVolumeMultiple", new java.math.BigDecimal("4.5"))
                .containsEntry("minBullishBodyPct", new java.math.BigDecimal("4"))
                .containsEntry("minChangePct", new java.math.BigDecimal("5"));
        assertThat(plan.unsupportedPhrases()).isEmpty();
    }

    @Test
    void omitsUnrequestedPatternsFromALiquidityOnlyIntent() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_plan":{"criteria":{"minAmount":500000000,"volumeContractionDays":5,"minBreakoutVolumeMultiple":2,"minBullishBodyPct":4,"minChangePct":5},"applied_rules":["流动性要求"],"assumptions":[],"unsupported_phrases":["成交量50亿以上"],"warnings":[]}}
                """, "provider", "model"));
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        StockSelectionAiDomainService.IntentPlan plan = service.parseIntent(new StockSelectionAiDomainService.IntentCommand(
                "成交量50亿以上", Map.of("universe", "ALL", "excludeSt", true)));

        assertThat(plan.criteria()).containsEntry("minAmount", new java.math.BigDecimal("5000000000"))
                .doesNotContainKeys("volumeContractionDays", "minBreakoutVolumeMultiple", "minBullishBodyPct", "minChangePct");
        assertThat(plan.unsupportedPhrases()).isEmpty();
        assertThat(plan.warnings()).anyMatch(item -> item.contains("已忽略"));
    }

    @Test
    void mapsStandaloneVolumeIntentToAnExecutableMa5VolumeFilter() {
        CompletionPort completion = new CompletionPort();
        completion.responses.add(completion("""
                {"screening_plan":{"criteria":{"minBreakoutVolumeMultiple":3},"applied_rules":[],"assumptions":[],"unsupported_phrases":[],"warnings":[]}}
                """, "provider", "model"));
        StockSelectionAiDomainService service = service(completion, new LogRepository(), List.of(model("provider", "model")));

        StockSelectionAiDomainService.IntentPlan plan = service.parseIntent(new StockSelectionAiDomainService.IntentCommand(
                "三倍爆量", Map.of()));

        assertThat(plan.criteria()).doesNotContainKey("minBreakoutVolumeMultiple")
                .containsEntry("minVolumeVsMa5Multiple", new java.math.BigDecimal("2.1"))
                .containsEntry("maxVolumeVsMa5Multiple", new java.math.BigDecimal("4.5"));
        assertThat(plan.assumptions()).contains("未指定缩量阶段时，放量按前五个交易日均量计算；明确倍数作为区间中心处理，均可编辑。");
    }

    private StockSelectionAiDomainService service(CompletionPort completion, LogRepository logs, List<AiModelBO> models) {
        cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver prompts = (capability, promptKey, system, template) ->
                new cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver.AiPromptRuntime(promptKey, "v1", system, template);
        return new StockSelectionAiDomainServiceImpl(new RoutingService(models), logs,
                (criteria, max) -> new StockSelectionAiCandidatePort.ScreeningSnapshot("2026-08-14", "market.screener", 100, 1,
                        List.of(Map.of("code", "000001", "name", "Ping An Bank", "returnPct30d", 12.4, "amount", 100000000L)), List.of()), completion, prompts);
    }

    private AiCompletionResultBO completion(String content, String provider, String model) {
        return AiCompletionResultBO.builder().content(content).providerCode(provider).modelCode(model)
                .inputTokens(10).outputTokens(20).totalTokens(30).build();
    }

    private AiModelBO model(String provider, String model) {
        return AiModelBO.builder().providerCode(provider).modelCode(model).build();
    }

    private static final class RoutingService implements ApplicationAnalysisRoutingDomainService {
        private final List<AiModelBO> models;
        private RoutingService(List<AiModelBO> models) { this.models = models; }
        @Override public AiUsageBindingBO findBinding(String capability) { return null; }
        @Override public List<AiModelBO> routeModels(String capability, String preferredModelCode) { return models; }
        @Override public String promptVersion(AiUsageBindingBO binding) { return ""; }
    }

    private static final class CompletionPort implements StockSelectionAiCompletionPort {
        private final List<AiCompletionResultBO> responses = new ArrayList<>();
        private int calls;
        @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) { return responses.get(calls++); }
    }

    private static final class LogRepository implements AiExecutionLogRepository {
        private final List<AiExecutionLogBO> logs = new ArrayList<>();
        @Override public Long create(AiExecutionLogBO log) { logs.add(log); return (long) logs.size(); }
    }
}
