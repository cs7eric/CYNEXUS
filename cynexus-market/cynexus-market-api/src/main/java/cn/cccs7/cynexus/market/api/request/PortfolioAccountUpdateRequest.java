package cn.cccs7.cynexus.market.api.request;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PortfolioAccountUpdateRequest {
    private BigDecimal availableCash;
}
