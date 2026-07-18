from __future__ import annotations

from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.jobs import stock_fund_flow_daily_qc as qc
from cynexus_data.runtime.context import PipelineContext


class Hooks:
    class Postgres:
        def get_client(self):
            return object()

    postgres = Postgres()


class StockRepo:
    def __init__(self, client):
        pass

    def list_codes(self, active_only=True):
        return [f"600{i:03d}" for i in range(100)]


class LimitRepo:
    def __init__(self, client):
        pass

    def list_historical_limit_up_codes(self):
        return ["600001", "600003", "600003"]


class QcRepo:
    rows = []

    def __init__(self, client):
        pass

    def create_table(self):
        pass

    def upsert_many(self, rows):
        self.rows.extend(rows)
        return len(rows)


def test_sample_codes_randomizes_full_market_sample(monkeypatch):
    monkeypatch.setattr(qc, "StockBasicRepository", StockRepo)
    context = PipelineContext(hooks=Hooks())

    sample = qc._sample_codes(context, {}, sample_date=date(2026, 7, 17), sample_size=50)

    assert len(sample) == 50
    assert sample != [f"600{i:03d}" for i in range(50)]


def test_sample_codes_supports_limit_up_and_remaining_scopes(monkeypatch):
    monkeypatch.setattr(qc, "StockBasicRepository", StockRepo)
    monkeypatch.setattr(qc, "MarketLimitRepository", LimitRepo)
    context = PipelineContext(hooks=Hooks())

    assert set(qc._sample_codes(context, {"stock_scope": "limit-up-history", "sample_seed": "unit"}, sample_date=date(2026, 7, 17), sample_size=10)) == {"600001", "600003"}
    remaining = qc._sample_codes(context, {"stock_scope": "remaining-active", "sample_seed": "unit"}, sample_date=date(2026, 7, 17), sample_size=200)

    assert "600001" not in remaining
    assert "600003" not in remaining
    assert len(remaining) == 98


def test_daily_qc_runs_sequential_sample_and_summarizes(monkeypatch):
    QcRepo.rows = []
    monkeypatch.setattr(qc, "FundFlowQualityCheckRepository", QcRepo)
    monkeypatch.setattr(qc, "StockBasicRepository", StockRepo)
    calls = []

    def fake_easy(code, count, timeout):
        calls.append(("easy", code))
        return {"source": "easy_tdx.tdx", "items": [{"trade_date": "2026-07-17", "main_net": 100.0}]}

    def fake_eastmoney(client, code, mode):
        calls.append(("eastmoney", code))
        return {"source": "eastmoney.push2delay", "items": [{"date": "2026-07-17", "main_net": 80.0}]}

    monkeypatch.setattr(qc, "fetch_history_stock_fund_flow_payload", fake_easy)
    monkeypatch.setattr(qc.eastmoney_a_stock, "fund_flow_120d", fake_eastmoney)

    result = qc.run(
        PipelineContext(hooks=Hooks()),
        JobRequest(job_key=qc.JOB_KEY, params={"sample_size": 3, "sample_seed": "unit", "create_table": True}),
        "run-qc",
    )

    assert result.success is True
    assert result.rows_written == 3
    assert len(QcRepo.rows) == 3
    assert [kind for kind, _ in calls] == ["easy", "eastmoney", "easy", "eastmoney", "easy", "eastmoney"]
    assert result.metadata["qcSummary"]["directionMatchRate"] == 1.0
