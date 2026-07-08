from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market_overview import build_market_overview, build_market_overview_detail
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


class FakeDetailRepo:
    created = 0
    detail_calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeDetailRepo.created += 1

    def get(self, trade_date):
        return {
            "trade_date": trade_date,
            "rising_count": 3000,
            "falling_count": 1800,
            "limit_up_count": 70,
            "limit_down_count": 8,
            "stock_count": 5200,
        }

    def list_snapshots(self, *, end_date, limit=260):
        return [
            {
                "trade_date": f"2026-06-{day:02d}",
                "rising_count": 2500 + day,
                "falling_count": 1900,
                "limit_up_count": 40,
                "limit_down_count": 6,
                "stock_count": 5200,
            }
            for day in range(1, 21)
        ]

    def upsert_detail(self, *, trade_date, benchmark_code, source_table, payload, source="python"):
        FakeDetailRepo.detail_calls.append(
            {
                "trade_date": trade_date,
                "benchmark_code": benchmark_code,
                "source_table": source_table,
                "payload": payload,
                "source": source,
            }
        )
        return {
            "trade_date": trade_date,
            "benchmark_code": benchmark_code,
            "source_table": source_table,
            "payload": payload,
            "source": source,
        }


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


class BrokenAkshareClient:
    def fetch_market_overview(self):
        raise ConnectionError("remote disconnected")


class BrokenAkshareHook:
    def get_client(self):
        return BrokenAkshareClient()


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


class FakeTencentClient:
    def fetch_market_overview(self, codes, *, batch_size=500, batch_sleep=0.22):
        return {
            "totalAmount": 88.8,
            "totalVolume": 12.3,
            "risingCount": 10,
            "fallingCount": 5,
            "flatCount": 1,
            "limitUpCount": 2,
            "limitDownCount": 1,
            "stockCount": len(codes),
        }


class FakeTencentHook:
    def get_client(self):
        return FakeTencentClient()


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
    tencent = FakeTencentHook()


class FakeHooksWithBrokenAkshare:
    postgres = FakePostgresHook()
    akshare = BrokenAkshareHook()
    tdx = FakeTdxHook()
    eastmoney = FakeEastMoneyHook()
    tencent = FakeTencentHook()


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


def test_build_market_overview_falls_back_when_akshare_disconnects(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(build_market_overview, "MarketOverviewRepository", FakeRepo)

    result = build_market_overview.run(
        PipelineContext(hooks=FakeHooksWithBrokenAkshare()),
        JobRequest(
            job_key="build_market_overview",
            dry_run=False,
            params={"source": "akshare", "trade_date": "2026-06-26", "fallback_sources": "tencent,eastmoney", "codes": "600000,000001"},
        ),
    )

    assert result["requested_source"] == "akshare"
    assert result["source"] == "tencent"
    assert result["fetch_errors"]
    assert result["rows_written"] == 1
    assert FakeRepo.calls[0]["source"] == "tencent"


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


def test_build_market_overview_fetches_tencent_live_with_codes(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(build_market_overview, "MarketOverviewRepository", FakeRepo)

    result = build_market_overview.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="build_market_overview",
            dry_run=False,
            params={"source": "tencent", "trade_date": "2026-06-26", "codes": "600000,000001", "batch_size": 2},
        ),
    )

    assert result["fetch_live"] is True
    assert result["rows_written"] == 1
    call = FakeRepo.calls[0]
    assert call["source"] == "tencent"
    assert call["fields"]["total_amount"] == 88.8
    assert call["fields"]["stock_count"] == 2


def test_build_market_overview_detail_dry_run_uses_synthetic_payload() -> None:
    result = build_market_overview_detail.run(
        PipelineContext(),
        JobRequest(
            job_key="build_market_overview_detail",
            dry_run=True,
            params={"trade_date": "2026-06-26"},
        ),
    )

    assert result["rows_written"] == 0
    assert result["payload"]["benchmarkCode"] == "999999"
    assert result["payload"]["windowMetrics"]
    assert result["payload"]["sentiment"]["state"] in {"strong", "neutral", "weak"}
    assert "dry_run used synthetic benchmark rows" in result["notes"]


def test_build_market_overview_detail_persists_supplied_rows(monkeypatch) -> None:
    FakeDetailRepo.created = 0
    FakeDetailRepo.detail_calls = []
    monkeypatch.setattr(build_market_overview_detail, "MarketOverviewRepository", FakeDetailRepo)
    rows = _detail_bars(140)

    result = build_market_overview_detail.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="build_market_overview_detail",
            dry_run=False,
            params={
                "trade_date": "2026-06-26",
                "benchmark_code": "000001",
                "source_table": "daily_qfq",
                "create_table": True,
                "benchmark_rows": json.dumps(rows),
                "index_bars_map": json.dumps({"hs300": _detail_bars(140, step=0.22)}),
                "today_breadth": json.dumps({"rising_count": 3000, "falling_count": 1700, "limit_up_count": 66, "limit_down_count": 9, "stock_count": 5200}),
            },
        ),
    )

    assert result["rows_written"] == 1
    assert FakeDetailRepo.created == 1
    call = FakeDetailRepo.detail_calls[0]
    assert call["benchmark_code"] == "000001"
    assert call["source_table"] == "daily_qfq"
    assert call["payload"]["windowMetrics"]
    assert call["payload"]["similarScenario"]["forwardStats"]


def _detail_bars(count: int, *, step: float = 0.2):
    rows = []
    price = 100.0
    for index in range(count):
        price += step + ((index % 5) - 2) * 0.03
        rows.append(
            {
                "trade_date": f"2026-01-{(index % 28) + 1:02d}",
                "open": price - 0.2,
                "high": price + 0.7,
                "low": price - 0.7,
                "close": price,
                "volume": 1000 + index,
                "amount": (1000 + index) * price,
            }
        )
    return rows
