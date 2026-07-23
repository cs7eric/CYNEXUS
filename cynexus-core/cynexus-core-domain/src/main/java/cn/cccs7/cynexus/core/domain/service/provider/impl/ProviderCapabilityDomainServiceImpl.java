package cn.cccs7.cynexus.core.domain.service.provider.impl;

import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import cn.cccs7.cynexus.core.domain.repository.ProviderCapabilityRepository;
import cn.cccs7.cynexus.core.domain.service.provider.ProviderCapabilityDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProviderCapabilityDomainServiceImpl implements ProviderCapabilityDomainService {

    private final ProviderCapabilityRepository repository;

    public ProviderCapabilityDomainServiceImpl(ProviderCapabilityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProviderCapabilityBO> listEnabled() {
        return repository.listEnabled();
    }
}
