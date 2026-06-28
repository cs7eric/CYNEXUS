from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.quality.run_quality_check import run
from cynexus_data.runtime.context import PipelineContext


class FakeClickHouseClient:
    def __init__(self) -> None:
        self.calls = 0

    def query_one(self, sql: str, parameters=None):
        self.calls += 1
        if "open <= 0" in sql:
            return (2,)
        return (100,)


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
