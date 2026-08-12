from __future__ import annotations

from datetime import date

import pytest

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.sync_index_daily_raw_from_daily_raw import run
from cynexus_data.runtime.context import PipelineContext


class FakeClickHouseClient:
    def __init__(self) -> None:
        self.commands = []
        self.query_one_calls = []

    def query_one(self, sql, parameters=None):
        self.query_one_calls.append((sql, parameters))
        return (5,)

    def command(self, sql, parameters=None, **kwargs):
        self.commands.append((sql, parameters, kwargs))


class FakeClickHouseHook:
    def __init__(self) -> None:
        self.client = FakeClickHouseClient()

    def get_client(self):
        return self.client


class FakeHooks:
    def __init__(self) -> None:
        self.clickhouse = FakeClickHouseHook()


def test_sync_index_daily_raw_from_daily_raw_deletes_and_inserts_requested_date() -> None:
    hooks = FakeHooks()
    context = PipelineContext(hooks=hooks)

    result = run(
        context,
        JobRequest(
            job_key="sync_index_daily_raw_from_daily_raw",
            dry_run=False,
            params={"trade_date": "2026-07-08"},
        ),
    )

    assert result["rows_read"] == 5
    assert result["rows_written"] == 5
    delete_sql, delete_params, delete_kwargs = hooks.clickhouse.client.commands[0]
    insert_sql, insert_params, _ = hooks.clickhouse.client.commands[1]
    assert "ALTER TABLE index_daily_raw DELETE WHERE" in delete_sql
    assert "code IN %(index_codes)s" in delete_sql
    assert delete_params["start_date"] == date(2026, 7, 8)
    assert delete_params["end_date"] == date(2026, 7, 8)
    assert delete_kwargs["settings"] == {"mutations_sync": 1}
    assert "INSERT INTO index_daily_raw" in insert_sql
    assert "FROM daily_raw FINAL" in insert_sql
    assert "tdx_code AS code" in insert_sql
    assert insert_params["index_codes"] == ["sh000001", "sh000300", "sh000852", "sz399001", "sz399006"]


def test_sync_index_daily_raw_from_daily_raw_dry_run_skips_clickhouse() -> None:
    hooks = FakeHooks()
    context = PipelineContext(hooks=hooks)

    result = run(context, JobRequest(job_key="sync_index_daily_raw_from_daily_raw", dry_run=True, params={}))

    assert result["rows_written"] == 0
    assert hooks.clickhouse.client.commands == []


def test_sync_index_daily_raw_from_daily_raw_rejects_unbounded_replace() -> None:
    hooks = FakeHooks()
    context = PipelineContext(hooks=hooks)

    with pytest.raises(ValueError, match="requires trade_date/start_date/end_date"):
        run(context, JobRequest(job_key="sync_index_daily_raw_from_daily_raw", dry_run=False, params={}))

    assert hooks.clickhouse.client.commands == []
    assert hooks.clickhouse.client.query_one_calls == []
