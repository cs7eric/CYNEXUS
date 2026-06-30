from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.quality.run_quality_check import run
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.provider_fallback import ProviderFetchResult


class FakeClickHouseClient:
    def __init__(self, rows=None) -> None:
        self.calls = 0
        self.rows = rows or []
        self.row_calls = []

    def query_one(self, sql: str, parameters=None):
        self.calls += 1
        if "open <= 0" in sql:
            return (2,)
        if "HAVING row_count > 1" in sql and "ratio_threshold" in sql:
            return (4,)
        if "HAVING row_count > 1" in sql:
            return (9,)
        return (100,)

    def query_rows(self, sql: str, parameters=None):
        self.row_calls.append((sql, dict(parameters or {})))
        return self.rows


class FakeClickHouseHook:
    def __init__(self, client: FakeClickHouseClient) -> None:
        self.client = client

    def get_client(self):
        return self.client


class FakeHooks:
    def __init__(self, client: FakeClickHouseClient) -> None:
        self.clickhouse = FakeClickHouseHook(client)


def test_quality_check_dry_run_without_estimate_skips_clickhouse() -> None:
    client = FakeClickHouseClient()
    context = PipelineContext(hooks=FakeHooks(client))

    result = run(context, JobRequest(job_key="quality_check", dry_run=True, params={}))

    assert result["rows_read"] == 0
    assert client.calls == 0


def test_quality_check_estimate_counts_invalid_rows() -> None:
    client = FakeClickHouseClient()
    context = PipelineContext(hooks=FakeHooks(client))

    result = run(context, JobRequest(job_key="quality_check", dry_run=True, params={"estimate": True, "trade_date": "2026-06-26"}))

    assert result["rows_read"] == 100
    assert result["check_results"] == [{"check": "kline_ohlcv", "ok": False, "invalid_rows": 2}]


def test_quality_check_can_detect_daily_duplicate_and_scale_anomaly() -> None:
    client = FakeClickHouseClient()
    context = PipelineContext(hooks=FakeHooks(client))

    result = run(
        context,
        JobRequest(
            job_key="quality_check",
            dry_run=True,
            params={
                "estimate": True,
                "checks": "daily_duplicate_keys,daily_scale_anomaly",
                "trade_date": "2026-06-29",
                "daily_table": "daily_raw",
            },
        ),
    )

    assert result["check_results"] == [
        {"check": "daily_duplicate_keys", "ok": False, "duplicate_keys": 9, "table": "daily_raw"},
        {"check": "daily_scale_anomaly", "ok": False, "anomaly_keys": 4, "ratio_threshold": 100.0, "table": "daily_raw"},
    ]


def test_quality_check_daily_source_sample_matches_provider(monkeypatch) -> None:
    client = FakeClickHouseClient(
        rows=[
            ("SH", "600000", "600000", "sh600000", __import__("datetime").date(2026, 6, 29), 10.0, 11.0, 9.5, 10.5, 100, 1000.0),
            ("SZ", "000001", "000001", "sz000001", __import__("datetime").date(2026, 6, 29), 20.0, 21.0, 19.5, 20.5, 200, 2000.0),
        ]
    )
    context = PipelineContext(hooks=FakeHooks(client))

    def fake_fetch(hooks, *, symbol, providers, adjust="none", limit=30):
        price = 10.0 if symbol == "sh600000" else 20.0
        return ProviderFetchResult(
            items=[{"trade_date": "2026-06-29", "open": price, "high": price + 1, "low": price - 0.5, "close": price + 0.5}],
            source="tencent",
        )

    monkeypatch.setattr("cynexus_data.pipelines.quality.run_quality_check.fetch_daily_kline_with_fallback", fake_fetch)

    result = run(
        context,
        JobRequest(
            job_key="quality_check",
            dry_run=True,
            params={"estimate": True, "checks": "daily_source_sample", "trade_date": "2026-06-29", "sample_every": 100},
        ),
    )

    assert result["rows_read"] == 2
    check = result["check_results"][0]
    assert check["ok"] is True
    assert check["sampled_rows"] == 2
    assert check["checked_rows"] == 2
    sql, params = client.row_calls[0]
    assert "row_number() OVER (ORDER BY market, symbol)" in sql
    assert "modulo(rn - 1, %(sample_every)s) = 0" in sql
    assert params["sample_every"] == 100


def test_quality_check_daily_source_sample_reports_price_mismatch(monkeypatch) -> None:
    client = FakeClickHouseClient(
        rows=[
            ("SH", "600000", "600000", "sh600000", __import__("datetime").date(2026, 6, 29), 10.0, 11.0, 9.5, 10.5, 100, 1000.0),
        ]
    )
    context = PipelineContext(hooks=FakeHooks(client))

    monkeypatch.setattr(
        "cynexus_data.pipelines.quality.run_quality_check.fetch_daily_kline_with_fallback",
        lambda hooks, *, symbol, providers, adjust="none", limit=30: ProviderFetchResult(
            items=[{"trade_date": "2026-06-29", "open": 100.0, "high": 110.0, "low": 95.0, "close": 105.0}],
            source="eastmoney",
        ),
    )

    result = run(
        context,
        JobRequest(
            job_key="quality_check",
            dry_run=True,
            params={"estimate": True, "checks": "daily_source_sample", "trade_date": "2026-06-29"},
        ),
    )

    check = result["check_results"][0]
    assert check["ok"] is False
    assert check["mismatch_count"] == 1
    assert check["mismatches"][0]["symbol"] == "sh600000"
