package cn.cccs7.cynexus.ai.infra.agent.market;

final class MarketResearchAgentToolGuidance {

    static final String STOCK_RESEARCH = guidance(
            "Read bounded stock lookup and trading research data.",
            "Use for stock search, metadata, sector relations, fund flow, intraday activity, or auction evidence.",
            "Do not use for broad market ranking or F10 narrative reports; choose the dedicated market tool.");
    static final String STOCK_F10 = guidance(
            "Read one stock's selected F10 fundamental or disclosure section.",
            "Use when the user asks for valuation, financial reports, business composition, forecasts, announcements, news, or roadshows.",
            "Do not infer investment advice from a single provider section; preserve the returned report section and date.");
    static final String STOCK_INFORMATION = guidance(
            "Read a stock information feed or one selected item content/analysis result.",
            "Use for announcements, news, research, investor interactions, roadshows, or an already indexed item.",
            "Do not request missing content from the Agent; content fetch and analysis execution are write-like operations.");
    static final String SENTIMENT = guidance(
            "Read one market sentiment metric for an available trading session.",
            "Use to support breadth, risk appetite, limit emotion, volatility, turnover, style, profit-effect, or MSI analysis.",
            "Do not present a sentiment metric as a causal explanation or a trading recommendation.");
    static final String INDUSTRY = guidance(
            "Read bounded industry application and fund-flow analysis.",
            "Use for industry ranking, fund-flow comparison, history, or target-level TDX flow context.",
            "Do not use the refresh or target-configuration endpoints through the Agent.");
    static final String MARGIN = guidance(
            "Read bounded margin-trading summaries, rankings, and stock history.",
            "Use when the user asks about margin balance, industry ranking, or a stock or industry's margin history.",
            "Do not treat margin balance as a standalone buy or sell signal.");
    static final String INDICATOR = guidance(
            "Read full-market indicator snapshots, bounded 1m replay timelines, materialized Trade Review states, sector transmission episodes, replay outcomes, conditional trade references, and industry detail.",
            "Use when the user asks who led a sector move, whether followers confirmed it, what market context accompanied the anomaly, or how an episode performed afterward.",
            "Treat coverage, freshness, data quality, counter-evidence, and invalidation conditions as mandatory. Conditional references are auxiliary research only; never present them as guaranteed buy/sell instructions or automatic trading.");
    static final String ETF = guidance(
            "Read the bounded ETF market snapshot, trend, breadth, rotation, and flow proxies.",
            "Use when the user asks about broad or sector ETF rotation, subscription proxies, premium, or ETF breadth.",
            "Treat subscription and flow fields as proxies and do not present them as verified fund creations or trading signals.");
    static final String FUTURES_POSITION = guidance(
            "Read bounded futures-position aggregates, members, history, and data coverage.",
            "Use when the user asks about IF, IH, IM, or IC position structure, member ranking, history, or source coverage.",
            "Do not use this read tool to produce a buy or sell recommendation; use the separate facts-only analysis capability when grounded analysis is requested.");
    static final String PULSE = guidance(
            "Read market pulse snapshots, heatmaps, style-sector catalogs, and constituents.",
            "Use for market structure, sector rotation, heatmap, or index-return context.",
            "Do not call write or refresh endpoints and do not claim that a heatmap proves causality.");
    static final String FRESHNESS = guidance(
            "Read the current freshness status of market data sources.",
            "Use before explaining data recency or when a market result may be stale.",
            "Do not treat source freshness as market performance data.");
    static final String SKILL_DATA = guidance(
            "Read the registered, bounded A-share skill datasets.",
            "Use to discover available datasets or inspect rows for a known dataset with optional stock/source filters.",
            "Do not invent dataset codes and do not request broad unfiltered rows when a stock or source filter is available.");

    private static String guidance(String purpose, String useWhen, String avoidWhen) {
        return "{\"purpose\":\"" + purpose + "\",\"useWhen\":[\"" + useWhen
                + "\"],\"avoidWhen\":[\"" + avoidWhen
                + "\"],\"parameterNotes\":{\"operation\":\"Choose the operation that matches the user's requested data.\"},\"examples\":[{\"arguments\":{},\"note\":\"Provide only the parameters required by the selected operation.\"}],\"resultNotes\":\"Preserve returned dates, source, and availability when summarizing the result.\"}";
    }

    private MarketResearchAgentToolGuidance() {
    }
}
