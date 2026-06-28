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
public class HeatmapSnapshotBO {

    private Long id;
    private LocalDate snapshotDate;
    private String kind;
    private String source;
    private String payload;
    private Integer itemCount;
    private OffsetDateTime generatedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
