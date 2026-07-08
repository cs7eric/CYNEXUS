package cn.cccs7.cynexus.core.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCapabilityBO {

    private Long id;
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
    private String storageTables;
    private String updateJobs;
    private String upstreamEndpoints;
    private String usedBy;
    private String rateLimitPolicy;
    private String freshnessPolicy;
    private String antiScrapingNotes;
    private String description;
    private String params;
    private String extra;
    private Integer sortOrder;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
