package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FoodRecognitionWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoodRecognitionWorker.class);
    private final FoodRecognitionDomainService domain;
    private final FoodRecognitionFinalizationAppService finalization;

    public FoodRecognitionWorker(FoodRecognitionDomainService domain, FoodRecognitionFinalizationAppService finalization) {
        this.domain = domain;
        this.finalization = finalization;
    }

    @Scheduled(initialDelayString = "${cynexus.food.recognition.initial-delay-ms:3000}",
            fixedDelayString = "${cynexus.food.recognition.worker-delay-ms:1000}")
    public void processNext() {
        var task = domain.claimNext();
        if (task == null) return;
        try {
            if (domain.advance(task)) finalization.complete(task);
        } catch (RuntimeException exception) {
            LOGGER.warn("Food recognition processing failed for task {}", task.id(), exception);
            domain.fail(task, exception);
        }
    }

    @Scheduled(initialDelayString = "${cynexus.food.recognition.cleanup-initial-delay-ms:60000}",
            fixedDelayString = "${cynexus.food.recognition.cleanup-delay-ms:3600000}")
    public void purgeExpiredInputs() { domain.purgeExpiredInputs(); }
}
