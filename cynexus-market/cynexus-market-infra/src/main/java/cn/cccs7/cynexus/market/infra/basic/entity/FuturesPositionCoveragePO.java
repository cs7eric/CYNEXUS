package cn.cccs7.cynexus.market.infra.basic.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class FuturesPositionCoveragePO {
    private LocalDate tradeDate;
    private String exchangeCode;
    private String provider;
    private String status;
    private Boolean selected;
    private Integer expectedInstrumentCount;
    private Integer actualInstrumentCount;
    private Integer rowCount;
    private OffsetDateTime completedAt;
}
