package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.domain.marketinformation.MarketInformationAiDomainService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Scheduler adapter for the Market Information domain task worker. */
@Service
public class MarketInformationAiWorker {
    private final MarketInformationAiDomainService domainService;

    public MarketInformationAiWorker(MarketInformationAiDomainService domainService) {
        this.domainService = domainService;
    }

    @Scheduled(initialDelayString = "${cynexus.ai.market-information.recovery-initial-delay-ms:10000}",
            fixedDelayString = "${cynexus.ai.market-information.recovery-delay-ms:60000}")
    public void recoverStaleTasks() {
        domainService.recoverStaleTasks();
    }

    @Scheduled(fixedDelayString = "${cynexus.ai.market-information.worker-delay-ms:5000}")
    public void processNext() {
        domainService.processNext();
    }
}
