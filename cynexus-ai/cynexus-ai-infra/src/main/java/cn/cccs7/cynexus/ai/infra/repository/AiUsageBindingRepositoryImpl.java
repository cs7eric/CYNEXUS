package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiUsageBindingBO;
import cn.cccs7.cynexus.ai.domain.repository.AiUsageBindingRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AiUsageBindingBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AiUsageBindingPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiUsageBindingRepositoryImpl implements AiUsageBindingRepository {

    private final AiUsageBindingBasicService aiUsageBindingBasicService;

    public AiUsageBindingRepositoryImpl(AiUsageBindingBasicService aiUsageBindingBasicService) {
        this.aiUsageBindingBasicService = aiUsageBindingBasicService;
    }

    @Override
    public List<AiUsageBindingBO> listAll() {
        return AiUsageBindingPOConverter.INSTANCE.toTargetList(aiUsageBindingBasicService.listAll());
    }

    @Override
    public AiUsageBindingBO findById(Long id) {
        return Optional.ofNullable(aiUsageBindingBasicService.getById(id))
                .map(AiUsageBindingPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(AiUsageBindingBO binding) {
        return aiUsageBindingBasicService.create(AiUsageBindingPOConverter.INSTANCE.toSource(binding));
    }

    @Override
    public boolean update(AiUsageBindingBO binding) {
        return aiUsageBindingBasicService.update(AiUsageBindingPOConverter.INSTANCE.toSource(binding));
    }

    @Override
    public boolean delete(Long id) {
        return aiUsageBindingBasicService.delete(id);
    }
}
