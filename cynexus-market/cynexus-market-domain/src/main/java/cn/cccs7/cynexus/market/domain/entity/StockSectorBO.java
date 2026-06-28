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
public class StockSectorBO {

    private Long id;
    private String code;
    private String sectorId;
    private BigDecimal relationLevel;
    private String reason;
    private String source;
    private OffsetDateTime updatedAt;
}
