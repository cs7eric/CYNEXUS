package cn.cccs7.cynexus.ai.domain.valueobject.agent;

import lombok.Builder;
import lombok.Value;

/** Provider-neutral cache routing context. Provider-specific transport stays in infra. */
@Value
@Builder
public class PromptCacheContext {

    String conversationKey;
    String stablePrefixKey;
    String routingKey;
    String stablePrefixVersion;
}
