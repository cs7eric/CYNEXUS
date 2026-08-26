package cn.cccs7.cynexus.ai.application.config;

import cn.cccs7.cynexus.ai.domain.applicationanalysis.ApplicationAnalysisRoutingDomainService;
import cn.cccs7.cynexus.ai.domain.marketfactor.*;
import cn.cccs7.cynexus.ai.domain.marketfactor.impl.MarketFactorStudySynthesisDomainServiceImpl;
import cn.cccs7.cynexus.ai.domain.repository.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class MarketFactorStudySynthesisDomainServiceConfig {
    @Bean MarketFactorStudySynthesisDomainService marketFactorStudySynthesisDomainService(ApplicationAnalysisRoutingDomainService routing, AiPromptRepository prompts, AiExecutionLogRepository logs, MarketFactorStudyEvidencePort evidence, MarketEventFactorAnalysisCompletionPort completion) {
        return new MarketFactorStudySynthesisDomainServiceImpl(routing, prompts, logs, evidence, completion);
    }
}
