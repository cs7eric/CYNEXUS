package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionCompletionPort;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionDomainService;
import cn.cccs7.cynexus.ai.domain.mediatranscription.MediaTranscriptionStructuredOutputPort;
import cn.cccs7.cynexus.ai.domain.mediatranscription.impl.MediaTranscriptionDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.AiExecutionLogRepository;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaTranscriptionDomainServiceConfig {

    @Bean
    public MediaTranscriptionDomainService mediaTranscriptionDomainService(
            ApplicationAnalysisRoutingDomainService routingService,
            MediaTranscriptionCompletionPort completionPort,
            AiPromptRepository promptRepository,
            AiExecutionLogRepository executionLogRepository,
            MediaTranscriptionStructuredOutputPort structuredOutputPort) {
        return new MediaTranscriptionDomainServiceImpl(routingService, completionPort, promptRepository,
                executionLogRepository, structuredOutputPort);
    }
}
