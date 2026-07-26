package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;

public final class NoopPromptCacheStrategy implements PromptCacheStrategy {

    @Override
    public boolean supports(AiModelBO model) {
        return true;
    }
}
