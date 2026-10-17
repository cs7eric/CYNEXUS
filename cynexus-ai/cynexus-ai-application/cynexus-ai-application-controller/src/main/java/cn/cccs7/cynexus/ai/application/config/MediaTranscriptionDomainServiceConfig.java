package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.mediatranscription.impl.MediaTranscriptionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionCompletionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class MediaTranscriptionDomainServiceConfig {

    @Bean
    public MediaTranscriptionDomainService mediaTranscriptionDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            MediaTranscriptionCompletionPort modelExecutor,
            AiPromptRuntimeResolver promptResolver,
            AiExecutionLogRepository executionLogRepository,
            @Qualifier("jacksonStructuredOutputParser") MediaTranscriptionStructuredOutputPort structuredOutputPort) {
        return new MediaTranscriptionDomainServiceImpl(routingService, modelExecutor, promptResolver,
                executionLogRepository, structuredOutputPort);
    }
}
