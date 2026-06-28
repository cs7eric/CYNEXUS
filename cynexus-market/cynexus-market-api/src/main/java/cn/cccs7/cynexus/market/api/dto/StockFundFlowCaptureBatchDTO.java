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
public class StockFundFlowCaptureBatchDTO {

    private Long id;
    private LocalDate tradeDate;
    private String sourceType;
    private String status;
    private String source;
    private Integer rowCount;
    private String requestedCodes;
    private OffsetDateTime fetchedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
