package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.marketfactor.*;
import cn.cccs7.cynexus.ai.domain.marketfactor.impl.MarketFactorStudySynthesisDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.*;
import cn.cccs7.cynexus.ai.domain.port.AiPromptRuntimeResolver;
import cn.cccs7.cynexus.ai.domain.port.AiStructuredOutputParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class MarketFactorStudySynthesisDomainServiceConfig {
    @Bean MarketFactorStudySynthesisDomainService marketFactorStudySynthesisDomainService(ApplicationAnalysisRoutingDomainService routing, AiExecutionLogRepository logs, MarketFactorStudyEvidencePort evidence, MarketEventFactorAnalysisCompletionPort completion, AiPromptRuntimeResolver promptRuntimeResolver, AiStructuredOutputParser structuredOutputParser) {
        return new MarketFactorStudySynthesisDomainServiceImpl(routing, logs, evidence, completion, promptRuntimeResolver, structuredOutputParser);
    }
}
