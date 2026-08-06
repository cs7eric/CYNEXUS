package cn.cccs7.cynexus.market.domain.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicatorIndustryDetailBO {
    private String industryCode;
    private String industryName;
    private String categoryLabel;
    private Integer totalCount;
    private Integer page;
    private Integer pageSize;
    private Integer advancingCount;
    private Integer decliningCount;
    private Integer flatCount;
    private BigDecimal averageChangePct;
    private OffsetDateTime capturedAt;
    private List<MarketIndicatorSnapshotBO> constituents;
    private List<MarketIndicatorIndustryRelationBO> relations;
}
