from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market import sync_market_fund_flow
from cynexus_data.runtime.context import PipelineContext


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def upsert_fields(self, fields, *, source, clear_manual_override, extra):
        FakeRepo.calls.append({"fields": fields, "source": source, "clear_manual_override": clear_manual_override, "extra": extra})

    def record_source_failure(self, *, source, error, attempted_at):
        FakeRepo.calls.append({"source": source, "error": error, "attempted_at": attempted_at})
        return "2026-07-15"


class FakeEastMoneyClient:
    def fetch_market_fund_flow(self, *, limit):
        assert limit == 120
        return [
            {
                "trade_date": "2026-07-16",
                "main_net_inflow": -83885834240,
                "super_large_net_inflow": -42946482176,
                "large_net_inflow": -40939352064,
                "medium_net_inflow": 16797376512,
                "small_net_inflow": 67088457728,
                "main_net_inflow_ratio": -3.49,
                "super_large_net_inflow_ratio": -1.79,
                "large_net_inflow_ratio": -1.70,
                "medium_net_inflow_ratio": 0.70,
                "small_net_inflow_ratio": 2.79,
            }
        ]


class FakeEastMoneyHook:
    def get_client(self):
        return FakeEastMoneyClient()


class FailingEastMoneyClient:
    last_market_fund_flow_metadata = {
        "fallback_used": True,
        "source_mode": "failed",
        "stale": True,
        "error": "HTTP failed; browser failed",
    }

    def fetch_market_fund_flow(self, *, limit):
        raise RuntimeError("HTTP failed; browser failed")


class FailingEastMoneyHook:
    def get_client(self):
        return FailingEastMoneyClient()


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeHooks:
    eastmoney = FakeEastMoneyHook()
    postgres = FakePostgresHook()


class FailingHooks:
    eastmoney = FailingEastMoneyHook()
    postgres = FakePostgresHook()


def test_sync_market_fund_flow_dry_run() -> None:
    result = sync_market_fund_flow.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="sync_market_fund_flow", dry_run=True, params={}),
    )

    assert result["rows_read"] == 1
    assert result["rows_written"] == 0
    assert result["trade_date_end"] == date(2026, 7, 16).isoformat()


def test_sync_market_fund_flow_writes_history(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_market_fund_flow, "MarketOverviewRepository", FakeRepo)

    result = sync_market_fund_flow.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(job_key="sync_market_fund_flow", dry_run=False, params={"create_table": True}),
    )

    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0]["source"] == "eastmoney.market_fund_flow"
    assert FakeRepo.calls[0]["clear_manual_override"] is True
    assert FakeRepo.calls[0]["fields"]["main_net_inflow"] == -838.8583


def test_sync_market_fund_flow_failure_preserves_values_and_returns_alert(monkeypatch) -> None:
    FakeRepo.calls = []
    monkeypatch.setattr(sync_market_fund_flow, "MarketOverviewRepository", FakeRepo)

    result = sync_market_fund_flow.run(
        PipelineContext(hooks=FailingHooks()),
        JobRequest(job_key="sync_market_fund_flow", dry_run=False, params={}),
    )

    assert result["success"] is False
    assert result["status"] == "failed"
    assert result["stale"] is True
    assert result["rows_written"] == 0
    assert result["last_successful_trade_date"] == "2026-07-15"
    assert FakeRepo.calls[0]["source"] == "eastmoney.market_fund_flow"
    assert "error" in FakeRepo.calls[0]
