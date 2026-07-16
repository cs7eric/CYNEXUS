package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public interface AiCompletionGateway {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
}
