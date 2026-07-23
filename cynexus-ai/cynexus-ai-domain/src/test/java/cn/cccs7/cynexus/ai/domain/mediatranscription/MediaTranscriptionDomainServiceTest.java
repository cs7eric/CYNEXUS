package cn.cccs7.cynexus.ai.domain.mediatranscription;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.entity.MediaTranscriptionMetadataBO;
import cn.cccs7.cynexus.ai.domain.mediatranscription.impl.MediaTranscriptionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class MediaTranscriptionDomainServiceTest {

    @Test
    void emptyPolishDoesNotCallTheCompletionPort() {
        StubCompletion completion = new StubCompletion();
        MediaTranscriptionDomainService service = service(completion, new StubPrompts(), content -> "{}");

        assertThat(service.polish(new MediaTranscriptionDomainService.TextCommand("  ", null))).isEmpty();
        assertThat(completion.calls).isZero();
    }

    @Test
    void metadataUsesStructuredOutputPortAndReturnsDomainMetadata() {
        StubCompletion completion = new StubCompletion();
        MediaTranscriptionDomainService service = service(completion, new StubPrompts(), content ->
                "{\"title\":\"整理后的标题\",\"categories\":[\"投资\"],\"tags\":[\"趋势\"]}");

        MediaTranscriptionMetadataBO result = service.metadata(new MediaTranscriptionDomainService.MetadataCommand("正文", "摘要", null));

        assertThat(result.title()).isEqualTo("整理后的标题");
        assertThat(result.categories()).containsExactly("投资");
        assertThat(result.tags()).containsExactly("趋势");
        assertThat(completion.calls).isEqualTo(1);
    }

    private MediaTranscriptionDomainService service(StubCompletion completion, AiPromptRepository prompts,
                                                    MediaTranscriptionStructuredOutputPort outputPort) {
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

    private static final class StubCompletion implements MediaTranscriptionCompletionPort {
        private int calls;
        @Override public AiCompletionResultBO complete(AiModelBO model, String prompt, String capability) {
            calls++;
            return AiCompletionResultBO.builder().content("generated").inputTokens(1).outputTokens(1).totalTokens(2).build();
        }
    }

    private static final class StubPrompts implements AiPromptRepository {
        @Override public List<AiPromptBO> list(String keyword, String scenarioCode, String status) { return List.of(); }
        @Override public AiPromptBO findById(Long id) { return null; }
        @Override public Long create(AiPromptBO prompt) { return 1L; }
        @Override public boolean update(AiPromptBO prompt) { return true; }
        @Override public boolean delete(Long id) { return true; }
    }
}
