from __future__ import annotations

from datetime import date

import pytest

from cynexus_data.storage.clickhouse import readers


class FakeClickHouseClient:
    def __init__(self, rows=None, one=None):
        self.rows = rows or []
        self.one = one
        self.row_calls = []
        self.one_calls = []

    def query_rows(self, sql, params=None):
        self.row_calls.append((sql, params or {}))
        return self.rows

    def query_one(self, sql, params=None):
        self.one_calls.append((sql, params or {}))
        return self.one


def test_read_daily_kline_bars_matches_legacy_shape_with_adjustment() -> None:
    client = FakeClickHouseClient(rows=[(date(2026, 6, 25), 10, 12, 9, 11, 1000, 11000, 1.23)])

    rows = readers.read_daily_kline_bars(client, "000001", adjust="qfq", limit=5)

    sql, params = client.row_calls[0]
    assert "FROM daily_qfq" in sql
    assert "ORDER BY trade_date DESC LIMIT %(limit)s" in sql
    assert "ORDER BY trade_date ASC" in sql
    assert params == {"code": "000001", "limit": 5}
    assert rows == [
        {
            "timestamp": 1782351000000,
            "trade_date": "2026-06-25",
            "open": 10.0,
            "high": 12.0,
            "low": 9.0,
            "close": 11.0,
            "volume": 1000,
            "amount": 11000.0,
            "adj_factor": 1.23,
        }
    ]


def test_read_daily_kline_bars_raw_omits_adj_factor_and_accepts_dates() -> None:
    client = FakeClickHouseClient(rows=[(date(2026, 6, 26), 1, 2, 0.5, 1.5, 10, 15)])

    rows = readers.read_daily_kline_bars(client, "999999", adjust="none", start="2026-01-01", end=date(2026, 6, 26))

    sql, params = client.row_calls[0]
    assert "FROM daily_raw" in sql
    assert "adj_factor" not in sql
    assert params["start"] == date(2026, 1, 1)
    assert params["end"] == date(2026, 6, 26)
    assert "adj_factor" not in rows[0]


def test_daily_kline_reader_rejects_invalid_adjust() -> None:
    with pytest.raises(ValueError):
        readers.read_daily_kline_bars(FakeClickHouseClient(), "000001", adjust="bad")


def test_latest_and_adjusted_helpers() -> None:
    latest_client = FakeClickHouseClient(one=(date(2026, 6, 26),))
    assert readers.get_latest_daily_trade_date(latest_client, "000001") == date(2026, 6, 26)
    assert "max(trade_date)" in latest_client.one_calls[0][0]

    has_client = FakeClickHouseClient(one=(1,))
    assert readers.has_adjusted_daily(has_client, "000001", adjust="hfq") is True
    assert "daily_hfq" in has_client.one_calls[0][0]

    list_client = FakeClickHouseClient(rows=[("000001",), ("600000",)])
    assert readers.list_codes_with_adjusted_daily(list_client) == ["000001", "600000"]


def test_read_index_daily_bars_groups_anylast_and_returns_ascending() -> None:
    client = FakeClickHouseClient(
        rows=[
            ("sh000300", date(2026, 6, 26), 10, 12, 9, 11, 1000, 11000),
            ("sh000300", date(2026, 6, 25), 9, 10, 8, 9.5, 900, 8550),
        ]
    )

    rows = readers.read_index_daily_bars(client, "sh000300", as_of_date="2026-06-26", days=2)

    sql, params = client.row_calls[0]
    assert "FROM index_daily_raw" in sql
    assert "anyLast(close)" in sql
    assert "GROUP BY code, trade_date" in sql
    assert params["as_of_date"] == date(2026, 6, 26)
    assert [row["trade_date"] for row in rows] == ["2026-06-25", "2026-06-26"]


def test_index_kline_helpers() -> None:
    latest_client = FakeClickHouseClient(one=(date(2026, 6, 26),))
    assert readers.latest_index_trade_date(latest_client, "sh000001") == date(2026, 6, 26)
    assert "index_daily_raw" in latest_client.one_calls[0][0]

    has_client = FakeClickHouseClient(one=(True,))
    assert readers.has_index_kline(has_client, "sh000001", "2026-06-26") is True
    assert has_client.one_calls[0][1]["trade_date"] == date(2026, 6, 26)
