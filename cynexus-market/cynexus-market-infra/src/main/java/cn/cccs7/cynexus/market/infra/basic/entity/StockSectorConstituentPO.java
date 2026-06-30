package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class StockSectorConstituentPO {

    private String sectorId;
    private Integer categoryRaw;
    private String categoryLabel;
    private String sectorName;
    private String topicId;
    private String sectorSource;
    private String code;
    private String stockName;
    private String market;
    private BigDecimal totalMarketCap;
    private BigDecimal floatMarketCap;
    private BigDecimal relationLevel;
    private LocalDate tradeDate;
    private BigDecimal lastPrice;
    private BigDecimal preClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal change;
    private BigDecimal changePct;
    private BigDecimal volumeLots;
    private BigDecimal amountWan;
    private BigDecimal turnoverRate;
    private BigDecimal amplitude;
    private BigDecimal fundFlowTotalMarketCapYi;
    private BigDecimal fundFlowFloatMarketCapYi;
    private BigDecimal totalShares;
    private BigDecimal circulatingShares;
    private OffsetDateTime capturedAt;
    private String source;
}
