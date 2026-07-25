package cn.cccs7.cynexus.ai.infra.agent;

/** Model-facing usage guidance. Runtime safety remains defined by the Handler schema and flags. */
public final class AgentToolGuidanceCatalog {

    public static final String MARKET_OVERVIEW = """
            {
              "purpose": "Summarize market breadth, turnover, major index performance, and fund flow for one trading session.",
              "useWhen": ["The user asks how the overall market performed on a trading date.", "The user needs a broad market context before analyzing a sector or stock."],
              "avoidWhen": ["The user asks for one stock's detailed metrics; use a stock feature tool instead.", "The user asks for a date range; use market.overview_history instead."],
              "parameterNotes": {"tradeDate": "YYYY-MM-DD trading date. Omit it to use the latest available Asia/Shanghai session."},
              "examples": [{"arguments": {"tradeDate": "2026-07-24"}, "note": "Read the market snapshot for a specific session."}],
              "resultNotes": "The result is a daily market snapshot; cite the returned data date when summarizing it."
            }
            """;

    public static final String MARKET_OVERVIEW_HISTORY = """
            {
              "purpose": "Compare market overview snapshots across a bounded date range.",
              "useWhen": ["The user asks about market trend or changes across several sessions.", "The user wants historical breadth or turnover context."],
              "avoidWhen": ["The user asks for one session only; use market.overview.", "The user asks for individual stock history; use a stock chart or daily bars tool."],
              "parameterNotes": {"start": "Optional YYYY-MM-DD start date.", "end": "Optional YYYY-MM-DD end date. Omit both to use the recent session window."},
              "examples": [{"arguments": {"start": "2026-07-18", "end": "2026-07-24"}, "note": "Compare the last trading sessions."}],
              "resultNotes": "The result contains daily market snapshots for the requested range."
            }
            """;

    public static final String MARKET_SECTOR_RANK = """
            {
              "purpose": "Rank sectors by change percentage and net capital inflow for a trading session.",
              "useWhen": ["The user asks which sectors led or lagged the market.", "The user asks about sector capital inflow or outflow."],
              "avoidWhen": ["The user asks for constituents or individual stock metrics."],
              "parameterNotes": {"tradeDate": "Optional YYYY-MM-DD trading date.", "topN": "Optional number of sectors, from 1 to 50; omit it for the default ranking size."},
              "examples": [{"arguments": {"tradeDate": "2026-07-24", "topN": 10}, "note": "Read the top ten sector rankings."}],
              "resultNotes": "Use the returned ranking metric and data date together; do not infer a causal reason from ranking alone."
            }
            """;

    public static final String MARKET_STOCK_DAILY = """
            {
              "purpose": "Read one stock's daily price, valuation, and technical feature snapshot.",
              "useWhen": ["The user asks about a stock's daily metrics, valuation, or technical indicators.", "The user wants a historical daily snapshot for a named stock."],
              "avoidWhen": ["The user asks for latest intraday activity; use market.stock_intraday_feature.", "The user asks for a multi-day chart; use market.stock_chart_klines or market.clickhouse_daily_bars."],
              "parameterNotes": {"symbol": "Required stock code such as 600519.", "tradeDate": "Optional YYYY-MM-DD trading date; omit it for the latest available session."},
              "examples": [{"arguments": {"symbol": "600519", "tradeDate": "2026-07-24"}, "note": "Read a stock daily feature snapshot."}],
              "resultNotes": "Treat the returned date as the observation date; it may differ from the requested date when the market has no data."
            }
            """;

    public static final String MARKET_STOCK_INTRADAY = """
            {
              "purpose": "Read one stock's latest intraday price, volume, and market activity features.",
              "useWhen": ["The user asks for current-session or intraday stock activity.", "The user asks about volume or price activity during the latest session."],
              "avoidWhen": ["The user asks for valuation or daily technical history; use market.stock_daily_feature.", "The user asks for a multi-day series."],
              "parameterNotes": {"symbol": "Required stock code such as 600519.", "tradeDate": "Optional YYYY-MM-DD session date; omit it for the latest available session."},
              "examples": [{"arguments": {"symbol": "600519"}, "note": "Read the latest available intraday feature snapshot."}],
              "resultNotes": "Clearly distinguish intraday observations from end-of-day or historical data in the answer."
            }
            """;

    public static final String MARKET_STOCK_CHART_KLINES = """
            {
              "purpose": "Read historical K-line bars for a stock, index, sector, or ETF.",
              "useWhen": ["The user asks for a price chart or historical OHLC series.", "The user needs a time series for trend comparison."],
              "avoidWhen": ["The user needs valuation or technical feature fields for one day; use a daily feature tool.", "The user asks for current intraday activity."],
              "parameterNotes": {"targetType": "Optional stock, index, sector, or etf; defaults to stock.", "symbol": "Required identifier.", "period": "Optional bar period; defaults to day.", "adjust": "Optional price adjustment: none, qfq, or hfq; defaults to qfq."},
              "examples": [{"arguments": {"targetType": "stock", "symbol": "600519", "period": "day", "adjust": "qfq"}, "note": "Read adjusted daily K-lines."}],
              "resultNotes": "Use the returned period and adjustment when describing the series; do not treat K-lines as a valuation report."
            }
            """;

    public static final String MARKET_INFORMATION_FEED = """
            {
              "purpose": "Read persisted market information items, event impact mappings, and analysis status.",
              "useWhen": ["The user asks for market news or persisted event information.", "The user asks to filter information by industry or analysis status."],
              "avoidWhen": ["The user asks for raw price or sector rankings; use the market data tools."],
              "parameterNotes": {"page": "Optional page number from 1 to 20.", "pageSize": "Optional page size from 1 to 50.", "impactIndustryCode": "Optional industry code filter.", "analysisStatus": "Optional analysis status filter."},
              "examples": [{"arguments": {"page": 1, "pageSize": 20}, "note": "Read the first page of persisted market information."}],
              "resultNotes": "Separate persisted information and analysis status from verified market price data."
            }
            """;

