package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.factorresearch.MarketFactorStudyExecutionDomainService;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Bounded single-study dispatcher; heavy computation is deliberately durable and observable. */
@Service
public class MarketFactorStudyRuntimeService {
    private final MarketFactorStudyExecutionDomainService executionService;
    private final Executor studyExecutor;
    private final AtomicBoolean running = new AtomicBoolean();

    public MarketFactorStudyRuntimeService(MarketFactorStudyExecutionDomainService executionService,
                                           @Qualifier("marketFactorStudyExecutor") Executor marketFactorStudyExecutor) {
        this.executionService = executionService;
        this.studyExecutor = marketFactorStudyExecutor;
    }

    @Scheduled(initialDelayString = "${cynexus.market.factor-study.initial-delay-ms:5000}",
            fixedDelayString = "${cynexus.market.factor-study.dispatch-delay-ms:2000}")
    public void dispatch() {
        if (!running.compareAndSet(false, true)) return;
        try {
            studyExecutor.execute(() -> {
                try {
                    executionService.processNextQueuedStudy();
                } finally {
                    running.set(false);
                }
            });
        } catch (RejectedExecutionException ignored) {
            running.set(false);
        }
    }
}
