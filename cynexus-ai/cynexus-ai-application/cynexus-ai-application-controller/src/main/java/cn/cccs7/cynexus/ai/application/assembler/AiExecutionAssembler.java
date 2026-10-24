package cn.cccs7.cynexus.ai.application.assembler;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.application.command.ExecuteScenarioCommand;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiExecutionAssembler {

    @Mapping(target = "scenarioCode", source = "scenario.scenarioCode")
    @Mapping(target = "providerCode", ignore = true)
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    AiExecuteDTO toDTO(AiExecutionBO source);

    default ExecuteScenarioCommand toCommand(AiExecuteRequest request) {
        return new ExecuteScenarioCommand(request.getScenarioCode(), request.getUserInput(), request.getVariables(),
                request.getPreferredModelCode());
    }
}
