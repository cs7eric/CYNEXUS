package cn.cccs7.cynexus.ai.infra.ai.cache;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PromptCacheUsage {

    Integer cachedInputTokens;
    Integer cacheWriteTokens;
    Integer totalCachedTokens;

    public boolean hasData() {
        return cachedInputTokens != null || cacheWriteTokens != null || totalCachedTokens != null;
    }
}
