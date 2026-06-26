package cn.cccs7.cynexus.ai.infra.convert;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelRuntimeOptions;
import cn.cccs7.cynexus.ai.infra.basic.entity.AiModelConfigPO;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(imports = {
        ModelCapability.class,
        ModelRuntimeOptions.class
})
public interface AiModelPOConverter extends BaseMapper<AiModelConfigPO, AiModelBO> {

    AiModelPOConverter INSTANCE = Mappers.getMapper(AiModelPOConverter.class);

    @Override
    @Mapping(target = "capability", expression = "java(new ModelCapability(true, true, false))")
    @Mapping(target = "runtimeOptions", expression = "java(new ModelRuntimeOptions(null, null, null))")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(source.getEnabled()))")
    AiModelBO toTarget(AiModelConfigPO source);

    @Override
    default AiModelConfigPO toSource(AiModelBO target) {
        if (target == null) {
            return null;
        }
        AiModelConfigPO source = new AiModelConfigPO();
        source.setModelCode(target.modelCode());
        source.setProviderCode(target.providerCode());
        source.setModelName(target.modelName());
        source.setEnabled(target.enabled());
        return source;
    }
}
