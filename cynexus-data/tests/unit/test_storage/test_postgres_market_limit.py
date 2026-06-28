from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.market_limit import MarketLimitRepository


class FakeMappings:
    def __init__(self, rows=None):
        self.rows = rows or []

    def all(self):
        return self.rows


class FakeExecuteResult:
    def __init__(self, rows=None):
        self.rows = rows or []

    def mappings(self):
        return FakeMappings(self.rows)


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


def test_replace_stock_snapshots_soft_deletes_and_upserts_rows() -> None:
    client = FakeClient()
    repo = MarketLimitRepository(client)

    count = repo.replace_stock_snapshots(
        trade_date=date(2026, 6, 26),
        source="test",
        rows=[{"code": "1", "category": "limit_up", "streak": 2, "changePct": 10.0, "payload": {"code": "1"}}],
    )

    assert count == 1
    delete_sql, delete_params = client.session_obj.calls[0]
    insert_sql, insert_params = client.session_obj.calls[1]
    assert "UPDATE cynexus_appl_market.market_limit_daily_stocks" in delete_sql
    assert "INSERT INTO cynexus_appl_market.market_limit_daily_stocks" in insert_sql
    assert "ON CONFLICT (trade_date, code) WHERE deleted_at IS NULL" in insert_sql
    assert insert_params["code"] == "000001"
