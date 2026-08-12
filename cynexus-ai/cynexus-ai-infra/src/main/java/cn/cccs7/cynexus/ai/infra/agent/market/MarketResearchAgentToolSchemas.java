package cn.cccs7.cynexus.ai.infra.agent.market;

final class MarketResearchAgentToolSchemas {

    static final String STOCK_RESEARCH = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["search","metadata","sectors","sector_stocks","fund_flow","intraday","auction","auction_history"],"description":"Stock research operation to run."},
              "keyword":{"type":"string","minLength":1,"maxLength":80,"description":"Search text for stock or index lookup."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Stock, index, or instrument identifier."},
              "targetType":{"type":"string","enum":["stock","index","sector","etf"],"description":"Instrument type for intraday data; defaults to stock."},
              "name":{"type":"string","maxLength":120,"description":"Optional instrument display name for intraday lookup."},
              "adjust":{"type":"string","enum":["none","qfq","hfq"],"description":"Price adjustment mode for intraday data; defaults to qfq."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "periods":{"type":"string","maxLength":64,"description":"Comma-separated intraday periods such as 1m,5m,15m."},
              "sectorId":{"type":"string","maxLength":80,"description":"Optional sector identifier for sector_stocks."},
              "categoryRaw":{"type":"integer","description":"Optional raw sector category for sector_stocks."},
              "topicId":{"type":"string","maxLength":80,"description":"Optional topic identifier for sector_stocks."},
              "sectorName":{"type":"string","maxLength":120,"description":"Optional sector name for sector_stocks."},
              "limit":{"type":"integer","minimum":1,"maximum":120,"description":"Maximum history rows for auction_history."}
            },"additionalProperties":false}
            """;

    static final String STOCK_F10 = """
            {"type":"object","required":["operation","symbol"],"properties":{
              "operation":{"type":"string","enum":["valuation","finance_report","business_composition","profit_forecast","announcements","news","roadshows","company_news"],"description":"F10 research section to read."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Required stock identifier."},
              "requestId":{"type":"string","maxLength":40,"description":"Optional provider request id for valuation; defaults to the standard valuation query."},
              "reportType":{"type":"string","maxLength":40,"description":"Optional financial report type such as zcfzb."},
              "reportDate":{"type":"string","format":"date","description":"Optional report date for business composition."},
              "section":{"type":"string","maxLength":40,"description":"Optional company-news section; defaults to gsyj."}
            },"additionalProperties":false}
            """;

    static final String SENTIMENT = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["ma_count","risk_appetite","limit_emotion","volatility","turnover","style_risk","profit_effect","sector_breadth","msi_index","ma_count_history","risk_appetite_history","limit_emotion_history","volatility_history","turnover_history","style_risk_history","profit_effect_history","sector_breadth_history","msi_index_history","index_daily"],"description":"Market sentiment metric or bounded history operation to read."},
              "date":{"type":"string","format":"date","description":"Optional observation date in YYYY-MM-DD format; omit for the latest available session."}
              ,"start":{"type":"string","format":"date","description":"Range start date for history or index_daily."},
              "end":{"type":"string","format":"date","description":"Range end date for history or index_daily."},
              "days":{"type":"integer","minimum":1,"maximum":120,"description":"Number of sessions for sector_breadth_history."},
              "code":{"type":"string","minLength":1,"maxLength":30,"description":"Index code for index_daily."}
            },"additionalProperties":false}
            """;

    static final String INDUSTRY = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["targets","target_codes","overview","kline","result","fund_flow","fund_flow_history","tdx_fund_flow"],"description":"Industry analysis operation to run."},
              "sortBy":{"type":"string","maxLength":40,"description":"Optional overview sort field."},
              "ascending":{"type":"boolean","description":"Whether overview sorting is ascending; defaults to false."},
              "count":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum industry overview rows; defaults to 100."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "maxTradeDate":{"type":"string","format":"date","description":"Optional newest date for fund-flow history."},
              "days":{"type":"integer","minimum":1,"maximum":365,"description":"Number of history days; bounded to 365."},
              "top":{"type":"integer","minimum":1,"maximum":100,"description":"Optional number of top industries to return."},
              "targetType":{"type":"string","enum":["stock","index","sector","etf"],"description":"Instrument type for TDX fund flow."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Required target identifier for TDX fund flow."}
              ,"kind":{"type":"string","maxLength":40,"description":"Optional target-code category filter."},
              "period":{"type":"string","enum":["day","week","month"],"description":"Kline period; defaults to day."},
              "klineCount":{"type":"integer","minimum":1,"maximum":240,"description":"Maximum kline points for the kline operation."},
              "targetId":{"type":"string","minLength":1,"maxLength":80,"description":"Target result identifier for the result operation."}
            },"additionalProperties":false}
            """;

    static final String MARGIN = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["latest","summary_history","stocks","stock_history","industries","industry_history"],"description":"Margin-trading query operation to run."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "start":{"type":"string","format":"date","description":"Optional range start date for summary history."},
              "end":{"type":"string","format":"date","description":"Optional range end date for summary history."},
              "stockCode":{"type":"string","minLength":1,"maxLength":30,"description":"Required stock code for stock_history."},
              "industryCode":{"type":"string","minLength":1,"maxLength":40,"description":"Required industry code for industry_history or optional industry filter for stocks."},
              "exchange":{"type":"string","maxLength":20,"description":"Optional exchange filter for stocks."},
              "sort":{"type":"string","maxLength":40,"description":"Sort field; defaults to margin_balance."},
              "sortDir":{"type":"string","enum":["asc","desc"],"description":"Sort direction; defaults to desc."},
              "page":{"type":"integer","minimum":1,"maximum":100,"description":"Optional result page."},
              "pageSize":{"type":"integer","minimum":1,"maximum":100,"description":"Optional page size."},
              "top":{"type":"integer","minimum":1,"maximum":50,"description":"Maximum latest ranking rows; defaults to 20."},
              "limit":{"type":"integer","minimum":1,"maximum":120,"description":"Maximum stock rows or history rows."}
            },"additionalProperties":false}
            """;

    static final String INDICATOR = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["latest","industry_detail","transmission","replay_timeline","radar_state","radar_events","limit_ladder","replay_summary","replay_episode","trade_review_state"],"description":"Market-indicator query operation to run."},
              "tradeDate":{"type":"string","format":"date","description":"Optional trading date in YYYY-MM-DD format."},
              "status":{"type":"string","enum":["all","limit_up","limit_down","rapid_rise","rapid_fall"],"description":"Optional status filter within the recent historical limit-up/down pool."},
              "eventType":{"type":"string","maxLength":40,"description":"Optional event type filter for latest."},
              "limit":{"type":"integer","minimum":1,"maximum":300,"description":"Maximum rows to return from the historical limit-up/down pool."},
              "industryCode":{"type":"string","minLength":1,"maxLength":40,"description":"Required industry code for industry_detail."},
              "page":{"type":"integer","minimum":0,"maximum":100,"description":"Zero-based industry-detail page number."},
              "pageSize":{"type":"integer","minimum":10,"maximum":100,"description":"Industry-detail page size."},
              "asOf":{"type":"string","format":"date-time","description":"Optional evidence cutoff; events after this instant are excluded."},
              "board":{"type":"string","enum":["all","limit_up","first_board","sealed","limit_down","touch"],"description":"Limit ladder board filter."},
              "episodeId":{"type":"string","minLength":1,"maxLength":120,"description":"Episode identifier for replay_episode."},
              "from":{"type":"string","pattern":"^([01]\\\\d|2[0-3]):[0-5]\\\\d(:[0-5]\\\\d)?$","description":"Replay start time in Asia/Shanghai, defaults to 09:30."},
              "to":{"type":"string","pattern":"^([01]\\\\d|2[0-3]):[0-5]\\\\d(:[0-5]\\\\d)?$","description":"Replay end time in Asia/Shanghai, defaults to 15:00."},
              "mode":{"type":"string","enum":["intraday","daily"],"description":"Trade Review materialized-state mode; daily ignores asOf."},
              "membership":{"type":"string","enum":["ths","tdx","union"],"description":"Industry membership basis for Trade Review state; defaults to union."},
              "windowDays":{"type":"integer","minimum":5,"maximum":120,"description":"Bounded Trade Review trend window; defaults to 60."}
            },"additionalProperties":false}
            """;

    static final String ETF = """
            {"type":"object","properties":{
              "historyDays":{"type":"integer","minimum":3,"maximum":30,"description":"Number of ETF trend days to return."},
              "rowLimit":{"type":"integer","minimum":20,"maximum":300,"description":"Maximum ETF rows to return across broad and sector groups."}
            },"additionalProperties":false}
            """;

    static final String FUTURES_POSITION = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["latest","instruments","members","history","coverage"],"description":"Futures-position query operation to run."},
              "tradeDate":{"type":"string","format":"date","description":"Optional requested trading date in YYYY-MM-DD format."},
              "scopeType":{"type":"string","enum":["CONTRACT","VARIETY"],"description":"Position aggregation scope; defaults to CONTRACT."},
              "exchangeCode":{"type":"string","enum":["CFFEX","SHFE","CZCE","DCE","GFEX","INE"],"description":"Exchange code; required for members and history."},
              "varietyCode":{"type":"string","maxLength":30,"description":"Optional futures variety filter such as IF, IH, IM, or IC."},
              "instrumentCode":{"type":"string","minLength":1,"maxLength":40,"description":"Required instrument code for members or history."},
              "rankType":{"type":"string","enum":["VOLUME","LONG","SHORT"],"description":"Optional member ranking dimension."},
              "top":{"type":"integer","enum":[5,10,15,20],"description":"Bounded top-row count; defaults to 10."},
              "sort":{"type":"string","enum":["volume","longPosition","shortPosition","netPosition","ratio","instrumentCode"],"description":"Instrument sort field; defaults to netPosition."},
              "sortDir":{"type":"string","enum":["asc","desc"],"description":"Instrument sort direction; defaults to desc."},
              "page":{"type":"integer","minimum":1,"maximum":100,"description":"Optional one-based result page."},
              "pageSize":{"type":"integer","minimum":1,"maximum":100,"description":"Optional result page size."},
              "limit":{"type":"integer","minimum":1,"maximum":120,"description":"Maximum history rows for history."}
            },"additionalProperties":false}
            """;

    static final String PULSE = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["overview_akshare","overview_archive","overview_eltdx","overview_history","manual_fund_flow","sector_snapshot","sector_history","index_returns","index_returns_history","index_kline","style_sectors","style_constituents","industry_constituents","heatmap","limit_emotion"],"description":"Market pulse query operation to run."},
              "days":{"type":"integer","minimum":1,"maximum":120,"description":"Bounded number of sessions for history or index returns."},
              "name":{"type":"string","minLength":1,"maxLength":80,"description":"Style-sector name for style_constituents."},
              "code":{"type":"string","minLength":1,"maxLength":80,"description":"Industry code for industry_constituents."},
              "kind":{"type":"string","enum":["industries","concepts","styles"],"description":"Heatmap category; defaults to industries."},
              "topN":{"type":"integer","minimum":1,"maximum":100,"description":"Requested heatmap row bound."}
              ,"range":{"type":"string","enum":["7d","30d","60d","90d"],"description":"Overview history range."},
              "tradingDate":{"type":"string","format":"date","description":"Trading date for archived overview or manual fund-flow lookup."},
              "window":{"type":"integer","minimum":1,"maximum":30,"description":"Window size for index return history."},
              "start":{"type":"string","format":"date","description":"Range start date for index return history."},
              "end":{"type":"string","format":"date","description":"Range end date for index return history."},
              "codes":{"type":"string","maxLength":200,"description":"Optional comma-separated index codes for index_kline."},
              "interval":{"type":"string","enum":["1m","5m","15m","30m","60m"],"description":"Index kline interval."}
            },"additionalProperties":false}
            """;

    static final String EMPTY = "{\"type\":\"object\"}";

    static final String STOCK_INFORMATION = """
            {"type":"object","required":["operation","symbol"],"properties":{
              "operation":{"type":"string","enum":["feed","content","analysis"],"description":"Stock information operation to run."},
              "symbol":{"type":"string","minLength":1,"maxLength":30,"description":"Required stock identifier."},
              "limit":{"type":"integer","minimum":1,"maximum":100,"description":"Maximum feed items per category."},
              "kind":{"type":"string","minLength":1,"maxLength":40,"description":"Information item kind for content or analysis."},
              "source":{"type":"string","minLength":1,"maxLength":80,"description":"Information source for content or analysis."},
              "itemId":{"type":"string","minLength":1,"maxLength":120,"description":"Information item identifier for content or analysis."}
            },"additionalProperties":false}
            """;

    static final String SKILL_DATA = """
            {"type":"object","required":["operation"],"properties":{
              "operation":{"type":"string","enum":["datasets","rows"],"description":"A-share skill dataset operation to run."},
              "datasetCode":{"type":"string","minLength":1,"maxLength":80,"description":"Registered dataset code for rows."},
              "limit":{"type":"integer","minimum":1,"maximum":200,"description":"Maximum dataset rows to return."},
              "code":{"type":"string","maxLength":40,"description":"Optional stock code filter."},
              "source":{"type":"string","maxLength":80,"description":"Optional data-source filter."}
            },"additionalProperties":false}
            """;

    private MarketResearchAgentToolSchemas() {
    }
}
