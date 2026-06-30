from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.jobs import market_sentiment_chain_refresh
from cynexus_data.pipelines.msi import (
    build_limit_emotion,
    build_ma_count,
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
        return {"600000": {"market": "SH", "is_st": True, "limit_up_threshold": 0.05}}


class FakeMsiRepo:
    saved = None

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        pass

    def save_limit_emotion(self, payload):
        FakeMsiRepo.saved = payload
        return 1

    def save_style_risk(self, payload):
        FakeMsiRepo.saved = payload
        return 1

    def save_risk_appetite(self, payload):
        FakeMsiRepo.saved = payload
        return 1

    def save_ma_count(self, payload):
        FakeMsiRepo.saved = payload
        return 1


class FakeMarketLimitRepo:
    saved = None
    summary = None

    def __init__(self, client) -> None:
        self.client = client

    def create_table(self) -> None:
        pass

    def replace_stock_snapshots(self, *, trade_date, rows, source="clickhouse.daily_raw"):
        FakeMarketLimitRepo.saved = {"trade_date": trade_date, "rows": rows, "source": source}
        return len(rows)

    def upsert_daily_snapshot(self, *, payload, stock_rows, source="clickhouse.daily_raw"):
        FakeMarketLimitRepo.summary = {"payload": payload, "stock_rows": stock_rows, "source": source}
        return 1


def test_build_limit_emotion_uses_stock_meta_threshold(monkeypatch) -> None:
    monkeypatch.setattr(build_limit_emotion, "StockBasicRepository", FakeStockRepo)
    monkeypatch.setattr(build_limit_emotion, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_limit_emotion, "MarketLimitRepository", FakeMarketLimitRepo)
    monkeypatch.setattr(build_limit_emotion, "get_previous_table_trade_date", lambda client, trade_date, table="daily_raw": None)
    monkeypatch.setattr(
        build_limit_emotion,
        "read_limit_emotion_ohlcv_rows",
        lambda client, trade_date, table="daily_raw": [{"market": "SH", "code": "600000", "tdxCode": "sh600000", "open": 10, "high": 10.5, "low": 9.8, "close": 10.5, "preClose": 10}],
    )
    monkeypatch.setattr(build_limit_emotion, "read_limit_emotion_history_ohlcv_rows", lambda client, trade_date, codes, tdx_codes=None, lookback_days=120, table="daily_raw": [])
    FakeMsiRepo.saved = None
    FakeMarketLimitRepo.saved = None
    FakeMarketLimitRepo.summary = None

    result = build_limit_emotion.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_limit_emotion", dry_run=False, params={"trade_date": "2026-06-26"}))

    assert result["rows_written"] == 1
    assert result["serving_rows_written"] == 1
    assert result["stock_rows_written"] == 1
    assert result["payload"]["limitUpCount"] == 1
    assert FakeMsiRepo.saved["limitUpCount"] == 1
    assert FakeMarketLimitRepo.summary["payload"]["limitUpCount"] == 1
    assert FakeMarketLimitRepo.saved["rows"][0]["category"] == "limit_up"


def test_build_limit_emotion_auto_previous_trade_date(monkeypatch) -> None:
    monkeypatch.setattr(build_limit_emotion, "StockBasicRepository", FakeStockRepo)
    monkeypatch.setattr(build_limit_emotion, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_limit_emotion, "MarketLimitRepository", FakeMarketLimitRepo)
    monkeypatch.setattr(build_limit_emotion, "get_previous_table_trade_date", lambda client, trade_date, table="daily_raw": __import__("datetime").date(2026, 6, 26))

    def fake_ohlcv(client, trade_date, table="daily_raw"):
        if str(trade_date) == "2026-06-29":
            return [
                {"market": "SH", "code": "600000", "tdxCode": "sh600000", "open": 10.6, "high": 11.1, "low": 10.5, "close": 10.92, "preClose": 10.5},
                {"market": "SZ", "code": "000001", "tdxCode": "sz000001", "open": 9.8, "high": 10.0, "low": 9.7, "close": 9.9, "preClose": 10.0},
            ]
        return [
            {"market": "SH", "code": "600000", "tdxCode": "sh600000", "open": 10, "high": 10.5, "low": 9.8, "close": 10.5, "preClose": 10},
            {"market": "SZ", "code": "000001", "tdxCode": "sz000001", "open": 10, "high": 10.2, "low": 9.8, "close": 10.0, "preClose": 10},
        ]

    monkeypatch.setattr(build_limit_emotion, "read_limit_emotion_ohlcv_rows", fake_ohlcv)
    monkeypatch.setattr(build_limit_emotion, "read_limit_emotion_history_ohlcv_rows", lambda client, trade_date, codes, tdx_codes=None, lookback_days=120, table="daily_raw": [])
    FakeMsiRepo.saved = None

    result = build_limit_emotion.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_limit_emotion", dry_run=False, params={"trade_date": "2026-06-29"}))

    assert result["payload"]["prevTradeDate"] == "2026-06-26"
    assert result["payload"]["yesterdayLimitUpCount"] == 1
    assert result["payload"]["yesterdayLimitUpAvgReturn"] == 4.0
    assert FakeMsiRepo.saved["yesterdayLimitUpAvgReturn"] == 4.0


def test_build_limit_emotion_applies_history_percentile_scores(monkeypatch) -> None:
    class FakePercentileRepo:
        def __init__(self, client) -> None:
            self.client = client

        def percentile_score(self, table, column, target_date, current_value):
            return {
                "limit_up_down_ratio": 80.0,
                "break_board_rate": 20.0,
                "yesterday_limit_up_avg_return": 70.0,
                "composite_score": 66.0,
            }[column]

    monkeypatch.setattr(build_limit_emotion, "StockBasicRepository", FakeStockRepo)
    monkeypatch.setattr(build_limit_emotion, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_limit_emotion, "MarketLimitRepository", FakeMarketLimitRepo)
    monkeypatch.setattr(build_limit_emotion, "PercentileRepository", FakePercentileRepo)
    monkeypatch.setattr(build_limit_emotion, "get_previous_table_trade_date", lambda client, trade_date, table="daily_raw": __import__("datetime").date(2026, 6, 25))
    monkeypatch.setattr(build_limit_emotion, "read_limit_emotion_history_ohlcv_rows", lambda client, trade_date, codes, tdx_codes=None, lookback_days=120, table="daily_raw": [])
    monkeypatch.setattr(
        build_limit_emotion,
        "read_limit_emotion_ohlcv_rows",
        lambda client, trade_date, table="daily_raw": [{"market": "SH", "code": "600000", "tdxCode": "sh600000", "open": 10, "high": 11, "low": 10, "close": 11, "preClose": 10}],
    )
    FakeMsiRepo.saved = None

    result = build_limit_emotion.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_limit_emotion", dry_run=False, params={"trade_date": "2026-06-26"}))

    assert result["payload"]["components"]["upDownScore"] == 80.0
    assert result["payload"]["components"]["breakBoardScore"] == 80.0
    assert result["payload"]["components"]["yesterdayReturnScore"] == 70.0
    assert result["payload"]["compositeScore"] == 66.0
    assert FakeMsiRepo.saved["compositeScore"] == 66.0


def test_build_ma_count_applies_history_percentile_scores(monkeypatch) -> None:
    class FakePercentileRepo:
        def __init__(self, client) -> None:
            self.client = client

        def percentile_score(self, table, column, target_date, current_value):
            return {"new_high_252d_pct": 12.5, "breadth_raw": 88.8}[column]

    rows = [
        {"close": 10, "ma20": 9, "ma60": 8, "close_1d_ago": 9, "close_5d_ago": 8, "close_59d_ago": 7, "min_60d": 7, "close_251d_ago": 6, "max_252d": 10},
    ]
    monkeypatch.setattr(build_ma_count, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_ma_count, "PercentileRepository", FakePercentileRepo)
    FakeMsiRepo.saved = None

    result = build_ma_count.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_ma_count", dry_run=False, params={"trade_date": "2026-06-26", "rows": json.dumps(rows)}))

    assert result["rows_written"] == 1
    assert FakeMsiRepo.saved["priceStrengthScore"] == 12.5
    assert FakeMsiRepo.saved["breadthScore"] == 88.8


def test_market_sentiment_chain_forces_final_msi_require_full(monkeypatch) -> None:
    captured = {}

    class FakeJobStore:
        def mark_progress(self, run_id, patch):
            pass

    def fake_step(context, request):
        captured.update(request.params)
        return {"pipeline": "msi.build_msi", "rows_read": 9, "rows_written": 1}

    monkeypatch.setattr(market_sentiment_chain_refresh, "CHAIN_STEPS", [("market_sentiment_index_refresh", fake_step)])

    result = market_sentiment_chain_refresh.run(
        PipelineContext(job_store=FakeJobStore()),
        JobRequest(job_key="market_sentiment_chain_refresh", dry_run=False, params={"trade_date": "2026-06-26"}),
        run_id="run-1",
    )

    assert result.success is True
    assert captured["force"] is True
    assert captured["require_full"] is True


def test_build_style_risk_defaults_to_raw_index_codes(monkeypatch) -> None:
    calls = []

    def fake_read_close_series(client, code, trade_date, table="daily_qfq", limit=260):
        calls.append({"code": code, "table": table, "limit": limit})
        return [{"trade_date": "2026-06-25", "close": 100}, {"trade_date": "2026-06-26", "close": 110}]

    class FakePercentileRepo:
        def __init__(self, client) -> None:
            self.client = client

        def percentile_score(self, table, column, target_date, current_value):
            return 66.6

    monkeypatch.setattr(build_style_risk, "read_close_series", fake_read_close_series)
    monkeypatch.setattr(build_style_risk, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_style_risk, "PercentileRepository", FakePercentileRepo)
    FakeMsiRepo.saved = None

    result = build_style_risk.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_style_risk", dry_run=False, params={"trade_date": "2026-06-26"}))

    assert result["rows_written"] == 1
    assert calls == [
        {"code": "000300", "table": "daily_raw", "limit": 6},
        {"code": "000852", "table": "daily_raw", "limit": 6},
    ]
    assert FakeMsiRepo.saved["score"] == 66.6
    assert FakeMsiRepo.saved["source"] == "clickhouse.daily_raw"


def test_build_risk_appetite_defaults_to_raw_index_and_etf_codes(monkeypatch) -> None:
    calls = []

    def fake_read_close_series(client, code, trade_date, table="daily_qfq", limit=260):
        calls.append({"code": code, "table": table, "limit": limit})
        return [{"trade_date": "2026-06-25", "close": 100}, {"trade_date": "2026-06-26", "close": 110}]

    class FakePercentileRepo:
        def __init__(self, client) -> None:
            self.client = client

        def percentile_score(self, table, column, target_date, current_value):
            return 61.2

    monkeypatch.setattr(build_risk_appetite, "read_close_series", fake_read_close_series)
    monkeypatch.setattr(build_risk_appetite, "MsiRepository", FakeMsiRepo)
    monkeypatch.setattr(build_risk_appetite, "PercentileRepository", FakePercentileRepo)
    FakeMsiRepo.saved = None

    result = build_risk_appetite.run(PipelineContext(hooks=FakeHooks()), JobRequest(job_key="build_risk_appetite", dry_run=False, params={"trade_date": "2026-06-26"}))

    assert result["rows_written"] == 1
    assert calls == [
        {"code": "000300", "table": "daily_raw", "limit": 21},
        {"code": "511010", "table": "daily_raw", "limit": 21},
        {"code": "511090", "table": "daily_raw", "limit": 21},
    ]
    assert FakeMsiRepo.saved["score"] == 61.2
    assert FakeMsiRepo.saved["source"] == "clickhouse.daily_raw"
