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
public class MarketPulseOverviewEltdxDTO {

    private Boolean ok;
    private String error;
    private LocalDate tradingDate;
    private OffsetDateTime fetchedAt;
    private BigDecimal totalAmount;
    private BigDecimal totalVolume;
    private Integer risingCount;
    private Integer fallingCount;
    private Integer flatCount;
    private Integer limitUpCount;
    private Integer limitDownCount;
    private Integer stockCount;
    private BigDecimal prevDayTotalAmount;
    private LocalDate prevDayTradingDate;
}
