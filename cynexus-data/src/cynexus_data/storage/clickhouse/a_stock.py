from __future__ import annotations

from pathlib import Path
from typing import Any

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.clickhouse.client import ClickHouseClient
from cynexus_data.storage.clickhouse.ddl import apply_sql_file


DDL_PATH = PROJECT_ROOT / "sql" / "clickhouse" / "20260707_create_a_stock_data_tables.sql"

TABLE_COLUMNS = {
    "ch_ods_stock_fund_flow_minute": [
        "source",
        "code",
        "trade_date",
        "trade_time",
        "main_net",
        "small_net",
        "mid_net",
        "large_net",
        "super_net",
        "raw_payload",
        "ingested_at",
    ],
    "ch_ods_stock_transaction": [
        "source",
        "code",
        "trade_date",
        "trade_time",
        "price",
        "volume",
        "amount",
        "buy_sell",
        "raw_payload",
        "ingested_at",
    ],
    "ch_ods_stock_order_book_snapshot": [
        "source",
        "code",
        "quote_time",
        "trade_date",
        "price",
        "open",
        "high",
        "low",
        "pre_close",
        "volume",
        "amount",
        "bid_prices",
        "ask_prices",
        "bid_volumes",
        "ask_volumes",
        "raw_payload",
        "ingested_at",
    ],
    "ch_ods_baidu_kline_ma": [
        "source",
        "code",
        "trade_date",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "amount",
        "ma5",
        "ma10",
        "ma20",
        "raw_payload",
        "ingested_at",
    ],
    "ch_ods_northbound_minute": [
        "source",
        "trade_date",
        "minute_time",
        "hgt_yi",
        "sgt_yi",
        "raw_payload",
        "ingested_at",
    ],
    "ch_ods_option_quote_snapshot": [
        "source",
        "option_code",
        "quote_time",
        "trade_date",
        "name",
        "last",
        "bid",
        "ask",
        "strike",
        "volume",
        "amount",
        "open_interest",
        "delta",
        "gamma",
        "theta",
        "vega",
        "iv",
        "raw_payload",
        "ingested_at",
    ],
}


class AStockClickHouseRepository:
    def __init__(self, client: ClickHouseClient) -> None:
        self.client = client

    def create_tables(self, path: Path = DDL_PATH) -> int:
        return apply_sql_file(self.client, path)

    def insert_batches(self, batches: dict[str, list[dict[str, Any]]]) -> int:
        total = 0
        for table, rows in batches.items():
            total += self.insert_many(table, rows)
        return total

    def insert_many(self, table: str, rows: list[dict[str, Any]]) -> int:
        if not rows:
            return 0
        if table not in TABLE_COLUMNS:
            raise ValueError(f"unsupported A-stock ClickHouse table: {table}")
        columns = TABLE_COLUMNS[table]
        values = [tuple(row.get(column) for column in columns) for row in rows]
        self.client.insert(table, values, columns)
        return len(values)
