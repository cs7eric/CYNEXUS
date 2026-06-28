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
public class OverviewDetailSnapshotBO {

    private Long id;
    private LocalDate tradeDate;
    private String benchmarkCode;
    private String sourceTable;
    private String payload;
    private String source;
    private OffsetDateTime generatedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
