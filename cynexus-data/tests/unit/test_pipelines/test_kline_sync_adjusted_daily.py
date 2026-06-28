from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

import pandas as pd

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline import sync_adjusted_daily
from cynexus_data.runtime.context import PipelineContext


@dataclass
class FakeBar:
    time: datetime
    open: float = 10
    high: float = 11
    low: float = 9
    close: float = 10.5
    volume_lots: int = 100
    amount: float = 1000.0


class FakeClient:
    def get_adjusted_kline_all(self, period, code, adjust, page_size=800, max_pages=200):
        return [FakeBar(datetime(2026, 6, 26))]


class FakeSink:
    created = 0
    inserted = 0

    def __init__(self, config, adjust):
        self.adjust = adjust

    def create_table(self):
        FakeSink.created += 1

    def replace_daily(self, frame: pd.DataFrame, batch_size=100_000):
        FakeSink.inserted += len(frame)
        return len(frame)


class FakeClickHouseHook:
    def get_config(self):
        return object()


class FakeHooks:
    clickhouse = FakeClickHouseHook()


def test_sync_adjusted_daily_dry_run_skips_external_access() -> None:
    result = sync_adjusted_daily.run(PipelineContext(), JobRequest(job_key="sync_adjusted_kline_daily", dry_run=True, params={"codes": "000001", "adjust": "qfq"}))

    assert result["planned_codes"] == 1
    assert result["rows_written"] == 0


def test_sync_adjusted_daily_uses_client_and_sink(monkeypatch) -> None:
    FakeSink.created = 0
    FakeSink.inserted = 0
    monkeypatch.setattr(sync_adjusted_daily, "create_eltdx_client", lambda: FakeClient())
    monkeypatch.setattr(sync_adjusted_daily, "ClickHouseAdjustedDailyKlineSink", FakeSink)

    result = sync_adjusted_daily.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="sync_adjusted_kline_daily", dry_run=False, params={"codes": ["000001"], "adjust": "qfq", "create_table": True}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 1
    assert FakeSink.created == 1
    assert FakeSink.inserted == 1


def test_sync_adjusted_daily_loads_codes_from_postgres(monkeypatch) -> None:
    class FakeRepo:
        def __init__(self, client):
            self.client = client

        def list_codes(self, active_only=True):
            return ["000001"]

    class FakePostgresHook:
        def get_client(self):
            return object()

    class Hooks(FakeHooks):
        postgres = FakePostgresHook()

    FakeSink.created = 0
    FakeSink.inserted = 0
    monkeypatch.setattr(sync_adjusted_daily, "create_eltdx_client", lambda: FakeClient())
    monkeypatch.setattr(sync_adjusted_daily, "ClickHouseAdjustedDailyKlineSink", FakeSink)
    monkeypatch.setattr(sync_adjusted_daily, "StockBasicRepository", FakeRepo)

    result = sync_adjusted_daily.run(
        PipelineContext(hooks=Hooks()),
        JobRequest(job_key="sync_adjusted_kline_daily", dry_run=False, params={"adjust": "qfq"}),
    )

    assert result["planned_codes"] == 1
    assert result["code_source"] == "postgres.sec_stock_basic"
    assert result["rows_written"] == 1


def test_sync_adjusted_daily_resume_skips_completed_checkpoint(monkeypatch) -> None:
    FakeSink.created = 0
    FakeSink.inserted = 0
    monkeypatch.setattr(sync_adjusted_daily, "create_eltdx_client", lambda: FakeClient())
    monkeypatch.setattr(sync_adjusted_daily, "ClickHouseAdjustedDailyKlineSink", FakeSink)
    context = PipelineContext(hooks=FakeHooks())
    context.checkpoints.put("kline.sync_adjusted_daily:qfq:000001::", "sync_adjusted_kline_daily", {"status": "completed"})

    result = sync_adjusted_daily.run(
        context,
        JobRequest(job_key="sync_adjusted_kline_daily", dry_run=False, params={"codes": ["000001"], "adjust": "qfq", "resume": True}),
    )

    assert result["skipped_checkpoints"] == 1
    assert result["rows_written"] == 0
    assert FakeSink.inserted == 0
