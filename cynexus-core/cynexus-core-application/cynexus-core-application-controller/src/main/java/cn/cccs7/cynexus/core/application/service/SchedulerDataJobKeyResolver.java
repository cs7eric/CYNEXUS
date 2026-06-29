package cn.cccs7.cynexus.core.application.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class SchedulerDataJobKeyResolver {

    private static final Map<String, String> LEGACY_TO_DATA_JOB = Map.ofEntries(
            Map.entry("test_scheduler_demo", "trigger_probe"),
            Map.entry("initial_backfill_refresh", "sync_kline_daily"),
            Map.entry("qfq_reconciliation_refresh", "sync_adjusted_kline_daily"),
            Map.entry("tdx_hsjday_download", "sync_tdx_hsjday"),
            Map.entry("stock_universe_refresh", "sync_stock_basic"),
            Map.entry("ths_industry_constituents_daily", "sync_ths_industry_constituents"),
            Map.entry("ths_industry_constituents_weekly", "sync_ths_industry_constituents"),
            Map.entry("ths_industry_fund_flow_daily", "sync_fund_flow"),
            Map.entry("turnover", "sync_stock_turnover"),
            Map.entry("turnover_refresh", "sync_stock_turnover"),
            Map.entry("limit_emotion_refresh", "build_limit_emotion"),
            Map.entry("ma_count", "build_ma_count"),
            Map.entry("ma_count_refresh", "build_ma_count"),
            Map.entry("market_sentiment_index_refresh", "build_msi"),
            Map.entry("market_sentiment_chain_refresh", "rebuild_msi"),
            Map.entry("profit_effect_refresh", "build_profit_effect"),
            Map.entry("risk_appetite_refresh", "build_risk_appetite"),
            Map.entry("sector_breadth_refresh", "build_sector_breadth"),
            Map.entry("style_risk_appetite_refresh", "build_style_risk"),
            Map.entry("turnover_activity_refresh", "build_turnover_activity"),
            Map.entry("volatility_sentiment_refresh", "build_volatility_sentiment"),
            Map.entry("market_overview_daily", "build_market_overview"),
            Map.entry("market_overview_inside", "build_market_overview"),
            Map.entry("market_overview_close", "build_market_overview"),
            Map.entry("market_overview_warmup", "build_market_overview"),
            Map.entry("eltdx_overview_inside", "build_market_overview"),
            Map.entry("eltdx_overview_close", "build_market_overview"),
            Map.entry("eltdx_overview_warmup", "build_market_overview"),
            Map.entry("market_pulse_inside", "build_market_pulse"),
            Map.entry("market_pulse_close", "build_market_pulse"),
            Map.entry("market_pulse_constituents", "build_market_pulse")
    );
    private static final Set<String> UNSUPPORTED_LEGACY_JOBS = Set.of(
            "application_analysis",
            "auction_ai_analysis",
            "daily_eod_incremental"
    );

    private SchedulerDataJobKeyResolver() {
    }

    static Optional<String> resolve(String schedulerJobCode) {
        if (schedulerJobCode == null || schedulerJobCode.isBlank()) {
            return Optional.empty();
        }
        if (UNSUPPORTED_LEGACY_JOBS.contains(schedulerJobCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(LEGACY_TO_DATA_JOB.get(schedulerJobCode));
    }
}
