package cn.cccs7.cynexus.ai.application.controller;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import cn.cccs7.cynexus.ai.application.usecase.AiModelQueryUseCase;
import cn.cccs7.cynexus.common.core.result.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/models")
public class AiModelController {

    private final AiModelQueryUseCase aiModelQueryUseCase;

    public AiModelController(AiModelQueryUseCase aiModelQueryUseCase) {
        this.aiModelQueryUseCase = aiModelQueryUseCase;
    }

    @GetMapping
    public Result<List<AiModelDTO>> listModels() {
        return Result.success(aiModelQueryUseCase.listModels());
    }
}
