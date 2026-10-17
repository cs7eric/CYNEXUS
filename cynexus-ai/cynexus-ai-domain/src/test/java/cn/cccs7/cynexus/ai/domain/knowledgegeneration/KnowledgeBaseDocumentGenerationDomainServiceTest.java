package cn.cccs7.cynexus.ai.domain.knowledgegeneration;

import static org.assertj.core.api.Assertions.assertThat;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.domain.knowledgebase.KnowledgeBaseDocumentDraftPolicy;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.impl.KnowledgeBaseDocumentGenerationDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationOutputParserPort;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class KnowledgeBaseDocumentGenerationDomainServiceTest {

    @Test
    void normalizesInputAndReturnsOnlyParsedAiOutput() {
        List<String> prompts = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        KnowledgeBaseDocumentGenerationDomainService service = service(
                outputParser(raw -> new KnowledgeBaseGenerationParsedDocument(
                        "Generated title", "Generated description", List.of("ai"),
                        List.of("Research"), "# Generated")),
                (model, prompt, capability) -> {
                    calls.incrementAndGet();
                    prompts.add(prompt);
                    return completion("ignored");
                });

        KnowledgeBaseGenerationOutcome outcome = service.generate(new KnowledgeBaseGenerationCommand(
                "  source text  ", null, null, null, null));

        assertThat(outcome.status()).isEqualTo(KnowledgeBaseGenerationStatus.SUCCEEDED);
        assertThat(outcome.sourceText()).isEqualTo("source text");
        assertThat(outcome.draft().title()).isEqualTo("Generated title");
        assertThat(outcome.draft().content()).isEqualTo("# Generated");
        assertThat(outcome.attempts()).hasSize(1);
        assertThat(calls).hasValue(1);
        assertThat(prompts.getFirst()).contains("source text");
    }

    @Test
    void retriesMalformedStructuredOutputWithRepairPrompt() {
        List<String> prompts = new ArrayList<>();
        KnowledgeBaseGenerationParsedDocument repaired = new KnowledgeBaseGenerationParsedDocument(
                "Repaired title", "Repaired description", List.of("repair"),
                List.of("Knowledge"), "# Repaired");
        KnowledgeBaseGenerationOutputParserPort parser = new KnowledgeBaseGenerationOutputParserPort() {
            private int index;

            @Override
            public String formatInstructions() {
                return "JSON format";
            }

            @Override
            public KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception {
                if (index++ == 0) {
                    throw new IllegalArgumentException("invalid json");
                }
                return repaired;
            }
        };
        KnowledgeBaseDocumentGenerationDomainService service = service(parser, (model, prompt, capability) -> {
            prompts.add(prompt);
            return completion(prompts.size() == 1 ? "malformed" : "repaired");
        });

        KnowledgeBaseGenerationOutcome outcome = service.generate(command("repair source"));

        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.draft().title()).isEqualTo("Repaired title");
        assertThat(prompts).hasSize(2);
        assertThat(prompts.get(1)).contains("修复后的合法 JSON");
        assertThat(outcome.attempts()).singleElement().extracting(KnowledgeBaseGenerationAttempt::status)
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void returnsFailureWithoutLocalPseudoSuccessWhenAllAiAttemptsFail() {
        AtomicInteger calls = new AtomicInteger();
        KnowledgeBaseGenerationOutputParserPort parser = outputParser(raw -> {
            throw new IllegalArgumentException("invalid structured output");
        });
        KnowledgeBaseDocumentGenerationDomainService service = service(parser, (model, prompt, capability) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("provider unavailable");
        });

        KnowledgeBaseGenerationOutcome outcome = service.generate(command("important source"));

        assertThat(outcome.status()).isEqualTo(KnowledgeBaseGenerationStatus.FAILED);
        assertThat(outcome.draft()).isNull();
        assertThat(outcome.failureReason()).contains("provider unavailable");
        assertThat(outcome.attempts()).singleElement().extracting(KnowledgeBaseGenerationAttempt::status)
                .isEqualTo("FAILED");
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsBlankInputBeforeResolvingAnyModel() {
        KnowledgeBaseDocumentGenerationDomainService service = service(
                outputParser(raw -> new KnowledgeBaseGenerationParsedDocument(
                        "unused", "unused", List.of(), List.of(), "unused")),
                (model, prompt, capability) -> {
                    throw new AssertionError("completion must not be called");
                });

        KnowledgeBaseGenerationOutcome outcome = service.generate(command("  \n  "));

        assertThat(outcome.status()).isEqualTo(KnowledgeBaseGenerationStatus.INVALID_INPUT);
        assertThat(outcome.failureReason()).isEqualTo("sourceText is required");
        assertThat(outcome.attempts()).isEmpty();
    }

    private KnowledgeBaseDocumentGenerationDomainService service(
            KnowledgeBaseGenerationOutputParserPort parser,
            cn.cccs7.cynexus.ai.domain.knowledgegeneration.port.KnowledgeBaseGenerationCompletionPort completion) {
        AiModelBO model = AiModelBO.builder().modelCode("test-model").providerCode("test-provider").enabled(true).build();
        return new KnowledgeBaseDocumentGenerationDomainServiceImpl(
                new StubUsageBindingRepository(),
                new StubProviderRepository(),
                log -> 1L,
                preferred -> model,
                raw -> KnowledgeBaseGenerationRoutingParameters.empty(),
                completion,
                parser,
                new KnowledgeBaseDocumentDraftPolicy(),
                (capability, promptKey, system, template) -> new cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver.AiPromptRuntime(promptKey, "v1", system, template));
    }

    private KnowledgeBaseGenerationCommand command(String source) {
        return new KnowledgeBaseGenerationCommand(source, null, null, null, null);
    }

    private KnowledgeBaseGenerationOutputParserPort outputParser(ParserFunction parser) {
        return new KnowledgeBaseGenerationOutputParserPort() {
            @Override
            public String formatInstructions() {
                return "JSON format";
            }

            @Override
            public KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception {
                return parser.parse(rawOutput);
            }
        };
    }

    private AiCompletionResultBO completion(String content) {
        return AiCompletionResultBO.builder()
                .content(content)
                .providerCode("test-provider")
                .modelCode("test-model")
                .inputTokens(1)
                .outputTokens(1)
                .totalTokens(2)
                .build();
    }

    @FunctionalInterface
    private interface ParserFunction {
        KnowledgeBaseGenerationParsedDocument parse(String rawOutput) throws Exception;
    }

    private static final class StubUsageBindingRepository implements AiUsageBindingRepository {
        @Override public List<cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO> listAll() { return List.of(); }
        @Override public cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO findById(Long id) { return null; }
        @Override public Long create(cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO binding) { return null; }
        @Override public boolean update(cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO binding) { return false; }
        @Override public boolean delete(Long id) { return false; }
    }

    private static final class StubProviderRepository implements AiProviderRepository {
        @Override public List<cn.cccs7.cynexus.ai.domain.entity.AiProviderBO> listAll() { return List.of(); }
        @Override public cn.cccs7.cynexus.ai.domain.entity.AiProviderBO findById(Long id) { return null; }
        @Override public Long create(cn.cccs7.cynexus.ai.domain.entity.AiProviderBO provider) { return null; }
        @Override public boolean update(cn.cccs7.cynexus.ai.domain.entity.AiProviderBO provider) { return false; }
        @Override public boolean delete(Long id) { return false; }
    }

}
