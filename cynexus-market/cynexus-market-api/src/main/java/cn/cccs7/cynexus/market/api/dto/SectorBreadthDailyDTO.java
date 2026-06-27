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
public class SectorBreadthDailyDTO {

    private Long id;
    private LocalDate tradeDate;
    private Integer advancing;
    private Integer declining;
    private Integer flat;
    private Integer total;
    private BigDecimal advancePct;
    private String source;
    private Integer elapsedMs;
    private OffsetDateTime ingestedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
