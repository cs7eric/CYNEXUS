package cn.cccs7.cynexus.market.infra.datafeed;

import org.springframework.stereotype.Component;

@Component
public class MockMarketDataFeedClient implements MarketDataFeedClient {

    @Override
    public boolean available() {
        return true;
    }
}
