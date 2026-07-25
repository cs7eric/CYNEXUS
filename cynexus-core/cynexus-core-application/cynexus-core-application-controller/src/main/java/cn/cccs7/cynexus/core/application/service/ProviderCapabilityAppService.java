package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.ProviderCapabilityDTO;
import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import cn.cccs7.cynexus.core.domain.policy.provider.ProviderCapabilityCatalogPolicy;
import cn.cccs7.cynexus.core.domain.service.provider.ProviderCapabilityDomainService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProviderCapabilityAppService {

    private final ProviderCapabilityDomainService domainService;

    public ProviderCapabilityAppService(ProviderCapabilityDomainService domainService) {
        this.domainService = domainService;
    }

    public ProviderCapabilityDTO catalog() {
        List<ProviderCapabilityBO> capabilities = domainService.listEnabled();
        List<ProviderCapabilityDTO.Item> items = capabilities.stream().map(this::toItem).toList();
        return ProviderCapabilityDTO.builder()
                .generatedAt(OffsetDateTime.now())
                .summary(ProviderCapabilityDTO.Summary.builder()
                        .total(items.size())
                        .byCategory(ProviderCapabilityCatalogPolicy.countBy(capabilities, ProviderCapabilityBO::getCategory))
                        .byProvider(ProviderCapabilityCatalogPolicy.countBy(capabilities, ProviderCapabilityBO::getProviderCode))
                        .byTargetStore(ProviderCapabilityCatalogPolicy.countBy(capabilities, ProviderCapabilityBO::getTargetStore))
                        .build())
                .items(items)
                .build();
    }

    private ProviderCapabilityDTO.Item toItem(ProviderCapabilityBO value) {
        return ProviderCapabilityDTO.Item.builder()
                .capabilityCode(value.getCapabilityCode())
                .name(value.getName())
                .category(value.getCategory())
                .providerCode(value.getProviderCode())
                .providerName(value.getProviderName())
                .capabilityType(value.getCapabilityType())
                .sourceLayer(value.getSourceLayer())
                .targetStore(value.getTargetStore())
                .status(value.getStatus())
                .dataGranularity(value.getDataGranularity())
                .storageTables(ProviderCapabilityCatalogPolicy.parseStringList(value.getStorageTables()))
                .updateJobs(ProviderCapabilityCatalogPolicy.parseStringList(value.getUpdateJobs()))
                .upstreamEndpoints(ProviderCapabilityCatalogPolicy.parseStringList(value.getUpstreamEndpoints()))
                .usedBy(ProviderCapabilityCatalogPolicy.parseStringList(value.getUsedBy()))
                .rateLimitPolicy(value.getRateLimitPolicy())
                .freshnessPolicy(value.getFreshnessPolicy())
                .antiScrapingNotes(value.getAntiScrapingNotes())
                .description(value.getDescription())
                .sortOrder(value.getSortOrder())
                .build();
    }
}
