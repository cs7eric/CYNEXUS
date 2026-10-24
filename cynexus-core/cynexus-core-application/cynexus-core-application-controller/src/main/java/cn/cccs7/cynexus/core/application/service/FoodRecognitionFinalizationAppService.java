package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionDomainService;
import cn.cccs7.cynexus.core.domain.food.recognition.FoodRecognitionModels.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Commits the custom food and successful workflow state in the same database transaction. */
@Service
public class FoodRecognitionFinalizationAppService {
    private final FoodRecognitionDomainService domain;
    public FoodRecognitionFinalizationAppService(FoodRecognitionDomainService domain) { this.domain = domain; }
    @Transactional
    public void complete(Task task) { domain.complete(task); }
}
