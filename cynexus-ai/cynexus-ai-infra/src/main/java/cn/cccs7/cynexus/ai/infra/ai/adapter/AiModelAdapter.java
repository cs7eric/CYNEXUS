package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import java.util.List;

public interface AiModelAdapter {

    String providerCode();

    AiCompletionResultBO complete(AiModelBO model, String prompt);

    default AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images) {
        return complete(model, prompt);
    }
}
