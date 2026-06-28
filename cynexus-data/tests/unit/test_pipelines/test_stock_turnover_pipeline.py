from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.stock_basic import sync_stock_turnover
from cynexus_data.runtime.context import PipelineContext


class FakeClickHouseClient:
    pass


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

    def upsert_series(self, series, *, source=None):
        FakeRepo.calls.append({"series": series, "source": source})
        return {"entry_count": len(series["entries"])}


def test_sync_stock_turnover_dry_run_uses_supplied_bars_and_shares() -> None:
    result = sync_stock_turnover.run(
        PipelineContext(),
        JobRequest(
            job_key="sync_stock_turnover",
            dry_run=True,
            params={
                "codes": "sh600000",
                "bars_by_code": json.dumps({"600000": [{"trade_date": "2026-06-26", "volume": 12000, "amount": 1}]}),
                "shares_by_code": json.dumps({"600000": {"circulating_shares": 1_000_000_000}}),
            },
        ),
    )

    assert result["rows_read"] == 1
    assert result["planned_rows"] == 1
    assert result["sample"][0]["symbol"] == "600000"
    assert result["sample"][0]["entries"][0]["turnover_rate"] == 0.12


def test_sync_stock_turnover_reads_clickhouse_and_persists(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_stock_turnover, "StockTurnoverRepository", FakeRepo)
    monkeypatch.setattr(
        sync_stock_turnover,
        "read_daily_kline_bars",
        lambda client, code, adjust="qfq", limit=260: [{"trade_date": "2026-06-26", "volume": 12000, "amount": 1}],
    )

    result = sync_stock_turnover.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_stock_turnover",
            dry_run=False,
            params={
                "codes": "600000",
                "shares_by_code": json.dumps({"600000": {"circulating_shares": 1_000_000_000, "total_shares": 1_200_000_000}}),
                "create_table": True,
            },
        ),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0]["series"]["entries"][0]["turnover_rate"] == 0.12
