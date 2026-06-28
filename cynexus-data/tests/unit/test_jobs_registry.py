from __future__ import annotations

from cynexus_data.jobs.registry import JOB_REGISTRY, default_registry


def test_required_jobs_are_registered() -> None:
    for key in [
        "health_check",
        "sync_kline_daily",
        "sync_adjusted_kline_daily",
        "sync_kline_minute",
        "sync_tdx_hsjday",
        "aggregate_kline_periods",
        "sync_fund_flow",
        "sync_stock_fund_flow",
        "sync_ths_industry_constituents",
        "build_msi",
        "transcribe_media",
        "sync_stock_sectors",
    ]:
        assert key in JOB_REGISTRY


def test_default_registry_exposes_sorted_job_descriptions() -> None:
    registry = default_registry()
    jobs = registry.list_jobs()

    assert [job.key for job in jobs] == sorted(JOB_REGISTRY)
    assert registry.get("health_check") is not None
    assert registry.descriptions()["health_check"]["supports_dry_run"] is True
    assert registry.as_mapping()["health_check"] is JOB_REGISTRY["health_check"]
