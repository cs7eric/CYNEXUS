package cn.cccs7.cynexus.ai.infra.ai.adapter;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface AiModelAdapter {

    String providerCode();

    AiCompletionResultBO complete(AiModelBO model, String prompt);
}
