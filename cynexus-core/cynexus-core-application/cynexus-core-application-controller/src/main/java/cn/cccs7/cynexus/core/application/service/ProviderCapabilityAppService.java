package cn.cccs7.cynexus.core.application.service;

import cn.cccs7.cynexus.core.application.dto.ProviderCapabilityDTO;
import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import cn.cccs7.cynexus.core.domain.service.provider.ProviderCapabilityDomainService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProviderCapabilityAppService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ProviderCapabilityDomainService domainService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProviderCapabilityAppService(ProviderCapabilityDomainService domainService) {
        this.domainService = domainService;
    }

    public ProviderCapabilityDTO catalog() {
        List<ProviderCapabilityBO> capabilities = domainService.listEnabled();
        List<ProviderCapabilityDTO.Item> items = capabilities.stream()
                .map(this::toItem)
                .toList();
        return ProviderCapabilityDTO.builder()
                .generatedAt(OffsetDateTime.now())
                .summary(ProviderCapabilityDTO.Summary.builder()
                        .total(items.size())
                        .byCategory(countBy(items, ProviderCapabilityDTO.Item::getCategory))
                        .byProvider(countBy(items, ProviderCapabilityDTO.Item::getProviderCode))
                        .byTargetStore(countBy(items, ProviderCapabilityDTO.Item::getTargetStore))
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
                .storageTables(parseList(value.getStorageTables()))
                .updateJobs(parseList(value.getUpdateJobs()))
                .upstreamEndpoints(parseList(value.getUpstreamEndpoints()))
                .usedBy(parseList(value.getUsedBy()))
                .rateLimitPolicy(value.getRateLimitPolicy())
                .freshnessPolicy(value.getFreshnessPolicy())
                .antiScrapingNotes(value.getAntiScrapingNotes())
                .description(value.getDescription())
                .sortOrder(value.getSortOrder())
                .build();
    }

    private List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Long> countBy(List<ProviderCapabilityDTO.Item> items,
                                      java.util.function.Function<ProviderCapabilityDTO.Item, String> classifier) {
        return items.stream()
                .collect(Collectors.groupingBy(item -> {
                    String value = classifier.apply(item);
                    return value == null || value.isBlank() ? "unknown" : value;
                }, Collectors.counting()));
    }
}
