package cn.cccs7.cynexus.market.application.service;

import cn.cccs7.cynexus.market.domain.entity.StockDailyFeatureBO;
import cn.cccs7.cynexus.market.domain.entity.StockIntradayFeatureBO;
import cn.cccs7.cynexus.market.domain.valueobject.Stock;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class StockFeatureAppService {

    public StockDailyFeatureBO getDailyFeature(String symbol, String tradeDate) {
        return new StockDailyFeatureBO(new Stock(symbol, symbol), tradeDate, new BigDecimal("18.88"), new BigDecimal("3.24"),
                new BigDecimal("6.18"), new BigDecimal("1.82"), new BigDecimal("3200.00"), "strong");
    }

    public StockIntradayFeatureBO getIntradayFeature(String symbol, String tradeDate) {
        return new StockIntradayFeatureBO(new Stock(symbol, symbol), tradeDate, new BigDecimal("19.12"), new BigDecimal("4.26"),
                new BigDecimal("1.46"), new BigDecimal("860.00"), "accelerating");
    }
}
