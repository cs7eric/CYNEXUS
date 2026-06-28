from __future__ import annotations

import json
import os
from pathlib import Path

import pandas as pd

from cynexus_data.config.settings import ClickHouseConfig
from cynexus_data.storage.clickhouse.client import ClickHouseClient


DEFAULT_TABLE = "ch_dwd_kline_1m"
DDL_PATH = Path(__file__).resolve().parents[4] / "sql" / "clickhouse" / "20260627_create_ch_dwd_kline_1m.sql"
DAILY_RAW_TABLE = "daily_raw"
DAILY_RAW_DDL_PATH = Path(__file__).resolve().parents[4] / "sql" / "clickhouse" / "20260628_create_daily_raw.sql"
DAILY_ADJUSTED_DDL_PATH = Path(__file__).resolve().parents[4] / "sql" / "clickhouse" / "20260628_create_daily_adjusted.sql"
INTRADAY_DETAIL_DDL_PATH = Path(__file__).resolve().parents[4] / "sql" / "clickhouse" / "20260628_create_intraday_detail.sql"


class ClickHouseKlineSink:
    def __init__(self, config: ClickHouseConfig, table: str = DEFAULT_TABLE) -> None:
        self.config = config
        self.table = table
        _ensure_local_no_proxy(config.host)
        self.client = ClickHouseClient(config)

    def create_table(self) -> None:
        for statement in _split_sql(DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def insert_1m(self, frame: pd.DataFrame, batch_size: int = 50_000) -> int:
        if frame.empty:
            return 0

        inserted = 0
        columns = list(frame.columns)
        for start in range(0, len(frame), batch_size):
            batch = frame.iloc[start : start + batch_size]
            self.client.insert_df(self.table, batch[columns], column_names=columns)
            inserted += len(batch)
        return inserted

    def insert_arrow_1m(
        self,
        table,
        batch_rows: int = 100_000,
        max_partitions_per_insert_block: int | None = None,
    ) -> int:
        if table.num_rows == 0:
            return 0
        settings = None
        if max_partitions_per_insert_block is not None:
            settings = {"max_partitions_per_insert_block": max_partitions_per_insert_block}

        if batch_rows <= 0 or batch_rows >= table.num_rows:
            self.client.insert_arrow(self.table, table, settings=settings)
            return table.num_rows

        inserted = 0
        for start in range(0, table.num_rows, batch_rows):
            batch = table.slice(start, batch_rows)
            self.client.insert_arrow(self.table, batch, settings=settings)
            inserted += batch.num_rows
        return inserted


class ClickHouseDailyKlineSink:
    def __init__(self, config: ClickHouseConfig, table: str = DAILY_RAW_TABLE) -> None:
        self.config = config
        self.table = table
        _ensure_local_no_proxy(config.host)
        self.client = ClickHouseClient(config)

    def create_table(self) -> None:
        for statement in _split_sql(DAILY_RAW_DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def insert_daily(self, frame: pd.DataFrame, batch_size: int = 100_000) -> int:
        if frame.empty:
            return 0
        inserted = 0
        columns = list(frame.columns)
        for start in range(0, len(frame), batch_size):
            batch = frame.iloc[start : start + batch_size]
            self.client.insert_df(self.table, batch[columns], column_names=columns)
            inserted += len(batch)
        return inserted


class ClickHouseAdjustedDailyKlineSink:
    def __init__(self, config: ClickHouseConfig, adjust: str) -> None:
        if adjust not in {"qfq", "hfq"}:
            raise ValueError("adjust must be qfq or hfq")
        self.config = config
        self.adjust = adjust
        self.table = f"daily_{adjust}"
        _ensure_local_no_proxy(config.host)
        self.client = ClickHouseClient(config)

    def create_table(self) -> None:
        for statement in _split_sql(DAILY_ADJUSTED_DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def replace_daily(self, frame: pd.DataFrame, batch_size: int = 100_000) -> int:
        if frame.empty:
            return 0
        columns = list(frame.columns)
        inserted = 0
        for start in range(0, len(frame), batch_size):
            batch = frame.iloc[start : start + batch_size]
            self.client.insert_df(self.table, batch[columns], column_names=columns)
            inserted += len(batch)
        return inserted


class ClickHouseIntradayDetailSink:
    def __init__(
        self,
        config: ClickHouseConfig,
        auction_table: str = "ch_ods_stock_auction_point",
        timeshare_table: str = "ch_ods_stock_timeshare_1m",
    ) -> None:
        self.config = config
        self.auction_table = auction_table
        self.timeshare_table = timeshare_table
        _ensure_local_no_proxy(config.host)
        self.client = ClickHouseClient(config)

    def create_tables(self) -> None:
        for statement in _split_sql(INTRADAY_DETAIL_DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def insert_auction_points(self, frame: pd.DataFrame, batch_size: int = 50_000) -> int:
        return self._insert_frame(self.auction_table, frame, batch_size=batch_size)

    def insert_timeshare(self, frame: pd.DataFrame, batch_size: int = 50_000) -> int:
        return self._insert_frame(self.timeshare_table, frame, batch_size=batch_size)

    def _insert_frame(self, table: str, frame: pd.DataFrame, batch_size: int) -> int:
        if frame.empty:
            return 0
        prepared = frame.copy()
        if "raw_payload" in prepared.columns:
            prepared["raw_payload"] = prepared["raw_payload"].map(_json_dumps)
        inserted = 0
        columns = list(prepared.columns)
        for start in range(0, len(prepared), batch_size):
            batch = prepared.iloc[start : start + batch_size]
            self.client.insert_df(table, batch[columns], column_names=columns)
            inserted += len(batch)
        return inserted


def _split_sql(sql: str) -> list[str]:
    statements: list[str] = []
    current: list[str] = []
    for raw_line in sql.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("--"):
            continue
        current.append(raw_line)
        if line.endswith(";"):
            statement = "\n".join(current).rstrip().rstrip(";").strip()
            if statement:
                statements.append(statement)
            current = []
    if current:
        statements.append("\n".join(current).strip())
    return statements


def _json_dumps(value) -> str:
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":"))


def _ensure_local_no_proxy(host: str) -> None:
    if host not in {"127.0.0.1", "localhost", "::1"}:
        return

    additions = ["127.0.0.1", "localhost", "::1"]
    for env_name in ["NO_PROXY", "no_proxy"]:
        existing = os.getenv(env_name, "")
        parts = [part.strip() for part in existing.split(",") if part.strip()]
        merged = parts[:]
        for addition in additions:
            if addition not in merged:
                merged.append(addition)
        os.environ[env_name] = ",".join(merged)
