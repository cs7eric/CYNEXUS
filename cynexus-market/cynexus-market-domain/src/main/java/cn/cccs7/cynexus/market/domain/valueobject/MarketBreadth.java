package cn.cccs7.cynexus.market.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketBreadth {

    private Integer riseCount;
    private Integer fallCount;
    private Integer limitUpCount;
    private Integer limitDownCount;
}
