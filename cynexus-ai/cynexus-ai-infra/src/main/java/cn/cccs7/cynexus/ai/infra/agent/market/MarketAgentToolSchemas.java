package cn.cccs7.cynexus.ai.infra.agent.market;

final class MarketAgentToolSchemas {

    static final String TRADE_DATE = "{\"type\":\"object\",\"properties\":{\"tradeDate\":{\"type\":\"string\",\"description\":\"交易日，YYYY-MM-DD，可省略\"}}}";
    static final String HISTORY = "{\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"string\"},\"end\":{\"type\":\"string\"}}}";
    static final String SECTOR_RANK = "{\"type\":\"object\",\"properties\":{\"tradeDate\":{\"type\":\"string\"},\"topN\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}}}";
    static final String STOCK_DAILY = "{\"type\":\"object\",\"required\":[\"symbol\"],\"properties\":{\"symbol\":{\"type\":\"string\",\"description\":\"股票代码，如 600519\"},\"tradeDate\":{\"type\":\"string\"}}}";
    static final String STOCK_INTRADAY = STOCK_DAILY;
    static final String KLINES = "{\"type\":\"object\",\"required\":[\"symbol\"],\"properties\":{\"targetType\":{\"type\":\"string\",\"enum\":[\"stock\",\"index\",\"sector\",\"etf\"]},\"symbol\":{\"type\":\"string\"},\"period\":{\"type\":\"string\"},\"adjust\":{\"type\":\"string\",\"enum\":[\"none\",\"qfq\",\"hfq\"]}}}";

    private MarketAgentToolSchemas() {
    }
}
