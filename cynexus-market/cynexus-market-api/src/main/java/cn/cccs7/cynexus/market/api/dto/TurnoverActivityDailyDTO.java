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
public class TurnoverActivityDailyDTO {

    private Long id;
    private LocalDate tradeDate;
    private BigDecimal totalAmount;
    private BigDecimal avg20dAmount;
    private BigDecimal ratio;
    private Integer elapsedMs;
    private String source;
    private OffsetDateTime ingestedAt;
    private BigDecimal score;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
