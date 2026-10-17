package cn.cccs7.cynexus.ai.domain.mediatranscription;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.ai.domain.mediatranscription.impl.MediaTranscriptionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiModelExecutor;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;

class MediaTranscriptionDomainServiceTest {

    @Test
    void emptyPolishDoesNotCallTheCompletionPort() {
        StubCompletion completion = new StubCompletion();
        MediaTranscriptionDomainService service = service(completion, content -> "{}");

        assertThat(service.polish(new MediaTranscriptionDomainService.TextCommand("  ", null))).isEmpty();
        assertThat(completion.calls).isZero();
    }

    @Test
    void metadataUsesStructuredOutputPortAndReturnsDomainMetadata() {
        StubCompletion completion = new StubCompletion();
        MediaTranscriptionDomainService service = service(completion, content ->
                "{\"title\":\"整理后的标题\",\"categories\":[\"投资\"],\"tags\":[\"趋势\"]}");

        MediaTranscriptionMetadataBO result = service.metadata(new MediaTranscriptionDomainService.MetadataCommand("正文", "摘要", null));

        assertThat(result.title()).isEqualTo("整理后的标题");
        assertThat(result.categories()).containsExactly("投资");
        assertThat(result.tags()).containsExactly("趋势");
        assertThat(completion.calls).isEqualTo(1);
    }

    @Test
    void enrichmentUsesOneStructuredCallAndReturnsKnowledgeFields() {
        StubCompletion completion = new StubCompletion();
        completion.content = "{\"polishedText\":\"润色正文\",\"summary\":\"详细摘要\",\"shortSummary\":\"短摘要\",\"keyPoints\":[\"观点\"],\"topics\":[\"复盘\"],\"methodology\":[\"步骤\"],\"actionItems\":[\"行动\"],\"risks\":[\"风险\"],\"title\":\"标题\",\"categories\":[\"复盘\"],\"tags\":[\"A股\"]}";
        MediaTranscriptionDomainService service = service(completion, content -> content);

        var result = service.enrich(new MediaTranscriptionEnrichmentCommand("原始文本", "原题", "平台", 60, null));

        assertThat(completion.calls).isEqualTo(1);
        assertThat(result.polishedText()).isEqualTo("润色正文");
        assertThat(result.categories()).containsExactly("复盘");
        assertThat(result.keyPoints()).containsExactly("观点");
    }

    private MediaTranscriptionDomainService service(StubCompletion completion,
                                                    MediaTranscriptionStructuredOutputPort outputPort) {
        AiPromptRuntimeResolver prompts = (capability, key, system, user) ->
                new AiPromptRuntimeResolver.AiPromptRuntime(key, "test", system, user);
        return new MediaTranscriptionDomainServiceImpl(new Routing(), completion, prompts,
                log -> 1L, outputPort);
    }

    private static final class Routing implements ApplicationAnalysisRoutingDomainService {
        @Override public AiUsageBindingBO findBinding(String capability) { return null; }
        @Override public List<AiModelBO> routeModels(String capability, String preferredModelCode) {
            return List.of(AiModelBO.builder().providerCode("provider").modelCode("model").build());
        }
        @Override public String promptVersion(AiUsageBindingBO binding) { return null; }
    }

    private static final class StubCompletion implements AiModelExecutor {
        private int calls;
        private String content = "generated";
        @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
            calls++;
            return AiCompletionResultBO.builder().content(content).inputTokens(1).outputTokens(1).totalTokens(2).build();
        }
    }

}
