package cn.cccs7.cynexus.market.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThsIndustryConstituentCaptureBatchDTO {

    private Long id;
    private LocalDate snapshotDate;
    private String sourceType;
    private String status;
    private String source;
    private String requestedIndustryCodes;
    private Integer industryCount;
    private Integer rowCount;
    private String pageRowCounts;
    private OffsetDateTime fetchedAt;
    private String extra;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
