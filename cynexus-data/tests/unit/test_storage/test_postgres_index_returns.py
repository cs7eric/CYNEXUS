from __future__ import annotations

from datetime import date

from cynexus_data.storage.postgres.index_returns import IndexReturnRepository


class FakeExecuteResult:
    def __init__(self, rows=None):
        self.rows = rows or []

    def all(self):
        return self.rows


class FakeSession:
    def __init__(self, rows=None):
        self.rows = rows or []
        self.calls = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, statement, params):
        self.calls.append((str(statement), params))
        return FakeExecuteResult(self.rows)


class FakeClient:
    schema = "cynexus_appl_market"

    def __init__(self, rows=None):
        self.session_obj = FakeSession(rows)

    def session(self):
        return self.session_obj


def test_save_many_upserts_only_available_items() -> None:
    client = FakeClient()
    repo = IndexReturnRepository(client)
    count = repo.save_many(
        trade_date=date(2026, 6, 26),
        window_days=5,
        items=[
            {"available": True, "name": "沪深300", "fullCode": "sh000300", "current": 4000, "currentDate": "2026-06-26", "baseClose": 3900, "baseDate": "2026-06-22", "returnPct": 2.5641},
            {"available": False, "name": "中证1000", "fullCode": "sh000852"},
        ],
    )

    sql, params = client.session_obj.calls[0]
    assert count == 1
    assert "cynexus_appl_market.mkt_index_return_daily" in sql
    assert "ON CONFLICT (trade_date, index_code, window_days)" in sql
    assert params["index_code"] == "sh000300"
    assert params["window_days"] == 5


def test_get_many_maps_rows_to_legacy_shape() -> None:
    client = FakeClient(rows=[("sh000300", "沪深300", 4000, date(2026, 6, 26), 3900, date(2026, 6, 22), 2.56412)])
    items = IndexReturnRepository(client).get_many(trade_date=date(2026, 6, 26), window_days=5)

    assert items == [
        {
            "name": "沪深300",
            "code": "000300",
            "fullCode": "sh000300",
            "current": 4000.0,
            "currentDate": "2026-06-26",
            "baseClose": 3900.0,
            "baseDate": "2026-06-22",
            "returnPct": 2.5641,
            "available": True,
            "fromCache": True,
        }
    ]
