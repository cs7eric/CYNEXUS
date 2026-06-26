package cn.cccs7.cynexus.market.domain.entity;

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
public class SectorFundFlowCaptureBatchBO {

    private Long id;
    private String scope;
    private LocalDate tradeDate;
    private String sourceType;
    private String status;
    private String source;
    private Integer totalPages;
    private Integer rowCount;
    private String pageRowCounts;
    private OffsetDateTime fetchedAt;
    private String extra;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
