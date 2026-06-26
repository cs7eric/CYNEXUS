package cn.cccs7.cynexus.ai.infra.ai.factory;

import cn.cccs7.cynexus.ai.infra.ai.adapter.AiModelAdapter;
import cn.cccs7.cynexus.ai.infra.ai.registry.AiModelAdapterRegistry;
import org.springframework.stereotype.Component;

@Component
public class DynamicChatClientFactory {

    private final AiModelAdapterRegistry adapterRegistry;

    public DynamicChatClientFactory(AiModelAdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }

    public AiModelAdapter resolve(String providerCode) {
        return adapterRegistry.get(providerCode);
    }
}
