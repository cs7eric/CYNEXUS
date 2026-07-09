from __future__ import annotations

from pathlib import Path

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.clickhouse.client import ClickHouseClient


def apply_sql_file(client: ClickHouseClient, path: Path) -> int:
    count = 0
    for statement in split_sql(path.read_text(encoding="utf-8")):
        client.command(statement)
        count += 1
    return count


def create_kline_1m(client: ClickHouseClient) -> int:
    return apply_sql_file(client, PROJECT_ROOT / "sql" / "clickhouse" / "20260627_create_ch_dwd_kline_1m.sql")


def create_daily_raw(client: ClickHouseClient) -> int:
    return apply_sql_file(client, PROJECT_ROOT / "sql" / "clickhouse" / "20260628_create_daily_raw.sql")


def create_daily_adjusted(client: ClickHouseClient) -> int:
    return apply_sql_file(client, PROJECT_ROOT / "sql" / "clickhouse" / "20260628_create_daily_adjusted.sql")


def create_index_daily_raw(client: ClickHouseClient) -> int:
    return apply_sql_file(client, PROJECT_ROOT / "sql" / "clickhouse" / "20260708_create_index_daily_raw.sql")


def split_sql(sql: str) -> list[str]:
    statements: list[str] = []
    current: list[str] = []
    for raw_line in sql.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("--"):
            continue
        current.append(raw_line)
        if line.endswith(";"):
            statements.append("\n".join(current).rstrip().rstrip(";").strip())
            current = []
    if current:
        statements.append("\n".join(current).strip())
    return [item for item in statements if item]
