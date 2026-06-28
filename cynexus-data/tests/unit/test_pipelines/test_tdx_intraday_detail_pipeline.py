from __future__ import annotations

from datetime import datetime

import pandas as pd

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline import sync_intraday_detail
from cynexus_data.runtime.context import PipelineContext


class FakeClient:
    pass


class FakeTdxHook:
    def get_intraday_detail_client(self, timeout=20.0):
        return FakeClient()


class FakeClickHouseHook:
    def get_config(self):
        return object()


class FakeHooks:
    tdx = FakeTdxHook()
    clickhouse = FakeClickHouseHook()


class FakeSink:
    created = 0
    auction_inserted = 0
    timeshare_inserted = 0

    def __init__(self, config):
        self.config = config

    def create_tables(self):
        FakeSink.created += 1

    def insert_auction_points(self, frame: pd.DataFrame, batch_size=50_000):
        FakeSink.auction_inserted += len(frame)
        return len(frame)

    def insert_timeshare(self, frame: pd.DataFrame, batch_size=50_000):
        FakeSink.timeshare_inserted += len(frame)
        return len(frame)


def _auction_payload(client, code, trade_date, target_type=None):
    return {
        "points": [
            {
                "market": "SH",
                "symbol": code,
                "code": code,
                "tdx_code": f"sh{code}",
                "trade_date": pd.to_datetime(trade_date).date(),
                "trade_time": datetime(2026, 6, 26, 9, 25),
                "time_label": "09:25:00",
                "phase": "opening",
                "price": 10.0,
                "matched_volume": 100.0,
                "matched_amount": 1000.0,
                "unmatched_buy_volume": None,
                "unmatched_sell_volume": None,
                "source": "eltdx",
                "raw_payload": {},
                "fetched_at": datetime(2026, 6, 26, 9, 26),
                "ingested_at": datetime(2026, 6, 26, 9, 26),
            }
        ]
    }


def _timeshare_rows(client, code, trade_date, target_type=None):
    return [
        {
            "market": "SH",
            "symbol": code,
            "code": code,
            "tdx_code": f"sh{code}",
            "trade_date": pd.to_datetime(trade_date).date(),
            "trade_time": datetime(2026, 6, 26, 9, 30),
            "time_label": "09:30",
            "price": 10.1,
            "avg_price": 10.0,
            "volume": 100.0,
            "turnover": 1010.0,
            "turnover_rate": None,
            "volume_ratio": None,
            "source": "eltdx",
            "raw_payload": {},
            "fetched_at": datetime(2026, 6, 26, 9, 31),
            "ingested_at": datetime(2026, 6, 26, 9, 31),
        }
    ]


def test_sync_intraday_detail_dry_run_skips_external_access() -> None:
    result = sync_intraday_detail.run(
        PipelineContext(),
        JobRequest(job_key="sync_tdx_intraday_detail", dry_run=True, params={"codes": "600000", "trade_date": "2026-06-26"}),
    )

    assert result["planned_codes"] == 1
    assert result["rows_written"] == 0


def test_sync_intraday_detail_writes_auction_and_timeshare(monkeypatch) -> None:
    FakeSink.created = 0
    FakeSink.auction_inserted = 0
    FakeSink.timeshare_inserted = 0
    monkeypatch.setattr(sync_intraday_detail, "ClickHouseIntradayDetailSink", FakeSink)
    monkeypatch.setattr(sync_intraday_detail, "fetch_stock_auction", _auction_payload)
    monkeypatch.setattr(sync_intraday_detail, "fetch_stock_history_timeshare", _timeshare_rows)

    result = sync_intraday_detail.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_tdx_intraday_detail",
            dry_run=False,
            params={"codes": ["600000"], "trade_date": "2026-06-26", "create_table": True},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert FakeSink.created == 1
    assert FakeSink.auction_inserted == 1
    assert FakeSink.timeshare_inserted == 1


def test_sync_intraday_detail_resume_skips_completed_checkpoint(monkeypatch) -> None:
    FakeSink.auction_inserted = 0
    FakeSink.timeshare_inserted = 0
    monkeypatch.setattr(sync_intraday_detail, "ClickHouseIntradayDetailSink", FakeSink)
    monkeypatch.setattr(sync_intraday_detail, "fetch_stock_auction", _auction_payload)
    monkeypatch.setattr(sync_intraday_detail, "fetch_stock_history_timeshare", _timeshare_rows)
    context = PipelineContext(hooks=FakeHooks())
    context.checkpoints.put("kline.sync_intraday_detail:auction:600000:2026-06-26", "sync_tdx_intraday_detail", {"status": "completed"})

    result = sync_intraday_detail.run(
        context,
        JobRequest(
            job_key="sync_tdx_intraday_detail",
            dry_run=False,
            params={"codes": ["600000"], "trade_date": "2026-06-26", "datasets": "auction", "resume": True},
        ),
    )

    assert result["skipped_checkpoints"] == 1
    assert result["rows_written"] == 0
    assert FakeSink.auction_inserted == 0
