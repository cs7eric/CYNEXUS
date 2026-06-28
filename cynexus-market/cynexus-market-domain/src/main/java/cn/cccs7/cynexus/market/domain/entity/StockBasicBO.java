package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBasicBO {

    private Long id;
    private String code;
    private String name;
    private String market;
    private String board;
    private Boolean st;
    private BigDecimal limitUpThreshold;
    private Boolean active;
    private String source;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime updatedAt;
    private String industry;
    private BigDecimal totalMarketCap;
    private BigDecimal floatMarketCap;
    private String capStyle;
    private String sectorIndexSymbol;
    private String sectorIndexName;
    private String industryQuery;
}
