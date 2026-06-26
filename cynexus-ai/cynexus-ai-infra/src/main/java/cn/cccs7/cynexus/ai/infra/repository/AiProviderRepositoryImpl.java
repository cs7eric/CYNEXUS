package cn.cccs7.cynexus.ai.infra.repository;

import cn.cccs7.cynexus.ai.domain.entity.AiProviderBO;
import cn.cccs7.cynexus.ai.domain.repository.AiProviderRepository;
import cn.cccs7.cynexus.ai.infra.basic.service.AiProviderBasicService;
import cn.cccs7.cynexus.ai.infra.convert.AiProviderPOConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AiProviderRepositoryImpl implements AiProviderRepository {

    private final AiProviderBasicService aiProviderBasicService;

    public AiProviderRepositoryImpl(AiProviderBasicService aiProviderBasicService) {
        this.aiProviderBasicService = aiProviderBasicService;
    }

    @Override
    public List<AiProviderBO> listAll() {
        return AiProviderPOConverter.INSTANCE.toTargetList(aiProviderBasicService.listAll());
    }

    @Override
    public AiProviderBO findById(Long id) {
        return Optional.ofNullable(aiProviderBasicService.getById(id))
                .map(AiProviderPOConverter.INSTANCE::toTarget)
                .orElse(null);
    }

    @Override
    public Long create(AiProviderBO provider) {
        return aiProviderBasicService.create(AiProviderPOConverter.INSTANCE.toSource(provider));
    }

    @Override
    public boolean update(AiProviderBO provider) {
        return aiProviderBasicService.update(AiProviderPOConverter.INSTANCE.toSource(provider));
    }

    @Override
    public boolean delete(Long id) {
        return aiProviderBasicService.delete(id);
    }
}
