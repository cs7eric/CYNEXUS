package cn.cccs7.cynexus.market.domain.entity;

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
public class TdxIndustryQuoteBatchBO {

    private Long id;
    private String batchKey;
    private LocalDate snapshotDate;
    private String source;
    private Integer rowCount;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
