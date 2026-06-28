from __future__ import annotations

from cynexus_data.storage.clickhouse.client import ClickHouseClient


def delete_symbol_range(client: ClickHouseClient, table: str, symbol: str, start_date: str, end_date: str) -> None:
    client.command(
        f"ALTER TABLE {table} DELETE WHERE symbol = %(symbol)s AND trade_date BETWEEN %(start)s AND %(end)s",
        {"symbol": symbol, "start": start_date, "end": end_date},
        settings={"mutations_sync": 1},
    )

