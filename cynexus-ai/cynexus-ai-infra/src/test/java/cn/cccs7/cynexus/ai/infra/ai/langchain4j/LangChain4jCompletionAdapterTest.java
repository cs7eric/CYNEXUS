package cn.cccs7.cynexus.ai.infra.ai.langchain4j;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cccs7.cynexus.ai.domain.entity.AiExecutionLogBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.port.AiExecutionContext;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.infra.observability.LangfuseGenerationReporter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LangChain4jCompletionAdapterTest {
    @Test
    void successfulTypedOutputPreservesContextAndUsageContract() {
        LangChain4jServiceFactory factory = mock(LangChain4jServiceFactory.class);
        LangfuseGenerationReporter reporter = mock(LangfuseGenerationReporter.class);
        GenericTextAnalyzer analyzer = mock(GenericTextAnalyzer.class);
        when(factory.genericTextAnalyzer(any())).thenReturn(analyzer);
        when(analyzer.complete("prompt")).thenReturn("answer");
        var adapter = new LangChain4jCompletionAdapter(factory, reporter);
        AiModelBO model = AiModelBO.builder().providerCode("xhhai-grok").modelCode("grok-4.6").build();
        AiExecutionContext context = new AiExecutionContext("exec-ok", "workspace_chat", "workspace_chat", "v2",
                "xhhai-grok", "grok-4.6", "tenant", "principal", "request", "trace");

        var result = adapter.complete(model, "prompt", context);

        assertEquals("answer", result.getContent());
        assertEquals("exec-ok", result.getExecutionNo());
        assertEquals("xhhai-grok", result.getProviderCode());
        assertEquals("grok-4.6", result.getModelCode());
        verify(reporter).report(eq(model), eq("workspace_chat"), eq("prompt"), any(), any(), isNull(), eq(context));
    }

    @Test
    void typedServiceFailurePersistsCorrelatedFallbackEligibleAttempt() {
        LangChain4jServiceFactory factory = mock(LangChain4jServiceFactory.class);
        LangfuseGenerationReporter reporter = mock(LangfuseGenerationReporter.class);
        AiExecutionLogRepository logs = mock(AiExecutionLogRepository.class);
        MediaTranscriptAnalyzer analyzer = mock(MediaTranscriptAnalyzer.class);
        when(factory.mediaTranscriptAnalyzer(any())).thenReturn(analyzer);
        when(analyzer.analyze("prompt")).thenThrow(new IllegalStateException("typed unavailable"));

        var adapter = new LangChain4jCompletionAdapter(factory, reporter, logs);
        AiModelBO model = AiModelBO.builder().providerCode("xhhai-grok").modelCode("grok-4.6").build();
        AiExecutionContext context = new AiExecutionContext("exec-typed", "media_transcription_enrich", "media_transcription_enrich", "v1",
                "xhhai-grok", "grok-4.6", "tenant", "principal", "request", "trace");
        assertThrows(IllegalStateException.class, () -> adapter.complete(model, "prompt", context));

        ArgumentCaptor<AiExecutionLogBO> captor = ArgumentCaptor.forClass(AiExecutionLogBO.class);
        verify(logs).create(captor.capture());
        AiExecutionLogBO failure = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("exec-typed-lc4j", failure.getExecutionNo());
        org.junit.jupiter.api.Assertions.assertEquals("media_transcription_enrich", failure.getCapability());
        org.junit.jupiter.api.Assertions.assertEquals("media_transcription_enrich", failure.getPromptKey());
        org.junit.jupiter.api.Assertions.assertEquals("v1", failure.getPromptVersion());
        org.junit.jupiter.api.Assertions.assertTrue(failure.getExtra().contains("fallbackEligible"));
        org.junit.jupiter.api.Assertions.assertTrue(failure.getErrorMessage().contains("typed unavailable"));
    }
}
