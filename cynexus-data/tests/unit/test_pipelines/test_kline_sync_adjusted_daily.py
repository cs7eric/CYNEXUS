from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

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
    calls = []

    def __init__(self, config, adjust):
        self.adjust = adjust

    def create_table(self):
        FakeSink.created += 1

    def replace_daily(self, frame: pd.DataFrame, batch_size=100_000, *, replace_scope="keys"):
        FakeSink.inserted += len(frame)
        FakeSink.calls.append({"adjust": self.adjust, "rows": len(frame), "replace_scope": replace_scope})
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
    FakeSink.calls = []
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
    assert FakeSink.calls == [{"adjust": "qfq", "rows": 1, "replace_scope": "keys"}]


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
    FakeSink.calls = []
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
    FakeSink.calls = []
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


def test_sync_adjusted_daily_batches_date_replace(monkeypatch) -> None:
    FakeSink.created = 0
    FakeSink.inserted = 0
    FakeSink.calls = []
    monkeypatch.setattr(sync_adjusted_daily, "create_eltdx_client", lambda: FakeClient())
    monkeypatch.setattr(sync_adjusted_daily, "ClickHouseAdjustedDailyKlineSink", FakeSink)
    context = PipelineContext(hooks=FakeHooks())

    result = sync_adjusted_daily.run(
        context,
        JobRequest(
            job_key="sync_adjusted_kline_daily",
            dry_run=False,
            params={"codes": ["000001", "000002"], "adjust": "qfq", "flush_rows": 100_000, "replace_scope": "date", "resume": True},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert result["batch_results"] == [{"adjust": "qfq", "codes": 2, "rows_read": 2, "rows_written": 2}]
    assert FakeSink.calls == [{"adjust": "qfq", "rows": 2, "replace_scope": "date"}]
    assert context.checkpoints.get("kline.sync_adjusted_daily:qfq:000001::")["status"] == "completed"
    assert context.checkpoints.get("kline.sync_adjusted_daily:qfq:000002::")["status"] == "completed"


def test_sync_adjusted_daily_parallel_fetch_success(monkeypatch) -> None:
    FakeSink.created = 0
    FakeSink.inserted = 0
    FakeSink.calls = []
    monkeypatch.setattr(sync_adjusted_daily, "create_eltdx_client", lambda: FakeClient())
    monkeypatch.setattr(sync_adjusted_daily, "ClickHouseAdjustedDailyKlineSink", FakeSink)

    result = sync_adjusted_daily.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_adjusted_kline_daily",
            dry_run=False,
            params={"codes": ["000001", "000002"], "adjust": "qfq", "workers": 2},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert not any("error" in item for item in result["code_results"])


def test_sync_adjusted_daily_reports_parent_progress(monkeypatch) -> None:
    FakeSink.created = 0
    FakeSink.inserted = 0
    FakeSink.calls = []
    monkeypatch.setattr(sync_adjusted_daily, "create_eltdx_client", lambda: FakeClient())
    monkeypatch.setattr(sync_adjusted_daily, "ClickHouseAdjustedDailyKlineSink", FakeSink)
    context = PipelineContext(hooks=FakeHooks())
    context.job_store.mark_started(
        "parent_run",
        JobRequest(job_key="market_sentiment_chain_refresh", dry_run=False, params={}),
        datetime.now(timezone.utc),
    )

    sync_adjusted_daily.run(
        context,
        JobRequest(
            job_key="sync_adjusted_kline_daily",
            dry_run=False,
            params={
                "codes": ["000001", "000002"],
                "adjust": "qfq",
                "progress_run_id": "parent_run",
                "progress_step_code": "qfq_reconciliation_refresh",
                "progress_base_metadata": {"lastCompletedSteps": ["tdx_hsjday_download", "initial_backfill_refresh"]},
                "progress_parent_rows_read": 7,
                "progress_parent_rows_written": 7,
                "progress_update_every_items": 1,
            },
        ),
    )

    record = context.job_store.get("parent_run")
    progress = record["metadata"]["currentStepProgress"]
    assert record["rows_read"] == 9
    assert record["rows_written"] == 9
    assert progress["step"] == "qfq_reconciliation_refresh"
    assert progress["unitLabel"] == "标的"
    assert progress["processedItems"] == 2
    assert progress["totalItems"] == 2
    assert progress["rowsRead"] == 2
    assert progress["rowsWritten"] == 2
