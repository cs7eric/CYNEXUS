package cn.cccs7.cynexus.ai.domain.entity;

import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelRuntimeOptions;

public record AiModelBO(
        String modelCode,
        String providerCode,
        String modelName,
        ModelCapability capability,
        ModelRuntimeOptions runtimeOptions,
        boolean enabled
) {
}
