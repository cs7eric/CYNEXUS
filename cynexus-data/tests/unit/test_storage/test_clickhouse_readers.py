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
    assert "GROUP BY trade_date" in sql
    assert "ORDER BY trade_date DESC" in sql
    assert "LIMIT %(limit)s" in sql
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

    latest_table_client = FakeClickHouseClient(one=(date(2026, 6, 29),))
    assert readers.get_latest_table_trade_date(latest_table_client, "daily_raw") == date(2026, 6, 29)
    assert "FROM daily_raw" in latest_table_client.one_calls[0][0]

    previous_table_client = FakeClickHouseClient(one=(date(2026, 6, 26),))
    assert readers.get_previous_table_trade_date(previous_table_client, "2026-06-29", "daily_raw") == date(2026, 6, 26)
    assert "trade_date < %(trade_date)s" in previous_table_client.one_calls[0][0]
    assert previous_table_client.one_calls[0][1]["trade_date"] == date(2026, 6, 29)

    has_client = FakeClickHouseClient(one=(1,))
    assert readers.has_adjusted_daily(has_client, "000001", adjust="hfq") is True
    assert "daily_hfq" in has_client.one_calls[0][0]

    list_client = FakeClickHouseClient(rows=[("000001",), ("600000",)])
    assert readers.list_codes_with_adjusted_daily(list_client) == ["000001", "600000"]


def test_latest_effective_kline_trade_date_requires_symbol_coverage() -> None:
    client = FakeClickHouseClient(one=(date(2026, 7, 27),))

    result = readers.get_latest_effective_kline_trade_date(client, expected_symbols=100)

    assert result == date(2026, 7, 27)
    sql, params = client.one_calls[0]
    assert "uniqExact(tdx_code)" in sql
    assert params == {"source": "tdx_local", "period": 1, "minimum_symbols": 95}


def test_read_index_daily_bars_groups_max_close_and_returns_ascending() -> None:
    client = FakeClickHouseClient(
        rows=[
            ("sh000300", date(2026, 6, 26), 10, 12, 9, 11, 1000, 11000),
            ("sh000300", date(2026, 6, 25), 9, 10, 8, 9.5, 900, 8550),
        ]
    )

    rows = readers.read_index_daily_bars(client, "sh000300", as_of_date="2026-06-26", days=2)

    sql, params = client.row_calls[0]
    assert "FROM index_daily_raw" in sql
    assert "max(close) AS close_price" in sql
    assert "GROUP BY code, trade_date" in sql
    assert params["as_of_date"] == date(2026, 6, 26)
    assert [row["trade_date"] for row in rows] == ["2026-06-25", "2026-06-26"]


def test_read_close_series_groups_duplicate_trade_dates() -> None:
    client = FakeClickHouseClient(rows=[(date(2026, 6, 26), 860.141), (date(2026, 6, 25), 882.592)])

    rows = readers.read_close_series(client, "000852", "2026-06-26", table="daily_raw", limit=6)

    sql, params = client.row_calls[0]
    assert "max(close)" in sql
    assert "GROUP BY trade_date" in sql
    assert "FROM daily_raw" in sql
    assert "tdx_code = %(tdx_code)s" in sql
    assert params == {"tdx_code": "sh000852", "trade_date": "2026-06-26", "limit": 6}
    assert [row["trade_date"] for row in rows] == ["2026-06-25", "2026-06-26"]


def test_read_close_series_accepts_prefixed_tdx_code_for_wide_daily_raw() -> None:
    client = FakeClickHouseClient(rows=[(date(2026, 7, 8), 4755.53)])

    readers.read_close_series(client, "sh000300", "2026-07-08", table="daily_raw", limit=1)

    sql, params = client.row_calls[0]
    assert "tdx_code = %(tdx_code)s" in sql
    assert params["tdx_code"] == "sh000300"


def test_turnover_activity_reader_deduplicates_index_amounts_before_sum() -> None:
    client = FakeClickHouseClient(rows=[(date(2026, 6, 29), 16662.2, 18515.3, 35177.5)])

    rows = readers.read_turnover_activity_rows(client, "2026-06-29", table="daily_raw", limit_days=30)

    sql, params = client.row_calls[0]
    assert "SELECT tdx_code, trade_date, max(amount) AS amount" in sql
    assert "GROUP BY tdx_code, trade_date" in sql
    assert "sumIf(amount, tdx_code = 'sh999999')" in sql
    assert params == {"trade_date": "2026-06-29", "limit": 30}
    assert rows == [
        {
            "trade_date": "2026-06-29",
            "sh_amount": 16662.2,
            "sz_amount": 18515.3,
            "total_amount": 35177.5,
        }
    ]


def test_limit_emotion_readers_group_duplicate_daily_rows() -> None:
    client = FakeClickHouseClient(rows=[("SH", "000852", "sh000852", date(2026, 6, 29), 8616.0, 8677.0, 8413.0, 8597.0, 8600.0)])

    rows = readers.read_limit_emotion_ohlcv_rows(client, "2026-06-29", table="daily_raw")

    sql, params = client.row_calls[0]
    assert "argMax(open, close)" in sql
    assert "GROUP BY market, code, tdx_code, trade_date" in sql
    assert "PARTITION BY tdx_code" in sql
    assert params == {"trade_date": "2026-06-29"}
    assert rows[0]["tdxCode"] == "sh000852"
    assert rows[0]["close"] == 8597.0


def test_daily_quality_counts_duplicate_and_scaled_keys() -> None:
    duplicate_client = FakeClickHouseClient(one=(9,))
    assert readers.count_duplicate_daily_keys(duplicate_client, "daily_raw", start_date="2026-06-29", end_date="2026-06-29") == 9
    sql, params = duplicate_client.one_calls[0]
    assert "GROUP BY market, symbol, trade_date" in sql
    assert "HAVING row_count > 1" in sql
    assert params == {"start_date": date(2026, 6, 29), "end_date": date(2026, 6, 29)}

    scaled_client = FakeClickHouseClient(one=(4,))
    assert readers.count_scaled_duplicate_daily_keys(scaled_client, "daily_raw", end_date="2026-06-29", ratio_threshold=50) == 4
    sql, params = scaled_client.one_calls[0]
    assert "max_close / min_close > %(ratio_threshold)s" in sql
    assert params == {"end_date": date(2026, 6, 29), "ratio_threshold": 50.0}


def test_index_kline_helpers() -> None:
    latest_client = FakeClickHouseClient(one=(date(2026, 6, 26),))
    assert readers.latest_index_trade_date(latest_client, "sh000001") == date(2026, 6, 26)
    assert "index_daily_raw" in latest_client.one_calls[0][0]

    has_client = FakeClickHouseClient(one=(True,))
    assert readers.has_index_kline(has_client, "sh000001", "2026-06-26") is True
    assert has_client.one_calls[0][1]["trade_date"] == date(2026, 6, 26)
