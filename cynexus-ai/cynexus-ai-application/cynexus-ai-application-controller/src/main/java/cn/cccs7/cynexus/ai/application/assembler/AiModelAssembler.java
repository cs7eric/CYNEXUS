package cn.cccs7.cynexus.ai.application.assembler;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelCapability;
import cn.cccs7.cynexus.ai.domain.valueobject.ModelRuntimeOptions;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = ModelRuntimeOptions.class)
public interface AiModelAssembler extends BaseMapper<AiModelBO, AiModelDTO> {

    @Override
    @Mapping(target = "toolCallSupported", source = "capability.toolCallSupported")
    @Mapping(target = "structuredOutputSupported", source = "capability.structuredOutputSupported")
    AiModelDTO toTarget(AiModelBO source);

    @Override
    default AiModelBO toSource(AiModelDTO target) {
        if (target == null) {
            return null;
        }
        return AiModelBO.builder()
                .modelCode(target.getModelCode())
                .providerCode(target.getProviderCode())
                .modelName(target.getModelName())
                .capability(new ModelCapability(target.isToolCallSupported(), target.isStructuredOutputSupported(), false))
                .runtimeOptions(new ModelRuntimeOptions(null, null, null))
                .enabled(target.isEnabled())
                .build();
    }
}
