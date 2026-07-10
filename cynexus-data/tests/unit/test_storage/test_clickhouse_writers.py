from __future__ import annotations

from datetime import date, datetime

import pandas as pd

from cynexus_data.storage.clickhouse.writers import (
    ClickHouseAdjustedDailyKlineSink,
    ClickHouseDailyKlineSink,
    ClickHouseIntradayDetailSink,
    ClickHouseKlineSink,
)


class FakeClickHouseClient:
    def __init__(self, columns: list[str]) -> None:
        self.columns = columns
        self.query_calls: list[str] = []
        self.command_calls: list[dict] = []
        self.insert_calls: list[dict] = []

    def query_rows(self, sql):
        self.query_calls.append(sql)
        return [(column,) for column in self.columns]

    def insert_df(self, table, frame, column_names=None):
        self.insert_calls.append(
            {
                "table": table,
                "columns": list(column_names or []),
                "frame_columns": list(frame.columns),
                "rows": len(frame),
                "records": frame.to_dict("records"),
            }
        )

    def command(self, sql, parameters=None, **kwargs):
        self.command_calls.append({"sql": sql, "parameters": dict(parameters or {}), "kwargs": kwargs})


def test_daily_raw_writer_projects_to_existing_legacy_table_columns() -> None:
    client = FakeClickHouseClient(
        [
            "code",
            "trade_date",
            "open",
            "high",
            "low",
            "close",
            "volume",
            "amount",
            "unit_scale",
            "source",
            "ingested_at",
        ]
    )
    sink = ClickHouseDailyKlineSink.__new__(ClickHouseDailyKlineSink)
    sink.table = "daily_raw"
    sink.client = client
    sink._table_columns = None
    frame = pd.DataFrame(
        [
            {
                "market": "SH",
                "symbol": "600000",
                "code": "600000",
                "tdx_code": "sh600000",
                "trade_date": date(2026, 6, 26),
                "open": 10,
                "high": 11,
                "low": 9,
                "close": 10.5,
                "volume": 100,
                "amount": 1000,
                "unit_scale": 1,
                "source": "tdx_day",
                "source_file": "sh600000.day",
                "source_file_mtime": datetime(2026, 6, 26),
                "ingested_at": datetime(2026, 6, 29),
            }
        ]
    )

    assert sink.insert_daily(frame) == 1

    assert client.insert_calls == [
        {
            "table": "daily_raw",
            "columns": client.columns,
            "frame_columns": client.columns,
            "rows": 1,
            "records": [
                {
                    "code": "600000",
                    "trade_date": date(2026, 6, 26),
                    "open": 10,
                    "high": 11,
                    "low": 9,
                    "close": 10.5,
                    "volume": 100,
                    "amount": 1000,
                    "unit_scale": 1,
                    "source": "tdx_day",
                    "ingested_at": datetime(2026, 6, 29),
                }
            ],
        }
    ]
    assert client.command_calls[0]["sql"] == "ALTER TABLE daily_raw DELETE WHERE (code IN ('600000')) AND trade_date BETWEEN toDate('2026-06-26') AND toDate('2026-06-26')"
    assert client.query_calls == ["DESCRIBE TABLE daily_raw"]


def test_adjusted_daily_writer_projects_to_existing_legacy_table_columns() -> None:
    legacy_columns = [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "adj_factor",
        "ingested_at",
    ]
    client = FakeClickHouseClient(legacy_columns)
    sink = ClickHouseAdjustedDailyKlineSink.__new__(ClickHouseAdjustedDailyKlineSink)
    sink.table = "daily_qfq"
    sink.client = client
    sink._table_columns = None
    frame = pd.DataFrame(
        [
            {
                "market": "SZ",
                "symbol": "000001",
                "code": "000001",
                "tdx_code": "sz000001",
                "trade_date": date(2026, 6, 26),
                "open": 10,
                "high": 11,
                "low": 9,
                "close": 10.5,
                "volume": 100,
                "amount": 1000,
                "adj_factor": 1.0,
                "adjust": "qfq",
                "source": "eltdx",
                "ingested_at": datetime(2026, 6, 29),
            }
        ]
    )

    assert sink.replace_daily(frame) == 1

    assert client.insert_calls[0]["table"] == "daily_qfq"
    assert client.insert_calls[0]["columns"] == legacy_columns
    assert client.insert_calls[0]["frame_columns"] == legacy_columns
    assert client.command_calls[0]["sql"] == "ALTER TABLE daily_qfq DELETE WHERE (code IN ('000001')) AND trade_date BETWEEN toDate('2026-06-26') AND toDate('2026-06-26')"


