package cn.cccs7.cynexus.market.domain.entity;

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
public class IndexReturnDailyBO {

    private Long id;
    private LocalDate tradeDate;
    private String indexCode;
    private String indexName;
    private Integer windowDays;
    private BigDecimal currentClose;
    private LocalDate currentTradeDate;
    private BigDecimal baseClose;
    private LocalDate baseDate;
    private BigDecimal returnPct;
    private String source;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
