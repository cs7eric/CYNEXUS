package cn.cccs7.cynexus.ai.infra.ai.spring;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/** Stable infrastructure registry boundary for dynamically routed Spring AI models. */
@Component
public class SpringAiModelRegistry {
    private final SpringAiModelFactory factory;

    public SpringAiModelRegistry(SpringAiModelFactory factory) { this.factory = factory; }

    public ChatModel get(AiModelBO model) { return factory.resolve(model); }

    public void invalidate(AiModelBO model) { factory.invalidate(model); }

    public void invalidateAll() { factory.invalidateAll(); }
}
