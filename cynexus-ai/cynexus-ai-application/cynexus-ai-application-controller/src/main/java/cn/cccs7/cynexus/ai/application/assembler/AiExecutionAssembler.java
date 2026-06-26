package cn.cccs7.cynexus.ai.application.assembler;

import cn.cccs7.cynexus.ai.api.dto.AiExecuteDTO;
import cn.cccs7.cynexus.ai.api.request.AiExecuteRequest;
import cn.cccs7.cynexus.ai.application.command.ExecuteScenarioCommand;
import cn.cccs7.cynexus.ai.domain.entity.AiExecutionBO;
import cn.cccs7.cynexus.ai.domain.entity.AiScenarioBO;
import cn.cccs7.cynexus.ai.domain.execution.AiExecutionStatus;
import cn.cccs7.cynexus.common.core.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiExecutionAssembler extends BaseMapper<AiExecutionBO, AiExecuteDTO> {

    @Override
    @Mapping(target = "scenarioCode", source = "scenario.scenarioCode")
    @Mapping(target = "status", expression = "java(source.status().name())")
    AiExecuteDTO toTarget(AiExecutionBO source);

    @Override
    default AiExecutionBO toSource(AiExecuteDTO target) {
        if (target == null) {
            return null;
        }
        AiScenarioBO scenario = new AiScenarioBO(target.scenarioCode(), target.scenarioCode(), null, null,
                0, false, false, true);
        return new AiExecutionBO(target.executionNo(), scenario, target.modelCode(),
                AiExecutionStatus.valueOf(target.status()), target.content(), target.toolCalls());
    }

    default ExecuteScenarioCommand toCommand(AiExecuteRequest request) {
        return new ExecuteScenarioCommand(request.scenarioCode(), request.userInput(), request.variables(),
                request.preferredModelCode());
    }
}
