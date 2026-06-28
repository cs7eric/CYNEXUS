from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market_overview import build_market_overview
from cynexus_data.runtime.context import PipelineContext


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def upsert_fields(self, fields, *, source, extra=None):
        FakeRepo.calls.append({"fields": fields, "source": source, "extra": extra})
        return {**fields, "source": source}


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeAkshareClient:
    def fetch_market_overview(self):
        return {
            "totalAmount": 123.45,
            "risingCount": 3000,
            "mainNetInflow": 6.7,
            "largeNetInflow": 2.2,
        }


class FakeAkshareHook:
    def get_client(self):
        return FakeAkshareClient()


class FakeTdxClient:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return None

    def get_a_share_codes_all(self):
        return ["sh600000"]

    def get_quote(self, codes):
        return [{"amount": 100_000_000, "last_price": 11.0, "pre_close_price": 10.0}]


class FakeTdxHook:
    def get_market_overview_client(self, timeout=20.0):
        return FakeTdxClient()


class FakeEastMoneyClient:
    def fetch_market_breadth(self, *, page_size=80, max_pages=70, page_sleep=0.0):
        return {
            "totalAmount": 456.78,
            "totalVolume": 123.4,
            "risingCount": 2000,
            "fallingCount": 1500,
            "flatCount": 100,
            "limitUpCount": 66,
            "limitDownCount": 12,
            "stockCount": 3600,
        }


class FakeEastMoneyHook:
    def get_client(self):
        return FakeEastMoneyClient()


class FakeHooks:
    postgres = FakePostgresHook()
    akshare = FakeAkshareHook()
    tdx = FakeTdxHook()
    eastmoney = FakeEastMoneyHook()


def test_build_market_overview_dry_run_fields() -> None:
    fields = {"tradingDate": "2026-06-26", "totalAmount": 1234.5, "risingCount": 3000}

    result = build_market_overview.run(
        PipelineContext(),
        JobRequest(job_key="build_market_overview", dry_run=True, params={"source": "eltdx", "fields": json.dumps(fields)}),
    )

    assert result["trade_date"] == "2026-06-26"
    assert result["source"] == "eltdx"
    assert result["fields"]["total_amount"] == 1234.5
    assert result["fields"]["rising_count"] == 3000
    assert result["rows_written"] == 0


def test_build_market_overview_fetches_akshare_live(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(build_market_overview, "MarketOverviewRepository", FakeRepo)

    result = build_market_overview.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="build_market_overview",
            dry_run=False,
            params={"source": "akshare", "trade_date": "2026-06-26", "create_table": True},
        ),
    )

    assert result["fetch_live"] is True
    assert result["rows_written"] == 1
    assert FakeRepo.created == 1
    call = FakeRepo.calls[0]
    assert call["source"] == "akshare"
    assert call["fields"]["trade_date"].isoformat() == "2026-06-26"
    assert call["fields"]["total_amount"] == 123.45
    assert call["fields"]["main_net_inflow"] == 6.7


def test_build_market_overview_fetches_eltdx_live(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(build_market_overview, "MarketOverviewRepository", FakeRepo)

    result = build_market_overview.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="build_market_overview",
            dry_run=False,
            params={"source": "eltdx", "trade_date": "2026-06-26", "create_table": True, "eltdx_timeout": 1},
        ),
    )

    assert result["fetch_live"] is True
    assert result["rows_written"] == 1
    call = FakeRepo.calls[0]
    assert call["source"] == "eltdx"
    assert call["fields"]["trade_date"].isoformat() == "2026-06-26"
    assert call["fields"]["total_amount"] == 1.0
    assert call["fields"]["rising_count"] == 1


def test_build_market_overview_fetches_eastmoney_live(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(build_market_overview, "MarketOverviewRepository", FakeRepo)

    result = build_market_overview.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="build_market_overview",
            dry_run=False,
            params={"source": "eastmoney", "trade_date": "2026-06-26", "create_table": True, "max_pages": 1},
        ),
    )

    assert result["fetch_live"] is True
    assert result["rows_written"] == 1
    call = FakeRepo.calls[0]
    assert call["source"] == "eastmoney"
    assert call["fields"]["trade_date"].isoformat() == "2026-06-26"
    assert call["fields"]["total_amount"] == 456.78
    assert call["fields"]["limit_up_count"] == 66
