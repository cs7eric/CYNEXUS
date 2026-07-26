package cn.cccs7.cynexus.ai.infra.ai.cache;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.util.List;

public final class PromptCacheStrategyRegistry {

    private final List<PromptCacheStrategy> strategies;
    private final PromptCacheStrategy noop = new NoopPromptCacheStrategy();

    private PromptCacheStrategyRegistry(List<PromptCacheStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public static PromptCacheStrategyRegistry defaultRegistry() {
        return new PromptCacheStrategyRegistry(List.of(
                new XaiPromptCacheStrategy(),
                new OpenAiPromptCacheStrategy(),
                new GenericPromptCacheStrategy()));
    }

    public PromptCacheStrategy resolve(AiModelBO model) {
        return strategies.stream().filter(strategy -> strategy.supports(model)).findFirst().orElse(noop);
    }
}
