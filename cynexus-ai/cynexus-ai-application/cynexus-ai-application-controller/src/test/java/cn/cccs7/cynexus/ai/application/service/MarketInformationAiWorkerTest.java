package cn.cccs7.cynexus.ai.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.MarketInformationAiTaskBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.MarketInformationAiTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketInformationAiWorkerTest {

    @Test
    void deepSeekBudgetCountsEveryAllowedProviderRequest() throws Exception {
        MarketInformationAiWorker worker = worker(20);

        for (int index = 0; index < 20; index++) {
            assertTrue(worker.allowDeepSeekCall());
        }
        assertFalse(worker.allowDeepSeekCall());
    }

    @Test
    void zeroBudgetDisablesDeepSeekWithoutConsumingRequests() throws Exception {
        MarketInformationAiWorker worker = worker(0);

        assertFalse(worker.allowDeepSeekCall());
    }

    @Test
    void negativeBudgetAllowsUnlimitedReleaseRequests() {
        MarketInformationAiWorker worker = worker(-1);

        for (int index = 0; index < 25; index++) {
            assertTrue(worker.allowDeepSeekCall());
        }
    }

    @Test
    void customProviderUsesTypeForAdapterAndCodeForIdentity() {
        MarketInformationAiWorker worker = worker(0);
        AiProviderBO provider = AiProviderBO.builder()
                .code("custom-yancc")
                .providerType("custom")
                .defaultModel("gpt-5.4")
                .build();

        AiModelBO model = worker.model(provider, "gpt-5.4");

        assertEquals("custom", model.getProviderCode());
        assertEquals("custom-yancc", model.getProviderIdentity());
    }

    @Test
    void explicitPromptBindingTakesPrecedenceOverFallbackVersion() {
        AiPromptRepository prompts = new AiPromptRepository() {
            @Override public List<AiPromptBO> list(String keyword, String scenarioCode, String status) { return List.of(); }
            @Override public AiPromptBO findById(Long id) {
                return AiPromptBO.builder().id(id).promptKey("managed-event-prompt").promptVersion("v9")
                        .status("active").systemPrompt("managed system").userPromptTemplate("{{sources}}").build();
            }
            @Override public Long create(AiPromptBO prompt) { return 0L; }
            @Override public boolean update(AiPromptBO prompt) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };

        MarketInformationAiWorker worker = worker(0, prompts);
        MarketInformationAiWorker.PromptRuntime resolved = worker.resolvePrompt(
                cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO.builder()
                        .capability("market_information_event_analysis").promptId(42L).build(), "v4");

        assertEquals("managed-event-prompt", resolved.promptKey());
        assertEquals("v9", resolved.promptVersion());
        assertEquals("managed system", resolved.systemPrompt());
    }

    @Test
    void summaryOnlyPromptForbidsInferringMissingDetails() throws Exception {
        MarketInformationAiTaskBO task = MarketInformationAiTaskBO.builder()
                .title("政策标题")
                .category("policy")
                .sources(List.of(MarketInformationAiTaskBO.Source.builder()
                        .sourceId("official:1")
                        .source("official")
                        .title("政策标题")
                        .content("政策标题")
                        .contentQuality("summary_only")
                        .build()))
                .build();

        String prompt = worker(0).eventPrompt(task);

        assertTrue(prompt.contains("summary_only"));
        assertTrue(prompt.contains("exact contiguous substring"));
        assertTrue(prompt.contains("not infer article details"));
        assertTrue(prompt.contains("informationDelta"));
        assertTrue(prompt.contains("transmissionPaths"));
        assertTrue(prompt.contains("marketExpectationGap"));
        assertTrue(prompt.contains("event background"));
        assertTrue(prompt.contains("information change"));
    }

    @Test
    void briefFallbackPromptExplainsSnapshotBackgroundBeforeSynthesis() {
        MarketInformationAiWorker worker = worker(0);

        MarketInformationAiWorker.PromptRuntime prompt = worker.resolvePrompt(
                cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO.builder()
                        .capability("market_information_daily_brief").build(), "v6");

        assertTrue(prompt.systemPrompt().contains("current input snapshot"));
        assertTrue(prompt.systemPrompt().contains("First understand the background"));
        assertTrue(prompt.systemPrompt().contains("executiveSummary"));
        assertTrue(prompt.userPromptTemplate().contains("This is the only factual input"));
    }

    @Test
    void validatesEventImpactProtocolAndVerbatimEvidence() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MarketInformationAiTaskBO task = MarketInformationAiTaskBO.builder()
                .sources(List.of(MarketInformationAiTaskBO.Source.builder()
                        .sourceId("cninfo:1").source("cninfo").contentQuality("full_text")
                        .content("公司签订金额占上年度营业收入45%的重大订单。")
                        .build()))
                .build();
        JsonNode event = mapper.readTree("""
                {"summary":"公司签订重大订单，收入兑现仍取决于后续履约。",
                 "eventFacts":["公司签订重大订单"],
                 "informationDelta":"新增重大订单已签订。",
                 "whyItMatters":"订单若顺利履约，可能影响后续收入确认。",
                 "transmissionPaths":["订单履约 -> 收入确认 -> 盈利表现"],
                 "marketExpectationGap":"unknown","eventType":"company",
                 "affectedScopes":["公司经营"],"affectedIndustries":[],"affectedCompanies":["公司"],"entities":["公司"],
                 "marketDirection":"positive","materiality":"medium","timeHorizon":"medium","novelty":"new",
                 "catalysts":["履约进度披露"],
                 "scenarios":{"bull":"订单按期履约。","base":"按合同节奏执行。","bear":"履约或回款不及预期。"},
                 "watchItems":["合同履约进度","回款情况"],"confidence":0.78,"contentQuality":"full_text",
                 "evidence":[{"claim":"订单规模具有重要性","quote":"金额占上年度营业收入45%","sourceId":"cninfo:1"}],
                 "risks":["履约风险"],"uncertainties":["未提供收入确认节奏"],
                 "disclaimer":"Information analysis only; not investment advice."}
                """);

        assertEquals(event, worker(0).validateEvent(event, task));
    }

    @Test
    void validatesStructuredBriefAgainstCurrentInputSnapshot() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode input = mapper.readTree("""
                {"analysisWindow":{"from":"2026-07-14T08:00:00Z","to":"2026-07-14T10:00:00Z","eventCount":1},
                 "sourceCoverage":{"gov.cn.policy":1},"events":[{"clusterId":101}]}
                """);
        JsonNode brief = mapper.readTree("""
                {"headline":"Policy signal","executiveSummary":"One current policy event.",
                 "marketTone":{"direction":"neutral","confidence":0.6,"rationale":"Limited title-only evidence."},
                 "keyEvents":[{"clusterId":101,"title":"Policy","summary":"Policy event published.","eventType":"policy","direction":"neutral","materiality":"medium","timeHorizon":"medium","affectedScopes":["environment"],"confidence":0.6}],
                 "policySignals":[{"summary":"Policy signal","direction":"neutral","materiality":"medium","affectedScopes":["environment"],"eventReferences":[101]}],
                 "industrySignals":[],"companySignals":[],
                 "riskSignals":[{"summary":"Details remain unavailable.","severity":"low","timeHorizon":"unknown","affectedScopes":[],"eventReferences":[101]}],
                 "uncertainties":["Full text is unavailable."],
                 "sourceCoverage":{"gov.cn.policy":1},
                 "analysisWindow":{"from":"2026-07-14T08:00:00Z","to":"2026-07-14T10:00:00Z","eventCount":1},
                 "eventReferences":[101],
                 "disclaimer":"Information analysis only; not investment advice."}
                """);

        assertEquals(brief, worker(0).validateBrief(brief, input));
    }

    @Test
    void rejectsBriefReferencesOutsideCurrentInputSnapshot() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode input = mapper.readTree("""
                {"analysisWindow":{"from":"2026-07-14T08:00:00Z","to":"2026-07-14T10:00:00Z","eventCount":1},
                 "sourceCoverage":{"official":1},"events":[{"clusterId":101}]}
                """);
        JsonNode brief = mapper.readTree("""
                {"headline":"Brief","executiveSummary":"Summary",
                 "marketTone":{"direction":"neutral","confidence":0.5,"rationale":"Reason"},
                 "keyEvents":[{"clusterId":999,"title":"Stale","summary":"Stale","eventType":"market","direction":"neutral","materiality":"low","timeHorizon":"unknown","affectedScopes":[],"confidence":0.5}],
                 "policySignals":[],"industrySignals":[],"companySignals":[],"riskSignals":[],"uncertainties":[],
                 "sourceCoverage":{"official":1},
                 "analysisWindow":{"from":"2026-07-14T08:00:00Z","to":"2026-07-14T10:00:00Z","eventCount":1},
                 "eventReferences":[999],
                 "disclaimer":"Information analysis only; not investment advice."}
                """);

        assertThrows(IllegalArgumentException.class, () -> worker(0).validateBrief(brief, input));
    }

    @Test
    void skipsQueuedBriefWhenItsInputHashIsNoLongerCurrent() {
        MarketInformationAiWorker worker = worker(0);
        MarketInformationAiTaskBO stale = MarketInformationAiTaskBO.builder()
                .clusterContentHash("old-input-hash")
                .build();

        assertTrue(worker.isStaleBriefTask(stale, "current input"));
        assertFalse(worker.isStaleBriefTask(MarketInformationAiTaskBO.builder().build(), "current input"));
    }

    private MarketInformationAiWorker worker(int limit) {
        return worker(limit, null);
    }

    private MarketInformationAiWorker worker(int limit, AiPromptRepository prompts) {
        return new MarketInformationAiWorker(new BudgetRepository(), null, prompts, null, null, null, limit, 10);
    }

    private static final class BudgetRepository implements MarketInformationAiTaskRepository {
        private int requests;
        @Override public boolean tryReserveProviderRequest(String providerCode, String usageScope, int limit) { return requests < limit && ++requests > 0; }
        @Override public int recoverStaleTasks(int timeoutMinutes) { return 0; }
        @Override public MarketInformationAiTaskBO claimNext(String workerId) { return null; }
        @Override public MarketInformationAiTaskBO loadInput(MarketInformationAiTaskBO task) { return task; }
        @Override public String loadBriefInput(String eventSchemaVersion, String eventPromptVersion) { return null; }
        @Override public void saveEventAnalysis(MarketInformationAiTaskBO task, String schemaVersion, String promptVersion, String providerCode, String modelCode, String analysisJson, String responsePreview, boolean fallbackUsed) { }
        @Override public void saveDailyBrief(MarketInformationAiTaskBO task, String schemaVersion, String promptVersion, String providerCode, String modelCode, String briefJson, boolean fallbackUsed) { }
        @Override public void markSucceeded(long taskId) { }
        @Override public void markFailed(MarketInformationAiTaskBO task, String errorMessage) { }
        @Override public void enqueueDailyBrief(String eventSchemaVersion, String eventPromptVersion) { }
    }
}
