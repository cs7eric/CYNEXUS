from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.aggregate_periods import run
from cynexus_data.runtime.context import PipelineContext


class FakeClickHouseClient:
    def __init__(self) -> None:
        self.query_count = 0
        self.command_count = 0

    def query_one(self, sql: str, parameters=None):
        self.query_count += 1
        if "FROM (" in sql:
            return (3,)
        return (12,)

    def command(self, sql: str, parameters=None, **kwargs):
        self.command_count += 1
        return None


class FakeClickHouseHook:
    def __init__(self, client: FakeClickHouseClient) -> None:
        self.client = client

    def get_client(self) -> FakeClickHouseClient:
        return self.client


class FakeHooks:
    def __init__(self, client: FakeClickHouseClient) -> None:
        self.clickhouse = FakeClickHouseHook(client)


def test_aggregate_periods_dry_run_without_estimate_skips_clickhouse() -> None:
    client = FakeClickHouseClient()
    context = PipelineContext(hooks=FakeHooks(client))

    result = run(context, JobRequest(job_key="aggregate_kline_periods", dry_run=True, params={"periods": "5,30"}))

    assert result["rows_read"] == 0
    assert result["periods"] == [5, 30]
    assert client.query_count == 0
    assert client.command_count == 0


def test_aggregate_periods_executes_clickhouse_insert_when_not_dry_run() -> None:
    client = FakeClickHouseClient()
    context = PipelineContext(hooks=FakeHooks(client))

    result = run(
        context,
        JobRequest(
            job_key="aggregate_kline_periods",
            dry_run=False,
            params={"periods": [5], "trade_date": "2026-06-26", "symbols": ["000001"]},
        ),
    )

    assert result["rows_read"] == 12
    assert result["rows_written"] == 3
    assert result["period_results"] == [{"period": 5, "expected_rows": 3, "rows_written": 3}]
    assert client.command_count == 2
