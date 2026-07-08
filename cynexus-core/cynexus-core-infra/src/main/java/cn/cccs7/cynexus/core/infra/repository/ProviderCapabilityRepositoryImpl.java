package cn.cccs7.cynexus.core.infra.repository;

import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import cn.cccs7.cynexus.core.domain.repository.ProviderCapabilityRepository;
import cn.cccs7.cynexus.core.infra.basic.service.ProviderCapabilityBasicService;
import cn.cccs7.cynexus.core.infra.convert.ProviderCapabilityPOConverter;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ProviderCapabilityRepositoryImpl implements ProviderCapabilityRepository {

    private final ProviderCapabilityBasicService providerCapabilityBasicService;

    public ProviderCapabilityRepositoryImpl(ProviderCapabilityBasicService providerCapabilityBasicService) {
        this.providerCapabilityBasicService = providerCapabilityBasicService;
    }

    @Override
    public List<ProviderCapabilityBO> listEnabled() {
        return ProviderCapabilityPOConverter.INSTANCE.toTargetList(providerCapabilityBasicService.listEnabled());
    }
}
