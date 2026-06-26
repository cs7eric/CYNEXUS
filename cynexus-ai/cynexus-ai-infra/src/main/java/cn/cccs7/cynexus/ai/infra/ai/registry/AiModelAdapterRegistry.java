package cn.cccs7.cynexus.ai.infra.ai.registry;

import cn.cccs7.cynexus.ai.infra.ai.adapter.AiModelAdapter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiModelAdapterRegistry {

    private final Map<String, AiModelAdapter> adapters;

    public AiModelAdapterRegistry(List<AiModelAdapter> adapters) {
        this.adapters = adapters.stream().collect(Collectors.toMap(AiModelAdapter::providerCode, Function.identity()));
    }

    public AiModelAdapter get(String providerCode) {
        return adapters.get(providerCode);
    }
}
