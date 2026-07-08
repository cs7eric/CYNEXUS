from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.margin import MarginRepository


class FakeExecuteResult:
    def mappings(self):
        return self

    def all(self):
        return []


class FakeSession:
    def __init__(self):
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return FakeExecuteResult()


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self):
        self.session_obj = FakeSession()

    def session(self):
        return self.session_obj


def test_replace_market_summary_upserts_natural_key() -> None:
    client = FakeClient()
    repo = MarginRepository(client)

    written = repo.replace_market_summary(
        trade_date=date(2026, 7, 4),
        rows=[
            {
                "trade_date": date(2026, 7, 4),
                "exchange": "SSE",
                "source": "akshare.stock_margin_sse",
                "financing_balance": 100,
                "extra": {},
            }
        ],
    )

    delete_sql = client.session_obj.calls[0][0]
    sql = client.session_obj.calls[1][0]
    assert written == 1
    assert "UPDATE cynexus_appl_market.margin_market_summary" in delete_sql
    assert "INSERT INTO cynexus_appl_market.margin_market_summary" in sql
    assert "ON CONFLICT (trade_date, exchange, source)" in sql
    assert client.session_obj.calls[1][1]["exchange"] == "SSE"


def test_replace_stock_detail_upserts_stock_key() -> None:
    client = FakeClient()
    repo = MarginRepository(client)

    written = repo.replace_stock_detail(
        trade_date=date(2026, 7, 4),
        rows=[
            {
                "trade_date": date(2026, 7, 4),
                "exchange": "SZSE",
                "stock_code": "000001",
                "stock_name": "平安银行",
                "source": "akshare.stock_margin_detail_szse",
                "financing_balance": 100,
                "extra": {},
            }
        ],
    )

    delete_sql = client.session_obj.calls[0][0]
    sql = client.session_obj.calls[1][0]
    assert written == 1
    assert "UPDATE cynexus_appl_market.margin_stock_detail" in delete_sql
    assert "INSERT INTO cynexus_appl_market.margin_stock_detail" in sql
    assert "ON CONFLICT (trade_date, exchange, stock_code, source)" in sql
    assert client.session_obj.calls[1][1]["stock_code"] == "000001"


def test_replace_account_stats_upserts_date_source() -> None:
    client = FakeClient()
    repo = MarginRepository(client)

    written = repo.replace_account_stats(
        rows=[
            {
                "trade_date": date(2026, 7, 4),
                "source": "akshare.stock_margin_account_info",
                "financing_balance": 100,
                "extra": {},
            }
        ],
    )

    sql = client.session_obj.calls[0][0]
    assert written == 1
    assert "INSERT INTO cynexus_appl_market.margin_account_stats" in sql
    assert "ON CONFLICT (trade_date, source)" in sql
