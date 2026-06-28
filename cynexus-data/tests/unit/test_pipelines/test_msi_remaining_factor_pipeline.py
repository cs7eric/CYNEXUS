from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi import (
    build_limit_emotion,
    build_risk_appetite,
    build_style_risk,
    build_turnover_activity,
    build_volatility_sentiment,
)
from cynexus_data.runtime.context import PipelineContext


def test_remaining_factor_pipelines_dry_run() -> None:
    context = PipelineContext()
    turnover = build_turnover_activity.run(
        context,
        JobRequest(job_key="build_turnover_activity", dry_run=True, params={"trade_date": "2026-06-26", "rows": json.dumps([{"trade_date": "2026-06-24", "total_amount": 100}, {"trade_date": "2026-06-25", "total_amount": 200}, {"trade_date": "2026-06-26", "total_amount": 300}])}),
    )
    assert turnover["payload"]["ratio"] == 2
    vol_rows = [{"trade_date": f"2026-01-{day:02d}", "close": 100 + day} for day in range(1, 29)]
    vol = build_volatility_sentiment.run(context, JobRequest(job_key="build_volatility_sentiment", dry_run=True, params={"trade_date": "2026-01-28", "window": 5, "lookback": 20, "rows": json.dumps(vol_rows)}))
    assert vol["payload"] is not None
    hs = json.dumps([{"trade_date": "2026-06-24", "close": 100}, {"trade_date": "2026-06-26", "close": 110}])
    risk = build_risk_appetite.run(context, JobRequest(job_key="build_risk_appetite", dry_run=True, params={"trade_date": "2026-06-26", "hs300_rows": hs, "treasury_511010_rows": hs, "treasury_511090_rows": hs}))
    assert risk["payload"]["score"] == 50
    style = build_style_risk.run(context, JobRequest(job_key="build_style_risk", dry_run=True, params={"trade_date": "2026-06-26", "hs300_rows": hs, "csi1000_rows": hs}))
    assert style["payload"]["spread"] == 0
    limit = build_limit_emotion.run(context, JobRequest(job_key="build_limit_emotion", dry_run=True, params={"trade_date": "2026-06-26", "rows": json.dumps([{"code": "000001", "isLimitUp": True, "isTouchedLimitUp": True}])}))
    assert limit["payload"]["limitUpCount"] == 1


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


class FakeStockRepo:
    def __init__(self, client) -> None:
        self.client = client

    def list_limit_meta(self, active_only=True):
        return {"600000": {"is_st": True, "limit_up_threshold": 0.05}}


class FakeMsiRepo:
    saved = None

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        pass

    def save_limit_emotion(self, payload):
        FakeMsiRepo.saved = payload
        return 1


class FakeMarketLimitRepo:
    saved = None

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        pass

    def replace_stock_snapshots(self, *, trade_date, rows, source="clickhouse.daily_raw"):
        FakeMarketLimitRepo.saved = {"trade_date": trade_date, "rows": rows, "source": source}
        return len(rows)


def test_build_limit_emotion_uses_stock_meta_threshold(monkeypatch) -> None:
    monkeypatch.setattr(build_limit_emotion, "StockBasicRepository", FakeStockRepo)
    monkeypatch.setattr(build_limit_emotion, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_limit_emotion, "MarketLimitRepository", FakeMarketLimitRepo)
    monkeypatch.setattr(
        build_limit_emotion,
        "read_limit_emotion_ohlcv_rows",
        lambda client, trade_date, table="daily_raw": [{"code": "600000", "open": 10, "high": 10.5, "low": 9.8, "close": 10.5, "preClose": 10}],
    )
    monkeypatch.setattr(build_limit_emotion, "read_limit_emotion_history_ohlcv_rows", lambda client, trade_date, codes, lookback_days=120, table="daily_raw": [])
    FakeMsiRepo.saved = None
    FakeMarketLimitRepo.saved = None

    result = build_limit_emotion.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_limit_emotion", dry_run=False, params={"trade_date": "2026-06-26"}))

    assert result["rows_written"] == 1
    assert result["stock_rows_written"] == 1
    assert result["payload"]["limitUpCount"] == 1
    assert FakeMsiRepo.saved["limitUpCount"] == 1
    assert FakeMarketLimitRepo.saved["rows"][0]["category"] == "limit_up"
