from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.stock_basic import sync_trade_calendar
from cynexus_data.runtime.context import PipelineContext


class FakeRepo:
    created = 0
    records = []

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        FakeRepo.created += 1

    def upsert_many(self, records, *, source="local_rules"):
        FakeRepo.records = list(records)
        return len(records)


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    postgres = FakePostgresHook()


def test_sync_trade_calendar_dry_run() -> None:
    result = sync_trade_calendar.run(PipelineContext(), JobRequest(job_key="sync_trade_calendar", dry_run=True, params={"start_date": "2026-01-01", "end_date": "2026-01-05"}))
    assert result["rows_read"] == 5
    assert result["trading_days"] == 1


def test_sync_trade_calendar_writes(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.records = []
    monkeypatch.setattr(sync_trade_calendar, "TradeCalendarRepository", FakeRepo)
    result = sync_trade_calendar.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="sync_trade_calendar", dry_run=False, params={"start_date": "2026-01-01", "end_date": "2026-01-05", "create_table": True}))
    assert result["rows_written"] == 5
    assert FakeRepo.created == 1
    assert FakeRepo.records[-1]["is_trading_day"] is True
