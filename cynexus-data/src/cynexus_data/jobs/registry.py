from __future__ import annotations

from dataclasses import dataclass
from typing import Mapping

from cynexus_data.jobs import (
    aggregate_kline_periods,
    backfill_history,
    build_index_returns,
    build_market_heatmap,
    build_ma_count,
    build_limit_emotion,
    build_market_pulse,
    build_market_overview,
    build_market_overview_detail,
    build_sector_breadth,
    build_style_sector,
    build_msi,
    build_profit_effect,
    build_risk_appetite,
    build_style_risk,
    build_turnover_activity,
    build_volatility_sentiment,
    health_check,
    quality_check,
    rebuild_msi,
    repair_missing_data,
    sync_adjusted_kline_daily,
    sync_fund_flow,
    sync_kline_daily,
    sync_kline_minute,
    sync_margin,
    sync_metadata,
    sync_stock_basic,
    sync_stock_fund_flow,
    sync_stock_sectors,
    sync_stock_turnover,
    sync_tdx_industry_quotes,
    sync_tdx_intraday_detail,
    sync_tdx_sector_quote_details,
    sync_tdx_hsjday,
    sync_ths_industry_constituents,
    sync_trade_calendar,
    transcribe_media,
    trigger_probe,
)
from cynexus_data.runtime.job_runner import JobCallable


JOB_REGISTRY = {
    "health_check": health_check.run,
    "sync_metadata": sync_metadata.run,
    "sync_trade_calendar": sync_trade_calendar.run,
    "sync_stock_basic": sync_stock_basic.run,
    "sync_stock_sectors": sync_stock_sectors.run,
    "sync_stock_turnover": sync_stock_turnover.run,
    "sync_ths_industry_constituents": sync_ths_industry_constituents.run,
    "sync_tdx_industry_quotes": sync_tdx_industry_quotes.run,
    "sync_tdx_intraday_detail": sync_tdx_intraday_detail.run,
    "sync_tdx_sector_quote_details": sync_tdx_sector_quote_details.run,
    "sync_kline_daily": sync_kline_daily.run,
    "sync_adjusted_kline_daily": sync_adjusted_kline_daily.run,
    "sync_kline_minute": sync_kline_minute.run,
    "sync_tdx_hsjday": sync_tdx_hsjday.run,
    "aggregate_kline_periods": aggregate_kline_periods.run,
    "sync_fund_flow": sync_fund_flow.run,
    "sync_stock_fund_flow": sync_stock_fund_flow.run,
    "build_index_returns": build_index_returns.run,
    "build_market_heatmap": build_market_heatmap.run,
    "build_market_overview": build_market_overview.run,
    "build_market_overview_detail": build_market_overview_detail.run,
    "build_market_pulse": build_market_pulse.run,
    "build_style_sector": build_style_sector.run,
    "build_ma_count": build_ma_count.run,
    "build_turnover_activity": build_turnover_activity.run,
    "build_volatility_sentiment": build_volatility_sentiment.run,
    "build_risk_appetite": build_risk_appetite.run,
    "build_style_risk": build_style_risk.run,
    "build_limit_emotion": build_limit_emotion.run,
    "sync_margin": sync_margin.run,
    "build_sector_breadth": build_sector_breadth.run,
    "build_msi": build_msi.run,
    "build_profit_effect": build_profit_effect.run,
    "rebuild_msi": rebuild_msi.run,
    "backfill_history": backfill_history.run,
    "repair_missing_data": repair_missing_data.run,
    "quality_check": quality_check.run,
    "transcribe_media": transcribe_media.run,
    "trigger_probe": trigger_probe.run,
}


@dataclass(frozen=True)
class RegisteredJob:
    key: str
    handler: JobCallable
    supports_dry_run: bool = True

    @property
    def callable_path(self) -> str:
        return f"{self.handler.__module__}.{self.handler.__name__}"

    def to_description(self) -> dict[str, object]:
        return {
            "job_key": self.key,
            "callable": self.callable_path,
            "supports_dry_run": self.supports_dry_run,
        }


class JobRegistry:
    def __init__(self, jobs: Mapping[str, JobCallable]) -> None:
        self._jobs = {
            key: RegisteredJob(key=key, handler=handler)
            for key, handler in jobs.items()
        }

    def get(self, key: str) -> RegisteredJob | None:
        return self._jobs.get(key)

    def list_jobs(self) -> list[RegisteredJob]:
        return [self._jobs[key] for key in sorted(self._jobs)]

    def as_mapping(self) -> dict[str, JobCallable]:
        return {key: job.handler for key, job in self._jobs.items()}

    def descriptions(self) -> dict[str, dict[str, object]]:
        return {job.key: job.to_description() for job in self.list_jobs()}


def default_registry() -> JobRegistry:
    return JobRegistry(JOB_REGISTRY)


def job_descriptions() -> dict[str, dict]:
    return default_registry().descriptions()