def test_daily_raw_date_replace_keeps_source_filter_when_table_has_source() -> None:
    columns = [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "unit_scale",
        "source",
        "ingested_at",
    ]
    client = FakeClickHouseClient(columns)
    sink = ClickHouseDailyKlineSink.__new__(ClickHouseDailyKlineSink)
    sink.table = "daily_raw"
    sink.client = client
    sink._table_columns = None
    frame = pd.DataFrame(
        [
            {
                "code": "600000",
                "trade_date": date(2026, 6, 30),
                "open": 10,
                "high": 11,
                "low": 9,
                "close": 10.5,
                "volume": 100,
                "amount": 1000,
                "unit_scale": 1,
                "source": "tdx_day",
                "ingested_at": datetime(2026, 7, 1),
            }
        ]
    )

    assert sink.insert_daily(frame, replace_scope="date") == 1

    assert client.command_calls[0]["sql"] == "ALTER TABLE daily_raw DELETE WHERE trade_date BETWEEN toDate('2026-06-30') AND toDate('2026-06-30') AND source IN ('tdx_day')"


def test_adjusted_daily_date_replace_omits_source_filter_when_table_has_no_source() -> None:
    legacy_columns = [
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "adj_factor",
        "ingested_at",
    ]
    client = FakeClickHouseClient(legacy_columns)
    sink = ClickHouseAdjustedDailyKlineSink.__new__(ClickHouseAdjustedDailyKlineSink)
    sink.table = "daily_qfq"
    sink.client = client
    sink._table_columns = None
    frame = pd.DataFrame(
        [
            {
                "code": "000001",
                "trade_date": date(2026, 6, 30),
                "open": 10,
                "high": 11,
                "low": 9,
                "close": 10.5,
                "volume": 100,
                "amount": 1000,
                "adj_factor": 1.0,
                "source": "eltdx",
                "ingested_at": datetime(2026, 7, 1),
            }
        ]
    )

    assert sink.replace_daily(frame, replace_scope="date") == 1

    assert client.command_calls[0]["sql"] == "ALTER TABLE daily_qfq DELETE WHERE trade_date BETWEEN toDate('2026-06-30') AND toDate('2026-06-30')"


def test_intraday_auction_delete_scope_includes_phase() -> None:
    client = FakeClickHouseClient([])
    sink = ClickHouseIntradayDetailSink.__new__(ClickHouseIntradayDetailSink)
    sink.auction_table = "ch_ods_stock_auction_point"
    sink.client = client
    frame = pd.DataFrame(
        [
            {
                "market": "SH",
                "symbol": "600000",
                "code": "600000",
                "tdx_code": "sh600000",
                "trade_date": date(2026, 6, 26),
                "trade_time": datetime(2026, 6, 26, 9, 25),
                "time_label": "09:25:00",
                "phase": "opening",
                "price": 10.0,
                "matched_volume": 100.0,
                "matched_amount": 1000.0,
                "unmatched_buy_volume": None,
                "unmatched_sell_volume": None,
                "source": "eltdx",
                "raw_payload": {},
                "fetched_at": datetime(2026, 6, 26, 9, 26),
                "ingested_at": datetime(2026, 6, 26, 9, 26),
            }
        ]
    )

    assert sink.insert_auction_points(frame) == 1

    command = client.command_calls[0]
    assert "AND phase = %(phase_0)s" in command["sql"]
    assert command["parameters"]["phase_0"] == "opening"


def test_daily_raw_writer_keeps_metadata_when_table_has_wide_columns() -> None:
    wide_columns = [
        "market",
        "symbol",
        "code",
        "tdx_code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "amount",
        "volume",
        "unit_scale",
        "source",
        "source_file",
        "source_file_mtime",
        "ingested_at",
    ]
    client = FakeClickHouseClient(wide_columns)
    sink = ClickHouseDailyKlineSink.__new__(ClickHouseDailyKlineSink)
    sink.table = "daily_raw"
    sink.client = client
    sink._table_columns = None
    frame = pd.DataFrame([{**{column: "x" for column in wide_columns}, "code": "600000", "trade_date": date(2026, 6, 26)}])

    assert sink.insert_daily(frame) == 1

    assert client.insert_calls[0]["columns"] == wide_columns
    assert client.command_calls[0]["sql"] == "ALTER TABLE daily_raw DELETE WHERE ((market = 'x' AND code = '600000')) AND trade_date BETWEEN toDate('2026-06-26') AND toDate('2026-06-26')"


