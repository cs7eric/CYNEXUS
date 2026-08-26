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
    static final String SCREENER = guidance(
            "Run a bounded end-of-day A-share stock screen using THS-to-TDX industry groups, THS industry leaves, TDX concept/style memberships, technical, liquidity, size, valuation, and main-fund-flow fields.",
            "Use when the user asks to identify stocks meeting explicit daily conditions, including a TDX industry group, THS industry leaf, TDX concept, TDX style, board, momentum, volatility, turnover rate, volume ratio, PE, or main-fund-flow filters.",
            "Do not use for intraday or full financial-factor screening; PE has partial daily-snapshot coverage, sector memberships are descriptive classifications, and the returned list is factual research input, not a trading recommendation.");
    static final String FACTOR_OBSERVATORY = guidance(
            "Read a bounded daily limit-up or large-move cohort through independently traceable market, industry, intraday, technical, and margin evidence.",
            "Use when the user asks to inspect recurring event characteristics or the contextual evidence around a selected stock move.",
            "Do not infer a causal reason from the result. Preserve unavailable sources, coverage warnings, and counter-evidence when summarizing it.");
    static final String FACTOR_STUDY = guidance(
            "Read the active Agent owner's bounded persisted market-factor studies and their immutable research metadata.",
            "Use when the user asks whether a study is queued, complete, partial, failed, or which frozen window and leakage checks it used.",
            "This is a facts-only owner-scoped read. It cannot queue, cancel, retry, promote, or create a strategy, and it is not trading advice.");
    static final String EVENT_REPLAY = guidance(
            "Read a single bounded stock event replay around an explicit trading date and anchor.",
            "Use to inspect the factual stock-minute path and materialized market/industry coverage around one historical event.",
            "Do not infer causality, use unavailable minutes as facts, or turn the replay into trading advice.");
    static final String FACTOR_SET_VALIDATION = guidance(
            "Read one owner-scoped Factor Set and its active immutable version status.",
            "Use when the user asks whether a named Factor Set is draft, validated or promoted, and needs its persisted validation metadata.",
            "This tool cannot validate, promote, edit, or create a Factor Set or strategy; use it as factual evidence only.");
    static final String STOCK_SELECTION_STRATEGY_HEALTH = guidance(
            "Read the current health assessment for one governed stock-selection strategy in the active user's scope.",
            "Use when the user asks whether a named Screener strategy is Valid, Weakening, Invalid, or not assessed today, and needs coverage, freshness, signal-density, or recent-performance evidence.",
            "This tool reads only the current Agent owner's scoped health result. It cannot run a strategy, start a backtest, read another user's watchlist, or make a trading decision. Treat NOT_ASSESSED and warnings as missing evidence, not a positive signal.");
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
