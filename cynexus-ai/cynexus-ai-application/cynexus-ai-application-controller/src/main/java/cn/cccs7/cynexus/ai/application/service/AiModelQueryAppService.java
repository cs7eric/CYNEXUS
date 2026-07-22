package cn.cccs7.cynexus.ai.application.service;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.application.assembler.AiModelAssembler;
import cn.cccs7.cynexus.ai.application.usecase.AiModelQueryUseCase;
import cn.cccs7.cynexus.ai.domain.service.model.AiModelQueryDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiModelQueryAppService implements AiModelQueryUseCase {

    private final AiModelQueryDomainService aiModelQueryDomainService;
    private final AiModelAssembler aiModelAssembler;

    public AiModelQueryAppService(AiModelQueryDomainService aiModelQueryDomainService,
                                  AiModelAssembler aiModelAssembler) {
        this.aiModelQueryDomainService = aiModelQueryDomainService;
        this.aiModelAssembler = aiModelAssembler;
    }

    @Override
    public List<AiModelDTO> listModels() {
        return aiModelAssembler.toTargetList(aiModelQueryDomainService.findEnabledModels());
    }
}
