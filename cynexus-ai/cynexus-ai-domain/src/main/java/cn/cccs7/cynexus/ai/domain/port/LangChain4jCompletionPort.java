package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.util.List;

/** Optional typed execution boundary implemented by infrastructure. */
public interface LangChain4jCompletionPort {
    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);
    AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images, String capability);

    default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }

    default AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                AiExecutionContext context) {
        return completeVision(model, prompt, images, context == null ? null : context.capability());
    }
}
