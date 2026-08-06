from __future__ import annotations

from datetime import date

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.margin import sync_margin
from cynexus_data.runtime.context import PipelineContext


class FakeAkshareClient:
    def fetch_latest_margin_trade_date(self, end_date):
        return date(2026, 7, 2)

    def fetch_margin_market_summary(self, trade_date):
        return [{"trade_date": trade_date, "exchange": "SSE", "source": "akshare.stock_margin_sse"}]

    def fetch_margin_stock_detail(self, trade_date):
        return [{"trade_date": trade_date, "exchange": "SSE", "stock_code": "600000", "source": "akshare.stock_margin_detail_sse"}]

    def fetch_margin_account_stats(self, trade_date):
        return [{"trade_date": trade_date, "source": "akshare.stock_margin_account_info"}]


class FakeExchangeClient:
    def fetch_latest_margin_trade_date(self, end_date):
        return date(2026, 7, 3)

    def fetch_margin_market_summary(self, trade_date):
        return [{"trade_date": trade_date, "exchange": "SSE", "source": "exchange.sse.margin_summary"}]

    def fetch_margin_stock_detail(self, trade_date):
        return [{"trade_date": trade_date, "exchange": "SSE", "stock_code": "600000", "source": "exchange.sse.margin_detail"}]

    def fetch_margin_account_stats(self, trade_date):
        return []


class EmptyExchangeClient(FakeExchangeClient):
    def fetch_latest_margin_trade_date(self, end_date):
        return None

    def fetch_margin_market_summary(self, trade_date):
        return []

    def fetch_margin_stock_detail(self, trade_date):
        return []


class FakeAkshareHook:
    def get_client(self):
        return FakeAkshareClient()


class FakeExchangeHook:
    def get_client(self):
        return FakeExchangeClient()


class EmptyExchangeHook:
    def get_client(self):
        return EmptyExchangeClient()


class FakePostgresHook:
    def get_client(self):
        return object()


class FakeEastMoneyHook:
    def get_session(self):
        return object()


class FakeHooks:
    exchange_margin = FakeExchangeHook()
    akshare = FakeAkshareHook()
    postgres = FakePostgresHook()


class FakeHooksWithEmptyExchange:
    exchange_margin = EmptyExchangeHook()
    akshare = FakeAkshareHook()
    postgres = FakePostgresHook()


class FakeHooksWithEastMoney:
    exchange_margin = FakeExchangeHook()
    akshare = FakeAkshareHook()
    postgres = FakePostgresHook()
    eastmoney = FakeEastMoneyHook()


class FakeRepo:
    created = 0
    calls = []

    def __init__(self, client):
        self.client = client

    def create_table(self):
        FakeRepo.created += 1

    def replace_market_summary(self, *, trade_date, rows):
        FakeRepo.calls.append(("summary", trade_date, rows))
        return len(rows)

    def replace_stock_detail(self, *, trade_date, rows):
        FakeRepo.calls.append(("detail", trade_date, rows))
        return len(rows)

    def replace_account_stats(self, *, rows):
        FakeRepo.calls.append(("account", None, rows))
        return len(rows)


def test_sync_margin_dry_run_does_not_access_hooks() -> None:
    result = sync_margin.run(
        PipelineContext(),
        JobRequest(job_key="sync_margin", dry_run=True, params={"trade_date": "2026-07-04"}),
    )

    assert result["trade_date"] == "2026-07-04"
    assert result["rows_written"] == 0
    assert result["providers"] == ["exchange", "akshare"]
    assert result["datasets"] == ["summary", "detail", "account"]


def test_sync_margin_fetches_selected_datasets(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_margin, "MarginRepository", FakeRepo)

    result = sync_margin.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_margin",
            dry_run=False,
            params={"trade_date": "2026-07-04", "datasets": "summary,account", "create_table": True},
        ),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 2
    assert result["summary_rows"] == 1
    assert result["detail_rows"] == 0
    assert result["account_rows"] == 1
    assert result["provider_results"] == {"summary": "exchange", "account": "akshare"}
    assert FakeRepo.created == 1
    assert FakeRepo.calls[0][1] == date(2026, 7, 4)


def test_sync_margin_without_trade_date_uses_latest_available_margin_date(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_margin, "MarginRepository", FakeRepo)

    result = sync_margin.run(
        PipelineContext(hooks=FakeHooks()),
        JobRequest(
            job_key="sync_margin",
            dry_run=False,
            params={"datasets": "summary", "create_table": False},
        ),
    )

    assert result["trade_date"] == "2026-07-03"
    assert result["rows_written"] == 1
    assert result["provider_results"]["summary"] == "exchange"
    assert FakeRepo.calls[0][1] == date(2026, 7, 3)


def test_sync_margin_falls_back_to_akshare_when_exchange_has_no_rows(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_margin, "MarginRepository", FakeRepo)

    result = sync_margin.run(
        PipelineContext(hooks=FakeHooksWithEmptyExchange()),
        JobRequest(
            job_key="sync_margin",
            dry_run=False,
            params={"trade_date": "2026-07-04", "datasets": "summary,detail"},
        ),
    )

    assert result["rows_written"] == 2
    assert result["provider_results"] == {"summary": "akshare", "detail": "akshare"}


def test_sync_margin_enriches_exchange_detail_fields_from_eastmoney(monkeypatch) -> None:
    FakeRepo.created = 0
    FakeRepo.calls = []
    monkeypatch.setattr(sync_margin, "MarginRepository", FakeRepo)
    monkeypatch.setattr(sync_margin, "EastMoneyHttpClient", lambda session: object())
    monkeypatch.setattr(
        sync_margin,
        "margin_trading_latest",
        lambda client, trade_date: [{
            "stock_code": "600000",
            "rzye": 101,
            "rzmre": 12,
            "rzche": 3,
            "rqyl": 4,
            "rqye": 5,
            "rqmcl": 6,
            "rqchl": 7,
            "rzrqye": 106,
        }],
    )

    result = sync_margin.run(
        PipelineContext(hooks=FakeHooksWithEastMoney()),
        JobRequest(
            job_key="sync_margin",
            dry_run=False,
            params={"trade_date": "2026-07-04", "datasets": "detail"},
        ),
    )

    detail_rows = FakeRepo.calls[0][2]
    assert result["provider_results"]["detail_enrichment"] == "eastmoney.datacenter.margin (1/1)"
    assert detail_rows[0]["financing_repay_amount"] == 3
    assert detail_rows[0]["securities_lending_balance"] == 4
    assert detail_rows[0]["securities_lending_balance_amount"] == 5
    assert detail_rows[0]["margin_balance"] == 106


def test_margin_history_code_source_all_uses_active_stock_basic(monkeypatch) -> None:
    class HistoryResult:
        def all(self):
            return [("600000",), ("000001",)]

    class HistorySession:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def execute(self, statement, params):
            self.statement = str(statement)
            self.params = params
            return HistoryResult()

    class HistoryClient:
        def __init__(self):
            self.db = HistorySession()

        def session(self):
            return self.db

    class HistoryPostgresHook:
        def __init__(self):
            self.client = HistoryClient()

        def get_client(self):
            return self.client

    class HistoryHooks:
        postgres = HistoryPostgresHook()

    codes = sync_margin._margin_history_codes(
        PipelineContext(hooks=HistoryHooks()),
        {"code_source": "all", "limit_codes": 2, "history_days": 120},
    )

    session = HistoryHooks.postgres.client.db
    assert codes == ["600000", "000001"]
    assert "sec_stock_basic" in session.statement
    assert "is_active = TRUE" in session.statement
    assert session.params == {"history_days": 120, "limit": 2}
