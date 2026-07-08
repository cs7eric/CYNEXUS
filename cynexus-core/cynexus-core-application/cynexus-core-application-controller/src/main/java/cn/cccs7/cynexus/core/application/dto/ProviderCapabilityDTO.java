package cn.cccs7.cynexus.core.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCapabilityDTO {

    private OffsetDateTime generatedAt;
    private Summary summary;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer total;
        private Map<String, Long> byCategory;
        private Map<String, Long> byProvider;
        private Map<String, Long> byTargetStore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String capabilityCode;
        private String name;
        private String category;
        private String providerCode;
        private String providerName;
        private String capabilityType;
        private String sourceLayer;
        private String targetStore;
        private String status;
        private String dataGranularity;
        private List<String> storageTables;
        private List<String> updateJobs;
        private List<String> upstreamEndpoints;
        private List<String> usedBy;
        private String rateLimitPolicy;
        private String freshnessPolicy;
        private String antiScrapingNotes;
        private String description;
        private Integer sortOrder;
    }
}
