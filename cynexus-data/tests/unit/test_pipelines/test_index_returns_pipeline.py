from __future__ import annotations

from datetime import date, timedelta

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import build_index_returns
from cynexus_data.runtime.context import PipelineContext


class FakeClickHouseClient:
    def query_rows(self, sql, params=None):
        code = (params or {}).get("code")
        start = date(2026, 6, 20)
        return [
            (code, start + timedelta(days=index), 100 + index, 101 + index, 99 + index, 100 + index, 1000 + index, 100000 + index)
            for index in range(6)
        ]


class FakeClickHouseHook:
    def get_client(self):
        return FakeClickHouseClient()


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    clickhouse = FakeClickHouseHook()
    postgres = FakePostgresHook()


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def save_many(self, *, trade_date, window_days, items, source="clickhouse.index_daily_raw"):
        FakeRepo.calls.append({"trade_date": trade_date, "window_days": window_days, "items": items, "source": source})
        return len([item for item in items if item.get("available")])


def test_build_index_returns_dry_run_uses_synthetic_rows() -> None:
    result = build_index_returns.run(
        PipelineContext(),
        JobRequest(job_key="build_index_returns", dry_run=True, params={"trade_date": "2026-06-26", "window_days": 5}),
    )

    assert result["rows_written"] == 0
    assert result["rows_read"] > 0
    assert result["items"][0]["available"] is True
    assert "dry_run used synthetic index rows" in result["notes"]


def test_build_index_returns_reads_clickhouse_and_persists(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(build_index_returns, "IndexReturnRepository", FakeRepo)

    result = build_index_returns.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="build_index_returns", dry_run=False, params={"trade_date": "2026-06-26", "days": 5, "create_table": True}),
    )

    assert result["rows_written"] == 3
    assert FakeRepo.created == 1
    call = FakeRepo.calls[0]
    assert call["trade_date"] == date(2026, 6, 26)
    assert call["window_days"] == 5
    assert call["source"] == "clickhouse.index_daily_raw"
    assert {item["fullCode"] for item in call["items"]} == {"sh000001", "sh000300", "sh000852"}
