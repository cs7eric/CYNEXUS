package cn.cccs7.cynexus.core.application.service;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SchedulerDataJobKeyResolver {

    private static final Map<String, ResolvedDataJob> LEGACY_TO_DATA_JOB = Map.ofEntries(
            entry("test_scheduler_demo", "trigger_probe"),
            entry("initial_backfill_refresh", "sync_kline_daily"),
            entry("qfq_reconciliation_refresh", "sync_adjusted_kline_daily"),
            entry("tdx_hsjday_download", "sync_tdx_hsjday"),
            entry("stock_universe_refresh", "sync_stock_sectors", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("trading_day_only", true),
                    Map.entry("shard_size", 800),
                    Map.entry("workers", 64),
                    Map.entry("pool_size", 8),
                    Map.entry("timeout", 6),
                    Map.entry("max_retry_rounds", 5),
                    Map.entry("batch_timeout_seconds", 180),
                    Map.entry("sleep_first", 100),
                    Map.entry("sleep_retry", 5)
            )),
            entry("ths_industry_constituents_daily", "sync_ths_industry_constituents"),
            entry("ths_industry_constituents_weekly", "sync_ths_industry_constituents"),
            entry("ths_industry_fund_flow_daily", "sync_fund_flow"),
            entry("tdx_industry_fund_flow_daily", "sync_tdx_industry_fund_flow", Map.ofEntries(
                    Map.entry("create_table", true)
            )),
            entry("stock_fund_flow_daily", "sync_stock_fund_flow", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("code_source", "self_selected"),
                    Map.entry("batch_size", 120)
            )),
            entry("turnover", "sync_stock_turnover", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("target_type", "stock"),
                    Map.entry("period", "1d"),
                    Map.entry("adjust", "qfq"),
                    Map.entry("trading_day_only", true),
                    Map.entry("limit", 260),
                    Map.entry("source", "clickhouse+eltdx"),
                    Map.entry("pool_size", 4),
                    Map.entry("timeout", 8)
            )),
            entry("turnover_refresh", "sync_stock_turnover", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("target_type", "stock"),
                    Map.entry("period", "1d"),
                    Map.entry("adjust", "qfq"),
                    Map.entry("trading_day_only", true),
                    Map.entry("limit", 260),
                    Map.entry("source", "clickhouse+eltdx"),
                    Map.entry("pool_size", 4),
                    Map.entry("timeout", 8)
            )),
            entry("limit_emotion_refresh", "build_limit_emotion"),
            entry("ma_count", "build_ma_count"),
            entry("ma_count_refresh", "build_ma_count"),
            entry("market_sentiment_index_refresh", "build_msi"),
            entry("market_sentiment_chain_refresh", "market_sentiment_chain_refresh"),
            entry("profit_effect_refresh", "build_profit_effect"),
            entry("risk_appetite_refresh", "build_risk_appetite"),
            entry("sector_breadth_refresh", "build_sector_breadth"),
            entry("style_risk_appetite_refresh", "build_style_risk"),
            entry("turnover_activity_refresh", "build_turnover_activity"),
            entry("volatility_sentiment_refresh", "build_volatility_sentiment"),
            entry("market_overview_daily", "build_market_overview"),
            entry("market_overview_inside", "build_market_overview", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("source", "akshare"),
                    Map.entry("lock_scope", "akshare")
            )),
            entry("market_overview_close", "build_market_overview", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("source", "akshare"),
                    Map.entry("lock_scope", "akshare")
            )),
            entry("market_overview_warmup", "build_market_overview", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("source", "akshare"),
                    Map.entry("lock_scope", "akshare")
            )),
            entry("eltdx_overview_inside", "build_market_overview", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("source", "eltdx"),
                    Map.entry("lock_scope", "eltdx")
            )),
            entry("eltdx_overview_close", "build_market_overview", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("source", "eltdx"),
                    Map.entry("lock_scope", "eltdx")
            )),
            entry("eltdx_overview_warmup", "build_market_overview", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("source", "eltdx"),
                    Map.entry("lock_scope", "eltdx")
            )),
            entry("market_pulse_inside", "build_market_pulse", Map.ofEntries(
                    Map.entry("create_table", true)
            )),
            entry("market_pulse_close", "build_market_pulse", Map.ofEntries(
                    Map.entry("create_table", true)
            )),
            entry("market_pulse_constituents", "sync_ths_industry_constituents", Map.ofEntries(
                    Map.entry("create_table", true)
            )),
            entry("kline_1m_intraday_realtime", "sync_kline_minute", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("replace_existing", true),
                    Map.entry("providers", List.of("tencent", "mootdx")),
                    Map.entry("target_type", "auto"),
                    Map.entry("period", 1),
                    Map.entry("limit", 320),
                    Map.entry("codes", List.of("sh000001", "sz399001", "sz399006"))
            )),
            entry("kline_1m_tdx_close", "sync_kline_minute", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("replace_existing", true),
                    Map.entry("providers", List.of("tdx")),
                    Map.entry("target_type", "auto"),
                    Map.entry("tdx_root", "/app/tdx")
            )),
            entry("a_stock_marketwide_daily", "sync_a_stock_dataset", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("ignore_errors", true),
                    Map.entry("datasets", List.of("eastmoney_industry_rank", "eastmoney_daily_dragon_tiger", "eastmoney_limit_pool", "eastmoney_hot_rank", "eastmoney_global_news", "ths_hot_reason", "ths_limit_up_pool", "ths_hot_list", "sina_option_codes")),
                    Map.entry("pool", "zt"),
                    Map.entry("top", 100),
                    Map.entry("top_n", 30),
                    Map.entry("page_size", 50)
            )),
            entry("a_stock_code_daily", "sync_a_stock_dataset", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("ignore_errors", true),
                    Map.entry("code_source", "self_selected"),
                    Map.entry("limit_codes", 30),
                    Map.entry("fund_flow_mode", "daily"),
                    Map.entry("datasets", List.of("eastmoney_stock_info", "eastmoney_concept_blocks", "eastmoney_fund_flow_120d", "eastmoney_block_trade", "eastmoney_holders", "eastmoney_dividends", "eastmoney_stock_news", "sina_stock_news", "eastmoney_hot_concept", "cninfo_announcements", "cninfo_irm", "sina_financial_report")),
                    Map.entry("page_size", 20),
                    Map.entry("num", 8)
            )),
            entry("a_stock_fund_flow_history_backfill", "sync_a_stock_dataset", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("ignore_errors", false),
                    Map.entry("code_source", "fund_flow_history_missing"),
                    Map.entry("limit_codes", 1),
                    Map.entry("datasets", List.of("eastmoney_fund_flow_120d")),
                    Map.entry("fund_flow_mode", "history"),
                    Map.entry("request_sleep_seconds", 5)
            )),
            entry("margin_stock_history_backfill", "sync_margin", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("mode", "stock_history"),
                    Map.entry("code_source", "self_selected"),
                    Map.entry("limit_codes", 1),
                    Map.entry("page_size", 120),
                    Map.entry("providers", List.of("eastmoney", "exchange", "akshare"))
            )),
            entry("a_stock_research_weekly", "sync_a_stock_dataset", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("ignore_errors", true),
                    Map.entry("code_source", "self_selected"),
                    Map.entry("limit_codes", 20),
                    Map.entry("datasets", List.of("eastmoney_reports", "eastmoney_industry_reports", "iwencai_search", "ths_eps_forecast")),
                    Map.entry("max_pages", 1),
                    Map.entry("query", "A股 研报 盈利预测"),
                    Map.entry("size", 30)
            )),
            entry("a_stock_fund_flow_intraday", "sync_a_stock_dataset", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("ignore_errors", true),
                    Map.entry("code_source", "self_selected"),
                    Map.entry("limit_codes", 30),
                    Map.entry("datasets", List.of("eastmoney_fund_flow_minute", "ths_northbound")),
                    Map.entry("request_sleep_seconds", 2)
            )),
            entry("a_stock_tdx_realtime_probe", "sync_a_stock_dataset", Map.ofEntries(
                    Map.entry("create_table", true),
                    Map.entry("ignore_errors", true),
                    Map.entry("code_source", "self_selected"),
                    Map.entry("limit_codes", 20),
                    Map.entry("datasets", List.of("tdx_order_book", "tdx_transactions")),
                    Map.entry("request_sleep_seconds", 0.2)
            )),
            entry("auction_ai_analysis", "sync_tdx_intraday_detail", Map.of(
                    "datasets", List.of("auction"),
                    "create_table", true
            )),
            entry("auction_opening_refresh", "sync_tdx_intraday_detail", Map.of(
                    "datasets", List.of("auction"),
                    "auction_phase", "opening",
                    "create_table", true,
                    "ignore_errors", true
            )),
            entry("auction_closing_refresh", "sync_tdx_intraday_detail", Map.of(
                    "datasets", List.of("auction"),
                    "auction_phase", "closing",
                    "create_table", true,
                    "ignore_errors", true
            ))
    );
    private static final Set<String> UNSUPPORTED_LEGACY_JOBS = Set.of(
            "application_analysis",
            "daily_eod_incremental"
    );

    private SchedulerDataJobKeyResolver() {
    }

    static Optional<ResolvedDataJob> resolve(String schedulerJobCode) {
        if (schedulerJobCode == null || schedulerJobCode.isBlank()) {
            return Optional.empty();
        }
        if (UNSUPPORTED_LEGACY_JOBS.contains(schedulerJobCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(LEGACY_TO_DATA_JOB.get(schedulerJobCode));
    }

    private static Map.Entry<String, ResolvedDataJob> entry(String schedulerJobCode, String dataJobKey) {
        return entry(schedulerJobCode, dataJobKey, Map.of());
    }

    private static Map.Entry<String, ResolvedDataJob> entry(String schedulerJobCode,
                                                           String dataJobKey,
                                                           Map<String, Object> defaultParams) {
        return Map.entry(schedulerJobCode, new ResolvedDataJob(dataJobKey, defaultParams));
    }

    record ResolvedDataJob(String jobKey, Map<String, Object> defaultParams) {
    }
}
