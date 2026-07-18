package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import java.util.List;

public interface AiCompletionGateway {

    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);

    AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability);
}