    public static final String MARKET_CLICKHOUSE_DAILY_BARS = """
            {
              "purpose": "Read bounded historical OHLCV daily bars from the approved market dataset.",
              "useWhen": ["The user needs raw daily open, high, low, close, volume, or amount values.", "The user asks for a bounded historical bar series."],
              "avoidWhen": ["The user needs a summarized stock feature snapshot.", "The requested range or result would be unbounded."],
              "parameterNotes": {"market": "Optional SH, SZ, or BJ market code.", "symbol": "Required stock code.", "start": "Optional YYYY-MM-DD start date.", "end": "Optional YYYY-MM-DD end date.", "limit": "Optional row limit from 1 to 300; omit it for the bounded default."},
              "examples": [{"arguments": {"market": "SH", "symbol": "600519", "limit": 60}, "note": "Read a bounded recent daily bar series."}],
              "resultNotes": "The rows are raw daily bars ordered from newest to oldest; preserve the returned date range in the answer."
            }
            """;

    public static final String FINANCE_OVERVIEW = """
            {
              "purpose": "Summarize income, expense, net amount, assets, accounts, categories, and daily finance data.",
              "useWhen": ["The user asks for a financial overview or spending summary.", "The user needs totals before drilling into individual transactions."],
              "avoidWhen": ["The user asks for transaction-level details; use finance.transactions."],
              "parameterNotes": {"start": "Optional YYYY-MM-DD beginning of the finance range.", "end": "Optional YYYY-MM-DD end of the finance range."},
              "examples": [{"arguments": {"start": "2026-07-01", "end": "2026-07-24"}, "note": "Summarize finances for a date range."}],
              "resultNotes": "Distinguish income, expense, net amount, and net assets; do not treat missing totals as zero without evidence."
            }
            """;

    public static final String FINANCE_TRANSACTIONS = """
            {
              "purpose": "Read a bounded list of ledger transactions for spending details and audit context.",
              "useWhen": ["The user asks where money was spent or wants transaction details.", "The user needs merchant, account, category, or note-level evidence."],
              "avoidWhen": ["The user asks only for totals or net assets; use finance.overview.", "The user requests an unbounded full ledger export."],
              "parameterNotes": {"start": "Optional YYYY-MM-DD beginning of the transaction range.", "end": "Optional YYYY-MM-DD end of the transaction range.", "limit": "Optional result limit from 1 to 100; keep it bounded."},
              "examples": [{"arguments": {"start": "2026-07-01", "end": "2026-07-24", "limit": 50}, "note": "Inspect transactions for a bounded period."}],
              "resultNotes": "Use transaction fields as evidence and retain the transaction date, amount, and currency when available."
            }
            """;

    public static final String KNOWLEDGE_SEARCH = """
            {
              "purpose": "Search approved published knowledge-base documents by keyword.",
              "useWhen": ["The user asks about information that may be documented in the Cynexus knowledge base.", "The Agent needs grounded documents before answering."],
              "avoidWhen": ["The user already supplied the complete facts.", "The user asks for a specific document and already has its id; use knowledge.document."],
              "parameterNotes": {"query": "Required keyword query from 2 to 240 characters.", "limit": "Optional result limit from 1 to 10."},
              "examples": [{"arguments": {"query": "market risk policy", "limit": 5}, "note": "Find published documents matching a topic."}],
              "resultNotes": "Only published documents are returned; use knowledge.document to read the full content of a selected document."
            }
            """;

    public static final String KNOWLEDGE_DOCUMENT = """
            {
              "purpose": "Read one approved published knowledge-base document by id.",
              "useWhen": ["The user or a prior search result identifies a document id.", "The Agent needs the full document content for grounded analysis."],
              "avoidWhen": ["The document id is unknown; search first with knowledge.search."],
              "parameterNotes": {"documentId": "Required positive document id returned by the knowledge base."},
              "examples": [{"arguments": {"documentId": 42}, "note": "Read the full content of a selected document."}],
              "resultNotes": "Verify the returned document status and title before using its content as evidence."
            }
            """;

    public static final String APPLICATION_ANALYSIS_RESULT = """
            {
              "purpose": "Read the persisted application-analysis result for one configured target.",
              "useWhen": ["The user asks for the latest completed analysis result of a configured target.", "The Agent needs the stored result instead of rerunning analysis."],
              "avoidWhen": ["The user asks for the recent daily history; use application_analysis.recent30."],
              "parameterNotes": {"targetId": "Required configured application-analysis target identifier, 1 to 128 characters."},
              "examples": [{"arguments": {"targetId": "600519"}, "note": "Read the stored analysis result for a target."}],
              "resultNotes": "The result is persisted analysis output; distinguish it from live market data."
            }
            """;

    public static final String APPLICATION_ANALYSIS_RECENT30 = """
            {
              "purpose": "Read persisted daily application-analysis snapshots for one configured target.",
              "useWhen": ["The user asks about analysis changes over recent days.", "The Agent needs the stored daily analysis history."],
              "avoidWhen": ["The user asks for the current final analysis result; use application_analysis.result."],
              "parameterNotes": {"targetId": "Required configured application-analysis target identifier, 1 to 128 characters.", "limit": "Optional snapshot count from 1 to 60."},
              "examples": [{"arguments": {"targetId": "600519", "limit": 30}, "note": "Read up to 30 recent daily snapshots."}],
              "resultNotes": "The snapshots are persisted daily observations and may not represent the current live session."
            }
            """;

    private AgentToolGuidanceCatalog() {
    }
}
