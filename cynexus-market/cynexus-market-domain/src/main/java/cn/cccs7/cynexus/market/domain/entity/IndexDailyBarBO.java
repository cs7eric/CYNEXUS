package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDailyBarBO {

    private LocalDate tradeDate;
    private String code;
    private String name;
    private BigDecimal close;
    private BigDecimal amount;
}
