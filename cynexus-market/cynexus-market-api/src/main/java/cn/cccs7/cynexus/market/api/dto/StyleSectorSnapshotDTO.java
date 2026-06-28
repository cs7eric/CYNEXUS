package cn.cccs7.cynexus.market.api.dto;

import java.math.BigDecimal;
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
public class StyleSectorSnapshotDTO {

    private Long id;
    private LocalDate tradeDate;
    private String name;
    private Integer rankByChange;
    private BigDecimal changePct;
    private Integer validSize;
    private Integer sampleSize;
    private String source;
    private String payload;
    private OffsetDateTime ingestedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