def test_daily_raw_writer_keeps_same_code_cross_market_rows_in_wide_table() -> None:
    wide_columns = [
        "market",
        "symbol",
        "code",
        "tdx_code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "amount",
        "volume",
        "unit_scale",
        "source",
        "source_file",
        "source_file_mtime",
        "ingested_at",
    ]
    client = FakeClickHouseClient(wide_columns)
    sink = ClickHouseDailyKlineSink.__new__(ClickHouseDailyKlineSink)
    sink.table = "daily_raw"
    sink.client = client
    sink._table_columns = None
    common = {
        "symbol": "000852",
        "code": "000852",
        "trade_date": date(2026, 6, 29),
        "open": 5,
        "high": 6,
        "low": 4,
        "close": 5,
        "amount": 1000,
        "volume": 100,
        "unit_scale": 1,
        "source": "tdx_day",
        "source_file_mtime": datetime(2026, 6, 29),
        "ingested_at": datetime(2026, 6, 29),
    }
    frame = pd.DataFrame(
        [
            {**common, "market": "SH", "tdx_code": "sh000852", "source_file": "sh000852.day", "close": 8597.89},
            {**common, "market": "SZ", "tdx_code": "sz000852", "source_file": "sz000852.day", "close": 5.01},
        ]
    )

    assert sink.insert_daily(frame) == 2

    records = client.insert_calls[0]["records"]
    assert len(records) == 2
    assert {record["tdx_code"] for record in records} == {"sh000852", "sz000852"}
    assert "(market = 'SH' AND code = '000852')" in client.command_calls[0]["sql"]
    assert "(market = 'SZ' AND code = '000852')" in client.command_calls[0]["sql"]


def test_daily_raw_writer_deduplicates_same_code_date_with_highest_close() -> None:
    client = FakeClickHouseClient(
        [
            "code",
            "trade_date",
            "open",
            "high",
            "low",
            "close",
            "volume",
            "amount",
            "unit_scale",
            "source",
            "ingested_at",
        ]
    )
    sink = ClickHouseDailyKlineSink.__new__(ClickHouseDailyKlineSink)
    sink.table = "daily_raw"
    sink.client = client
    sink._table_columns = None
    frame = pd.DataFrame(
        [
            {
                "code": "000852",
                "trade_date": date(2026, 6, 26),
                "open": 0.5,
                "high": 0.6,
                "low": 0.4,
                "close": 0.504,
                "volume": 100,
                "amount": 1000,
                "unit_scale": 1,
                "source": "tdx_day",
                "ingested_at": datetime(2026, 6, 29, 9),
            },
            {
                "code": "000852",
                "trade_date": date(2026, 6, 26),
                "open": 860,
                "high": 866,
                "low": 850,
                "close": 860.141,
                "volume": 100,
                "amount": 2000,
                "unit_scale": 1000,
                "source": "tdx_day",
                "ingested_at": datetime(2026, 6, 29, 10),
            },
        ]
    )

    assert sink.insert_daily(frame) == 1

    records = client.insert_calls[0]["records"]
    assert len(records) == 1
    assert records[0]["close"] == 860.141
    assert records[0]["unit_scale"] == 1000


def test_kline_writer_can_replace_existing_symbol_date_source_rows() -> None:
    columns = ["market", "symbol", "tdx_code", "period", "trade_time", "trade_date", "open", "high", "low", "close", "amount", "volume", "source", "source_file", "source_file_mtime", "ingested_at"]
    client = FakeClickHouseClient(columns)
    sink = ClickHouseKlineSink.__new__(ClickHouseKlineSink)
    sink.table = "ch_dwd_kline_1m"
    sink.client = client
    frame = pd.DataFrame(
        [
            {
                "market": "SZ",
                "symbol": "000001",
                "tdx_code": "sz000001",
                "period": 1,
                "trade_time": datetime(2026, 6, 26, 9, 31),
                "trade_date": date(2026, 6, 26),
                "open": 10,
                "high": 11,
                "low": 9,
                "close": 10.5,
                "amount": 1000,
                "volume": 100,
                "source": "tdx_local",
                "source_file": "sz000001.lc1",
                "source_file_mtime": datetime(2026, 6, 26),
                "ingested_at": datetime(2026, 6, 29),
            },
            {
                "market": "SZ",
                "symbol": "000001",
                "tdx_code": "sz000001",
                "period": 1,
                "trade_time": datetime(2026, 6, 26, 9, 31),
                "trade_date": date(2026, 6, 26),
                "open": 10,
                "high": 11,
                "low": 9,
                "close": 10.6,
                "amount": 1001,
                "volume": 101,
                "source": "tdx_local",
                "source_file": "sz000001.lc1",
                "source_file_mtime": datetime(2026, 6, 26),
                "ingested_at": datetime(2026, 6, 29, 0, 1),
            },
        ]
    )

    assert sink.insert_1m(frame, replace_existing=True) == 1

    assert "ALTER TABLE ch_dwd_kline_1m DELETE WHERE" in client.command_calls[0]["sql"]
    assert client.command_calls[0]["parameters"]["symbol_0"] == "000001"
    assert client.insert_calls[0]["rows"] == 1
    assert client.insert_calls[0]["records"][0]["close"] == 10.6
