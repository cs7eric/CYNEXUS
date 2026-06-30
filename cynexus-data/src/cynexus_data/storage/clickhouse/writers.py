from __future__ import annotations

import json
import os
import re
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
DAILY_RAW_REQUIRED_COLUMNS = [
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
DAILY_ADJUSTED_REQUIRED_COLUMNS = [
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


class ClickHouseKlineSink:
    def __init__(self, config: ClickHouseConfig, table: str = DEFAULT_TABLE) -> None:
        self.config = config
        self.table = table
        _ensure_local_no_proxy(config.host)
        self.client = ClickHouseClient(config)

    def create_table(self) -> None:
        for statement in _split_sql(DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def insert_1m(self, frame: pd.DataFrame, batch_size: int = 50_000, *, replace_existing: bool = False) -> int:
        if frame.empty:
            return 0

        frame = _deduplicate_frame(frame, ["market", "symbol", "period", "trade_time", "source"])
        if replace_existing:
            _delete_existing_kline_rows(self.client, self.table, frame)
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
        replace_existing: bool = False,
    ) -> int:
        if table.num_rows == 0:
            return 0
        if replace_existing:
            _delete_existing_kline_arrow_rows(self.client, self.table, table)
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
        self._table_columns: list[str] | None = None

    def create_table(self) -> None:
        for statement in _split_sql(DAILY_RAW_DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def insert_daily(self, frame: pd.DataFrame, batch_size: int = 100_000, *, replace_existing: bool = True) -> int:
        if frame.empty:
            return 0
        table_columns = self._get_table_columns()
        frame = _deduplicate_daily_frame(frame)
        if replace_existing:
            _delete_existing_daily_rows(self.client, self.table, frame, table_columns=table_columns)
        inserted = 0
        columns = _resolve_insert_columns(
            frame,
            table_columns=table_columns,
            required_columns=DAILY_RAW_REQUIRED_COLUMNS,
            table=self.table,
        )
        for start in range(0, len(frame), batch_size):
            batch = frame.iloc[start : start + batch_size]
            self.client.insert_df(self.table, batch[columns], column_names=columns)
            inserted += len(batch)
        return inserted

    def _get_table_columns(self) -> list[str]:
        if self._table_columns is None:
            self._table_columns = _describe_table_columns(self.client, self.table)
        return self._table_columns


class ClickHouseAdjustedDailyKlineSink:
    def __init__(self, config: ClickHouseConfig, adjust: str) -> None:
        if adjust not in {"qfq", "hfq"}:
            raise ValueError("adjust must be qfq or hfq")
        self.config = config
        self.adjust = adjust
        self.table = f"daily_{adjust}"
        _ensure_local_no_proxy(config.host)
        self.client = ClickHouseClient(config)
        self._table_columns: list[str] | None = None

    def create_table(self) -> None:
        for statement in _split_sql(DAILY_ADJUSTED_DDL_PATH.read_text(encoding="utf-8")):
            self.client.command(statement)

    def replace_daily(self, frame: pd.DataFrame, batch_size: int = 100_000) -> int:
        if frame.empty:
            return 0
        frame = _deduplicate_daily_frame(frame)
        _delete_existing_daily_rows(self.client, self.table, frame)
        columns = _resolve_insert_columns(
            frame,
            table_columns=self._get_table_columns(),
            required_columns=DAILY_ADJUSTED_REQUIRED_COLUMNS,
            table=self.table,
        )
        inserted = 0
        for start in range(0, len(frame), batch_size):
            batch = frame.iloc[start : start + batch_size]
            self.client.insert_df(self.table, batch[columns], column_names=columns)
            inserted += len(batch)
        return inserted

    def _get_table_columns(self) -> list[str]:
        if self._table_columns is None:
            self._table_columns = _describe_table_columns(self.client, self.table)
        return self._table_columns


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
        return self._insert_frame(
            self.auction_table,
            frame,
            batch_size=batch_size,
            key_columns=["trade_date", "market", "symbol", "trade_time", "phase", "source"],
        )

    def insert_timeshare(self, frame: pd.DataFrame, batch_size: int = 50_000) -> int:
        return self._insert_frame(
            self.timeshare_table,
            frame,
            batch_size=batch_size,
            key_columns=["trade_date", "market", "symbol", "trade_time", "source"],
        )

    def _insert_frame(self, table: str, frame: pd.DataFrame, batch_size: int, *, key_columns: list[str]) -> int:
        if frame.empty:
            return 0
        prepared = _deduplicate_frame(frame, key_columns)
        _delete_existing_intraday_rows(self.client, table, prepared)
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


def _delete_existing_daily_rows(
    client: ClickHouseClient,
    table: str,
    frame: pd.DataFrame,
    *,
    table_columns: list[str] | None = None,
) -> None:
    if frame.empty or "code" not in frame.columns or "trade_date" not in frame.columns:
        return
    min_date = pd.to_datetime(frame["trade_date"].min()).date()
    max_date = pd.to_datetime(frame["trade_date"].max()).date()
    table_has_market = bool(table_columns and "market" in table_columns and "market" in frame.columns)
    if table_has_market:
        pairs = frame[["market", "code"]].dropna().drop_duplicates().sort_values(["market", "code"])
        if pairs.empty:
            return
        clauses = [
            f"(market = '{_escape_ch_string(str(row.market))}' AND code = '{_escape_ch_string(str(row.code))}')"
            for row in pairs.itertuples(index=False)
        ]
        key_where = " OR ".join(clauses)
    else:
        codes = sorted(str(value) for value in frame["code"].dropna().unique())
        if not codes:
            return
        key_where = f"code IN ({_quote_ch_strings(codes)})"
    client.command(
        f"ALTER TABLE {table} DELETE "
        f"WHERE ({key_where}) "
        f"AND trade_date BETWEEN toDate('{min_date}') AND toDate('{max_date}')",
        settings={"mutations_sync": 1},
    )


def _delete_existing_kline_rows(client: ClickHouseClient, table: str, frame: pd.DataFrame) -> None:
    required = {"market", "symbol", "period", "trade_date", "source"}
    if frame.empty or not required.issubset(frame.columns):
        return
    clauses: list[str] = []
    params: dict[str, object] = {}
    for index, row in frame[list(required)].drop_duplicates().reset_index(drop=True).iterrows():
        params[f"market_{index}"] = str(row["market"])
        params[f"symbol_{index}"] = str(row["symbol"])
        params[f"period_{index}"] = int(row["period"])
        params[f"source_{index}"] = str(row["source"])
        params[f"trade_date_{index}"] = pd.to_datetime(row["trade_date"]).date()
        clauses.append(
            "("
            f"market = %(market_{index})s "
            f"AND symbol = %(symbol_{index})s "
            f"AND period = %(period_{index})s "
            f"AND source = %(source_{index})s "
            f"AND trade_date = %(trade_date_{index})s"
            ")"
        )
    if not clauses:
        return
    client.command(
        f"ALTER TABLE {_safe_ch_identifier(table)} DELETE WHERE {' OR '.join(clauses)}",
        params,
        settings={"mutations_sync": 1},
    )


def _delete_existing_kline_arrow_rows(client: ClickHouseClient, table: str, arrow_table) -> None:
    names = set(getattr(arrow_table, "column_names", []) or [])
    required = {"market", "symbol", "period", "trade_date", "source"}
    if not required.issubset(names):
        return
    frame = arrow_table.select(list(required)).to_pandas()
    _delete_existing_kline_rows(client, table, frame)


def _delete_existing_intraday_rows(client: ClickHouseClient, table: str, frame: pd.DataFrame) -> None:
    required = {"trade_date", "market", "symbol", "source"}
    if frame.empty or not required.issubset(frame.columns):
        return
    clauses: list[str] = []
    params: dict[str, object] = {}
    for index, row in frame[list(required)].drop_duplicates().reset_index(drop=True).iterrows():
        params[f"trade_date_{index}"] = pd.to_datetime(row["trade_date"]).date()
        params[f"market_{index}"] = str(row["market"])
        params[f"symbol_{index}"] = str(row["symbol"])
        params[f"source_{index}"] = str(row["source"])
        clauses.append(
            "("
            f"trade_date = %(trade_date_{index})s "
            f"AND market = %(market_{index})s "
            f"AND symbol = %(symbol_{index})s "
            f"AND source = %(source_{index})s"
            ")"
        )
    if not clauses:
        return
    client.command(
        f"ALTER TABLE {_safe_ch_identifier(table)} DELETE WHERE {' OR '.join(clauses)}",
        params,
        settings={"mutations_sync": 1},
    )


def _deduplicate_daily_frame(frame: pd.DataFrame) -> pd.DataFrame:
    if frame.empty or not {"code", "trade_date"}.issubset(frame.columns):
        return frame
    if {"market", "symbol", "trade_date"}.issubset(frame.columns):
        return _pick_max_close_per_key(frame, ["market", "symbol", "trade_date"])
    return _pick_max_close_per_key(frame, ["code", "trade_date"])


def _pick_max_close_per_key(frame: pd.DataFrame, key_columns: list[str]) -> pd.DataFrame:
    if "close" not in frame.columns or not set(key_columns).issubset(frame.columns):
        return _deduplicate_frame(frame, key_columns)
    prepared = frame.copy()
    prepared["_dedupe_order"] = range(len(prepared))
    prepared["_close_for_dedupe"] = pd.to_numeric(prepared["close"], errors="coerce").fillna(float("-inf"))
    prepared = prepared.sort_values(key_columns + ["_close_for_dedupe", "_dedupe_order"])
    deduped = prepared.drop_duplicates(subset=key_columns, keep="last")
    return deduped.drop(columns=["_close_for_dedupe", "_dedupe_order"]).sort_index().reset_index(drop=True)


def _deduplicate_frame(frame: pd.DataFrame, key_columns: list[str]) -> pd.DataFrame:
    if frame.empty or not set(key_columns).issubset(frame.columns):
        return frame.copy()
    return frame.drop_duplicates(subset=key_columns, keep="last").reset_index(drop=True)


def _quote_ch_strings(values: list[str]) -> str:
    return ", ".join("'" + _escape_ch_string(value) + "'" for value in values)


def _escape_ch_string(value: str) -> str:
    return value.replace("'", "''")


def _json_dumps(value) -> str:
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":"))


def _resolve_insert_columns(
    frame: pd.DataFrame,
    *,
    table_columns: list[str],
    required_columns: list[str],
    table: str,
) -> list[str]:
    frame_columns = set(frame.columns)
    missing_required = [column for column in required_columns if column not in frame_columns]
    if missing_required:
        raise ValueError(f"{table} insert frame missing required columns: {', '.join(missing_required)}")

    table_column_set = set(table_columns)
    unsupported_required = [column for column in required_columns if column not in table_column_set]
    if unsupported_required:
        raise ValueError(f"{table} table missing required columns: {', '.join(unsupported_required)}")

    columns = [column for column in table_columns if column in frame_columns]
    if not columns:
        raise ValueError(f"{table} insert has no matching columns")
    return columns


def _describe_table_columns(client: ClickHouseClient, table: str) -> list[str]:
    table = _safe_ch_identifier(table)
    rows = client.query_rows(f"DESCRIBE TABLE {table}")
    return [str(row[0]) for row in rows]


def _safe_ch_identifier(identifier: str) -> str:
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)?", identifier):
        raise ValueError(f"Invalid ClickHouse table identifier: {identifier}")
    return identifier


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
