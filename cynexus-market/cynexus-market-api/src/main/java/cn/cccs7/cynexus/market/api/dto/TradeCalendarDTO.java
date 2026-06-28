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
public class TradeCalendarDTO {

    private Long id;
    private LocalDate tradeDate;
    private Boolean tradingDay;
    private String reason;
    private String source;
    private OffsetDateTime ingestedAt;
    private OffsetDateTime updatedAt;
}
