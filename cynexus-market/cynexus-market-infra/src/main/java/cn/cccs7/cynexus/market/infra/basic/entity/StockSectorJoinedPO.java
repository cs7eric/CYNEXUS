package cn.cccs7.cynexus.market.infra.basic.entity;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class StockSectorJoinedPO {

    private String code;
    private String sectorId;
    private Integer categoryRaw;
    private String categoryLabel;
    private String name;
    private String topicId;
    private BigDecimal relationLevel;
    private BigDecimal changePercent;
    private String source;
}
