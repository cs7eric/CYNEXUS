package cn.cccs7.cynexus.ai.application.usecase;

import cn.cccs7.cynexus.ai.api.dto.AiModelDTO;
import java.util.List;

public interface AiModelQueryUseCase {

    List<AiModelDTO> listModels();
}
