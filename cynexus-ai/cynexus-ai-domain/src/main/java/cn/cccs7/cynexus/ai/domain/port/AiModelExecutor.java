package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.entity.AiCompletionResultBO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.entity.AiImageInputBO;
import java.util.List;

/** Framework-neutral model execution boundary for domain capabilities. */
public interface AiModelExecutor {
    AiCompletionResultBO complete(AiModelBO model, String prompt, String capability);

    default AiCompletionResultBO complete(AiModelBO model, String prompt, AiExecutionContext context) {
        return complete(model, prompt, context == null ? null : context.capability());
    }

    default AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                String capability) {
        throw new UnsupportedOperationException("Vision execution is not supported by this executor");
    }

    default AiCompletionResultBO completeVision(AiModelBO model, String prompt, List<AiImageInputBO> images,
                                                AiExecutionContext context) {
        return completeVision(model, prompt, images, context == null ? null : context.capability());
    }
}
