package cn.cccs7.cynexus.market.domain.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndicatorBreadthBO {
    private Integer totalCount;
    private Integer advancingCount;
    private Integer decliningCount;
    private Integer flatCount;
    private Integer limitUpCount;
    private Integer limitUpTouchCount;
    private Integer limitDownCount;
    private Integer limitDownTouchCount;
    private Integer rapidRiseCount;
    private Integer rapidFallCount;
    private OffsetDateTime capturedAt;
}
