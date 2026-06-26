package cn.cccs7.cynexus.market.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFeatureQuery {

    private String symbol;
    private String tradeDate;
}
