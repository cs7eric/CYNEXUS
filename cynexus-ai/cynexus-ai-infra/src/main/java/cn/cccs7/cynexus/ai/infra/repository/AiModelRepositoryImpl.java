package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiModelBO;
import cn.cccs7.cynexus.ai.domain.repository.AiModelRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AiModelConfigBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AiModelPOConverter;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AiModelRepositoryImpl implements AiModelRepository {

    private final AiModelConfigBasicService aiModelConfigBasicService;
    private final AiModelPOConverter aiModelPOConverter;

    public AiModelRepositoryImpl(AiModelConfigBasicService aiModelConfigBasicService,
                                 AiModelPOConverter aiModelPOConverter) {
        this.aiModelConfigBasicService = aiModelConfigBasicService;
        this.aiModelPOConverter = aiModelPOConverter;
    }

    @Override
    public List<AiModelBO> findEnabledModels() {
        return aiModelConfigBasicService.listEnabled().stream()
                .map(aiModelPOConverter::toTarget)
                .toList();
    }
}
