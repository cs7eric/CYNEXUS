package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.application.assembler.AiModelAssembler;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/models")
public class AiModelController {

    private final AiModelRepository aiModelRepository;
    private final AiModelAssembler aiModelAssembler;

    public AiModelController(AiModelRepository aiModelRepository,
                             AiModelAssembler aiModelAssembler) {
        this.aiModelRepository = aiModelRepository;
        this.aiModelAssembler = aiModelAssembler;
    }

    @GetMapping
    public Result<List<AiModelDTO>> listModels() {
        return Result.success(aiModelAssembler.toTargetList(aiModelRepository.findEnabledModels()));
    }
}
