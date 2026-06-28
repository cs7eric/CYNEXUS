from __future__ import annotations

from cynexus_data.storage.postgres.stock_turnover import StockTurnoverRepository


class FakeMappings:
    def __init__(self, row=None):
        self.row = row

    def first(self):
        return self.row


class FakeExecuteResult:
    def __init__(self, row=None):
        self.row = row

    def mappings(self):
        return FakeMappings(self.row)


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


def test_upsert_series_writes_payload_and_latest_fields() -> None:
    client = FakeClient()
    repo = StockTurnoverRepository(client)
    row = repo.upsert_series(
        {
            "symbol": "000001",
            "target_type": "stock",
            "period": "1d",
            "adjust": "qfq",
            "circulating_shares": 1_000_000_000,
            "total_shares": 1_200_000_000,
            "source": "test",
            "entries": [{"trade_date": "2026-06-26", "turnover_rate": 0.12}],
        },
        source="test",
    )

    sql, params = client.session_obj.calls[0]
    assert "cynexus_appl_market.stock_turnover_series_snapshot" in sql
    assert "ON CONFLICT (symbol, target_type, period, adjust, source)" in sql
    assert params["latest_trade_date"] == "2026-06-26"
    assert params["latest_turnover_rate"] == 0.12
    assert row["entry_count"] == 1
