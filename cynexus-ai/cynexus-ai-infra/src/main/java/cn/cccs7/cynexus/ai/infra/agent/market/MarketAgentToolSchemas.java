package cn.cccs7.cynexus.ai.infra.agent.market;

final class MarketAgentToolSchemas {

    static final String TRADE_DATE = "{\"type\":\"object\",\"properties\":{\"tradeDate\":{\"type\":\"string\",\"description\":\"交易日，YYYY-MM-DD，可省略；默认取上海时区最近有数据交易日\"}},\"additionalProperties\":false}";
    static final String HISTORY = "{\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"string\",\"description\":\"起始日期 YYYY-MM-DD，可省略\"},\"end\":{\"type\":\"string\",\"description\":\"结束日期 YYYY-MM-DD，可省略\"}},\"additionalProperties\":false}";
    static final String SECTOR_RANK = "{\"type\":\"object\",\"properties\":{\"tradeDate\":{\"type\":\"string\",\"description\":\"交易日，YYYY-MM-DD，可省略\"},\"topN\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}},\"additionalProperties\":false}";
    static final String STOCK_DAILY = "{\"type\":\"object\",\"required\":[\"symbol\"],\"properties\":{\"symbol\":{\"type\":\"string\",\"description\":\"股票代码，如 600519\"},\"tradeDate\":{\"type\":\"string\",\"description\":\"交易日，YYYY-MM-DD，可省略\"}},\"additionalProperties\":false}";
    static final String STOCK_INTRADAY = STOCK_DAILY;
    static final String KLINES = "{\"type\":\"object\",\"required\":[\"symbol\"],\"properties\":{\"targetType\":{\"type\":\"string\",\"enum\":[\"stock\",\"index\",\"sector\",\"etf\"]},\"symbol\":{\"type\":\"string\"},\"period\":{\"type\":\"string\"},\"adjust\":{\"type\":\"string\",\"enum\":[\"none\",\"qfq\",\"hfq\"]}},\"additionalProperties\":false}";

    private MarketAgentToolSchemas() {
    }
}
