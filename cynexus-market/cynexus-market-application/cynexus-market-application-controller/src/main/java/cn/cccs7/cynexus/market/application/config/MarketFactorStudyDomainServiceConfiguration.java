package cn.cccs7.cynexus.market.application.config;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorResearchPolicy;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyDomainService;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyExecutionDomainService;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyIdGenerator;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyLifecyclePolicy;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyRepository;
import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudySnapshotRepository;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.LargeMoveCloseCohortProvider;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.LimitUpCloseCohortProvider;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.LimitUpTouchCohortProvider;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortDataRepository;
import cn.cccs7.cynexus.market.domain.factorresearch.cohort.MarketEventCohortProviderRegistry;
import cn.cccs7.cynexus.market.domain.factorresearch.control.MarketFactorControlMatchPolicy;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.ChangePointPatternDiscoverer;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.InterpretableSubgroupDiscoverer;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.PatternDiscoveryEngine;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.SequenceShapeClusterDiscoverer;
import cn.cccs7.cynexus.market.domain.factorresearch.discovery.SequentialRuleDiscoverer;
import cn.cccs7.cynexus.market.domain.repository.MarketPulseQueryRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketFactorStudyDomainServiceConfiguration {
    @Bean
    public MarketFactorStudyDomainService marketFactorStudyDomainService(MarketFactorStudyRepository repository,
                                                                          MarketFactorStudyIdGenerator idGenerator) {
        return new MarketFactorStudyDomainService(new MarketFactorResearchPolicy(), new MarketFactorStudyLifecyclePolicy(),
                repository, idGenerator);
    }

    @Bean
    public MarketEventCohortProviderRegistry marketEventCohortProviderRegistry(MarketPulseQueryRepository pulseRepository,
                                                                                 MarketEventCohortDataRepository eventRepository) {
        return new MarketEventCohortProviderRegistry(List.of(new LimitUpCloseCohortProvider(pulseRepository),
                new LimitUpTouchCohortProvider(eventRepository), new LargeMoveCloseCohortProvider(eventRepository)));
    }

    @Bean
    public MarketFactorStudyExecutionDomainService marketFactorStudyExecutionDomainService(MarketFactorStudyRepository repository,
                                                                                              MarketEventCohortProviderRegistry providers,
                                                                                              MarketFactorStudySnapshotRepository snapshots,
                                                                                              PatternDiscoveryEngine discoveryEngine) {
        return new MarketFactorStudyExecutionDomainService(repository, providers, new MarketFactorStudyLifecyclePolicy(), snapshots,
                new MarketFactorControlMatchPolicy(), discoveryEngine);
    }

    @Bean
    public PatternDiscoveryEngine marketFactorPatternDiscoveryEngine() {
        return new PatternDiscoveryEngine(List.of(new ChangePointPatternDiscoverer(), new SequenceShapeClusterDiscoverer(),
                new SequentialRuleDiscoverer(), new InterpretableSubgroupDiscoverer()));
    }
}
