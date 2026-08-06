package cn.cccs7.cynexus.market.infra.basic.entity;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MarketIndicatorBreadthPO {
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
