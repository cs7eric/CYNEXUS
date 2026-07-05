package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiPromptBO;
import cn.cccs7.cynexus.ai.domain.repository.AiPromptRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AiPromptBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AiPromptPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiPromptRepositoryImpl implements AiPromptRepository {

    private final AiPromptBasicService aiPromptBasicService;

    public AiPromptRepositoryImpl(AiPromptBasicService aiPromptBasicService) {
        this.aiPromptBasicService = aiPromptBasicService;
    }

    @Override
    public List<AiPromptBO> list(String keyword, String scenarioCode, String status) {
        return AiPromptPOConverter.INSTANCE.toTargetList(aiPromptBasicService.list(keyword, scenarioCode, status));
    }

    @Override
    public AiPromptBO findById(Long id) {
        return Optional.ofNullable(aiPromptBasicService.getById(id))
                .map(AiPromptPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(AiPromptBO prompt) {
        return aiPromptBasicService.create(AiPromptPOConverter.INSTANCE.toSource(prompt));
    }

    @Override
    public boolean update(AiPromptBO prompt) {
        return aiPromptBasicService.update(AiPromptPOConverter.INSTANCE.toSource(prompt));
    }

    @Override
    public boolean delete(Long id) {
        return aiPromptBasicService.delete(id);
    }
}
