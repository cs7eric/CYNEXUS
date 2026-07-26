package cn.cccs7.cynexus.ai.domain.service.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.agent.AgentConversationBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentMessageBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunEventBO;
import cn.cccs7.cynexus.ai.domain.entity.agent.AgentRunStepBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.repository.agent.AgentRunRepository;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentModelMessage;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolCall;
import cn.cccs7.cynexus.ai.domain.valueobject.agent.AgentToolResult;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentRunDomainServiceTest {

    @Test
    void recordsToolStepsInsideDomainWithRedactionAndSafeLinks() {
        RecordingRepository repository = new RecordingRepository();
        AgentRunDomainService service = new AgentRunDomainService(repository, null, null, null);

        AgentRunStepBO step = service.saveToolStep(
                "owner-1",
                "AGR-1",
                1,
                AgentToolCall.builder()
                        .toolCode("market.quote")
                        .arguments(Map.of("apiKey", "secret-value", "symbol", "600000"))
                        .build(),
                AgentToolResult.builder()
                        .toolCode("market.quote")
                        .status("FAILED")
                        .summary("failed with email user@example.com")
                        .outputJson("{\"token\":\"abc\",\"price\":12}")
                        .source("market")
                        .sourceUrl("javascript:alert(1)")
                        .warnings(List.of("timeout for user@example.com"))
                        .build(),
                2,
                "javascript:alert(1)",
                "realtime",
                "TIMEOUT");

        assertThat(repository.steps).containsExactly(step);
        assertThat(step.getInputJson()).contains("[REDACTED]").doesNotContain("secret-value");
        assertThat(step.getOutputJson()).contains("[REDACTED]").doesNotContain("abc");
        assertThat(step.getSummary()).doesNotContain("user@example.com");
        assertThat(step.getErrorMessage()).doesNotContain("user@example.com");
        assertThat(step.getSourceUrl()).isNull();
        assertThat(step.getFailureCategory()).isEqualTo("TIMEOUT");
    }

    @Test
    void persistsMessagesAndContinuesFromDatabaseSequence() {
        RecordingRepository repository = new RecordingRepository();
        AgentRunDomainService service = new AgentRunDomainService(repository, null, null, null);

        service.saveMessage("AGC-1", "AGR-1", 5,
                AgentModelMessage.builder().role("user").content("email user@example.com").build());

        assertThat(repository.messages).hasSize(1);
        assertThat(repository.messages.get(0).getContent()).isEqualTo("email [REDACTED]");
        assertThat(AgentRunDomainService.nextMessageSequence(List.of(
                AgentMessageBO.builder().sequenceNo(1).build(),
                AgentMessageBO.builder().sequenceNo(9).build()))).isEqualTo(9);
    }

    @Test
    void persistsReplayStateWithSensitiveArgumentsRedactedAndOpaqueReasoningPreserved() {
        RecordingRepository repository = new RecordingRepository();
        AgentRunDomainService service = new AgentRunDomainService(repository, null, null, null);

        service.saveMessage("AGC-1", "AGR-1", 6, AgentModelMessage.builder()
                .role("assistant")
                .runtimeContext("runtime for user@example.com")
                .providerOutputJson("[{\"type\":\"reasoning\",\"encrypted_content\":\"opaque-state\"},"
                        + "{\"type\":\"function_call\",\"arguments\":{\"apiKey\":\"secret-value\"}}]")
                .build());

        AgentMessageBO persisted = repository.messages.get(0);
        assertThat(persisted.getRuntimeContext()).isEqualTo("runtime for [REDACTED]");
        assertThat(persisted.getProviderOutputJson())
                .contains("opaque-state", "[REDACTED]")
                .doesNotContain("secret-value");
        AgentModelMessage reconstructed = service.toModelMessage(persisted);
        assertThat(reconstructed.getRuntimeContext()).isEqualTo(persisted.getRuntimeContext());
        assertThat(reconstructed.getProviderOutputJson()).isEqualTo(persisted.getProviderOutputJson());
    }

    @Test
    void replaysOnlyCompletedPriorRunsAsVisibleConversation() {
        RecordingRepository repository = new RecordingRepository();
        repository.runs.put("AGR-ok", AgentRunBO.builder().runNo("AGR-ok").status("SUCCEEDED").build());
        repository.runs.put("AGR-failed", AgentRunBO.builder().runNo("AGR-failed").status("FAILED").build());
        List<AgentMessageBO> persisted = List.of(
                AgentMessageBO.builder().runNo("AGR-ok").sequenceNo(1).role("user").content("q1").build(),
                AgentMessageBO.builder().runNo("AGR-ok").sequenceNo(2).role("assistant").content("")
                        .providerOutputJson("[{\"type\":\"function_call\"}]").build(),
                AgentMessageBO.builder().runNo("AGR-ok").sequenceNo(3).role("tool").content("result").build(),
                AgentMessageBO.builder().runNo("AGR-ok").sequenceNo(4).role("assistant").content("a1")
                        .runtimeContext("runtime").providerOutputJson("[{\"type\":\"message\"}]").build(),
                AgentMessageBO.builder().runNo("AGR-failed").sequenceNo(5).role("user").content("q2").build());

        List<AgentModelMessage> replay = new AgentRunDomainService(repository, null, null, null)
                .collapsePersistedHistoryForProvider("owner-1", persisted);

        assertThat(replay).extracting(AgentModelMessage::getContent).containsExactly("q1", "a1");
        assertThat(replay).allSatisfy(message -> {
            assertThat(message.getRuntimeContext()).isNull();
            assertThat(message.getProviderOutputJson()).isNull();
        });
    }

    @Test
    void archivesConversationThroughTheRepositoryPort() {
        RecordingRepository repository = new RecordingRepository();
        AgentRunDomainService service = new AgentRunDomainService(repository, null, null, null);

        assertThat(service.archiveConversation("owner-1", "AGC-1")).isTrue();
        assertThat(repository.archivedConversation).isEqualTo("owner-1/AGC-1");
        assertThat(repository.archivedAt).isNotNull();
    }

    @Test
    void appliesReasoningOverrideToRuntimeModelWithoutChangingProviderConfiguration() {
        AiProviderBO provider = AiProviderBO.builder().id(7L).code("openai").providerType("openai")
                .enabled(true).defaultModel("gpt-5")
                .extra("{\"model_reasoning_effort\":\"low\",\"disable_response_storage\":true}").build();
        AiProviderRepository providers = new AiProviderRepository() {
            @Override public List<AiProviderBO> listAll() { return List.of(provider); }
            @Override public AiProviderBO findById(Long id) { return provider; }
            @Override public Long create(AiProviderBO value) { return 1L; }
            @Override public boolean update(AiProviderBO value) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };
        AiUsageBindingRepository bindings = new AiUsageBindingRepository() {
            private final AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("autonomous_agent")
                    .providerId(7L).enabled(true).build();
            @Override public List<AiUsageBindingBO> listAll() { return List.of(binding); }
            @Override public AiUsageBindingBO findById(Long id) { return binding; }
            @Override public Long create(AiUsageBindingBO value) { return 1L; }
            @Override public boolean update(AiUsageBindingBO value) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };

        AiModelBO model = new AgentRunDomainService(new RecordingRepository(), null, providers, bindings)
                .resolveModel(null, "gpt-5", "high");

        assertThat(model.getExtra()).contains("\"model_reasoning_effort\":\"high\"")
                .contains("\"disable_response_storage\":true");
        assertThat(provider.getExtra()).contains("\"model_reasoning_effort\":\"low\"");
    }

    @Test
    void resolvesBindingReasoningBeforeProviderDefaultAndFallsBackToLow() {
        AiProviderBO provider = AiProviderBO.builder().id(7L).code("openai")
                .providerType("openai").enabled(true).defaultModel("gpt-5")
                .extra("{}").build();
        AiUsageBindingBO binding = AiUsageBindingBO.builder().capability("autonomous_agent")
                .providerId(7L).enabled(true).params("{\"reasoning_effort\":\"high\"}").build();
        AiProviderRepository providers = providerRepository(provider);
        AiUsageBindingRepository bindings = usageBindingRepository(binding);

        AiModelBO configured = new AgentRunDomainService(new RecordingRepository(), null, providers, bindings)
                .resolveModel(null, null, null);
        assertThat(configured.getExtra()).contains("\"model_reasoning_effort\":\"high\"");

        binding.setParams("{}");
        AiModelBO fallback = new AgentRunDomainService(new RecordingRepository(), null, providers, bindings)
                .resolveModel(null, null, null);
        assertThat(fallback.getExtra()).contains("\"model_reasoning_effort\":\"low\"");
    }

    private AiProviderRepository providerRepository(AiProviderBO provider) {
        return new AiProviderRepository() {
            @Override public List<AiProviderBO> listAll() { return List.of(provider); }
            @Override public AiProviderBO findById(Long id) { return provider; }
            @Override public Long create(AiProviderBO value) { return 1L; }
            @Override public boolean update(AiProviderBO value) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };
    }

    private AiUsageBindingRepository usageBindingRepository(AiUsageBindingBO binding) {
        return new AiUsageBindingRepository() {
            @Override public List<AiUsageBindingBO> listAll() { return List.of(binding); }
            @Override public AiUsageBindingBO findById(Long id) { return binding; }
            @Override public Long create(AiUsageBindingBO value) { return 1L; }
            @Override public boolean update(AiUsageBindingBO value) { return true; }
            @Override public boolean delete(Long id) { return true; }
        };
    }

    private static final class RecordingRepository implements AgentRunRepository {
        private final List<AgentRunStepBO> steps = new ArrayList<>();
        private final List<AgentMessageBO> messages = new ArrayList<>();
        private final Map<String, AgentRunBO> runs = new LinkedHashMap<>();
        private String archivedConversation;
        private OffsetDateTime archivedAt;

        @Override public void saveConversation(AgentConversationBO conversation) { }
        @Override public Optional<AgentConversationBO> findConversation(String ownerKey, String conversationNo) { return Optional.empty(); }
        @Override public List<AgentConversationBO> listConversations(String ownerKey, int limit, String query) { return List.of(); }
        @Override public void saveRun(AgentRunBO run) { }
        @Override public void updateRun(AgentRunBO run) { }
        @Override public void updateConversation(AgentConversationBO conversation) { }
        @Override public boolean archiveConversation(String ownerKey, String conversationNo, OffsetDateTime archivedAt) {
            this.archivedConversation = ownerKey + "/" + conversationNo;
            this.archivedAt = archivedAt;
            return true;
        }
        @Override public void saveStep(AgentRunStepBO step) { steps.add(step); }
        @Override public Optional<AgentRunBO> findRun(String ownerKey, String runNo) {
            return Optional.ofNullable(runs.get(runNo));
        }
        @Override public Optional<AgentRunBO> findRunByIdempotencyKey(String ownerKey, String idempotencyKey) { return Optional.empty(); }
        @Override public Optional<AgentRunBO> requestCancellation(String ownerKey, String runNo, String requestedBy) { return Optional.empty(); }
        @Override public boolean isCancellationRequested(String ownerKey, String runNo) { return false; }
        @Override public List<AgentRunStepBO> findSteps(String ownerKey, String runNo) { return List.copyOf(steps); }
        @Override public void saveMessage(AgentMessageBO message) { messages.add(message); }
        @Override public List<AgentMessageBO> findMessages(String ownerKey, String conversationNo) { return List.copyOf(messages); }
        @Override public List<AgentRunBO> findStaleRunningRuns(OffsetDateTime cutoff) { return List.of(); }
        @Override public void saveEvent(String ownerKey, AgentRunEventBO event) { }
    }
}
