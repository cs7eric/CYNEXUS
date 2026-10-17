package cn.cccs7.cynexus.ai.domain.port;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

/** A typed capability facade; routing and prompt governance remain outside the facade. */
public interface TypedAiService<I, O> {
    O invoke(AiModelBO model, I input, AiExecutionContext context);
}
