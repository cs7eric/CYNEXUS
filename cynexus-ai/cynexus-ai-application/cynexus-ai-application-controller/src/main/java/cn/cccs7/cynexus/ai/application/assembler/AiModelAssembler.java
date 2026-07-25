package cn.cccs7.cynexus.ai.application.assembler;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiModelAssembler {

    @Mapping(target = "toolCallSupported", source = "capability.toolCallSupported")
    @Mapping(target = "structuredOutputSupported", source = "capability.structuredOutputSupported")
    AiModelDTO toDTO(AiModelBO source);

    List<AiModelDTO> toDTOList(List<AiModelBO> source);
}
