package cn.cccs7.cynexus.core.domain.policy.provider;

import cn.cccs7.cynexus.core.domain.entity.ProviderCapabilityBO;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Domain helpers for provider capability catalog presentation facts. */
public final class ProviderCapabilityCatalogPolicy {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private ProviderCapabilityCatalogPolicy() {
    }

    public static List<String> parseStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = MAPPER.readValue(value, STRING_LIST);
            return parsed == null ? List.of() : List.copyOf(parsed);
        } catch (Exception exception) {
            return List.of();
        }
    }

    public static Map<String, Long> countBy(List<ProviderCapabilityBO> capabilities,
                                            Function<ProviderCapabilityBO, String> classifier) {
        if (capabilities == null || capabilities.isEmpty()) {
            return Map.of();
        }
        return capabilities.stream().collect(Collectors.groupingBy(item -> {
            String value = classifier.apply(item);
            return value == null || value.isBlank() ? "unknown" : value;
        }, LinkedHashMap::new, Collectors.counting()));
    }
}
