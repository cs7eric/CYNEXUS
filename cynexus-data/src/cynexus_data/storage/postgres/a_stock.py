from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260707_create_a_stock_data_tables.sql"

JSONB_COLUMNS = {
    "concepts",
    "fields",
    "metrics",
    "raw_payload",
}

TABLE_KEYS = {
    "sec_stock_basic": ["code"],
    "a_stock_research_report": ["source", "scope", "code", "industry_code", "report_id"],
    "a_stock_financial_statement": ["source", "code", "report_type", "report_date"],
    "a_stock_announcement": ["source", "code", "announcement_id"],
    "a_stock_news": ["source", "code", "news_id"],
    "a_stock_investor_interaction": ["source", "code", "interaction_id"],
    "a_stock_fund_flow_daily": ["source", "code", "trade_date"],
    "a_stock_industry_rank_snapshot": ["source", "snapshot_date", "rank_direction", "rank_no", "industry_code"],
    "a_stock_dragon_tiger_daily": ["source", "trade_date", "code", "reason"],
    "a_stock_dragon_tiger_seat": ["source", "trade_date", "code", "side", "seat_rank", "seat_name"],
    "a_stock_lockup_expiry": ["source", "code", "free_date", "lockup_type"],
    "a_stock_block_trade": ["source", "code", "trade_date", "price", "buyer", "seller"],
    "a_stock_holder_num": ["source", "code", "end_date"],
    "a_stock_dividend": ["source", "code", "ex_dividend_date", "plan"],
    "a_stock_hot_rank_snapshot": ["source", "snapshot_date", "list_type", "rank_no", "code"],
    "a_stock_hot_concept_snapshot": ["source", "snapshot_date", "code", "concept_code", "concept_name"],
    "a_stock_hot_reason_snapshot": ["source", "snapshot_date", "code", "reason_key"],
    "a_stock_limit_pool_snapshot": ["source", "snapshot_date", "pool", "code"],
    "a_stock_option_contract_snapshot": ["source", "snapshot_date", "underlying", "option_month", "option_type", "option_code"],
    "a_stock_concept_membership": ["source", "snapshot_date", "code", "concept_code", "concept_name"],
}


class AStockPostgresRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_tables(self, path: Path = DDL_PATH) -> int:
        count = 0
        with self.client.engine.begin() as conn:
            for statement in _split_sql(path.read_text(encoding="utf-8")):
                conn.execute(text(statement))
                count += 1
        return count

    def upsert_batches(self, batches: dict[str, list[dict[str, Any]]]) -> int:
        total = 0
        for table, rows in batches.items():
            total += self.upsert_many(table, rows)
        return total

    def upsert_many(self, table: str, rows: list[dict[str, Any]]) -> int:
        if not rows:
            return 0
        if table not in TABLE_KEYS:
            raise ValueError(f"unsupported A-stock Postgres table: {table}")
        key_columns = TABLE_KEYS[table]
        total = 0
        with self.client.session() as db:
            for raw in rows:
                record = _prepare_record(raw, key_columns=key_columns)
                columns = list(record.keys())
                values = ", ".join(_placeholder(column) for column in columns)
                updates = ", ".join(f"{column}=EXCLUDED.{column}" for column in columns if column != "id")
                sql = text(
                    f"""
                    INSERT INTO {self.client.schema}.{table} ({", ".join(columns)})
                    VALUES ({values})
                    ON CONFLICT ({", ".join(key_columns)}) DO UPDATE SET
                        {updates}
                    """
                )
                db.execute(sql, record)
                total += 1
        return total


def _prepare_record(raw: dict[str, Any], *, key_columns: list[str]) -> dict[str, Any]:
    record = {key: value for key, value in raw.items() if value is not None or key in key_columns}
    for column in key_columns:
        if column not in record:
            raise ValueError(f"A-stock upsert record missing key column {column}: {raw}")
    record.setdefault("id", next_bigint_id())
    for column in JSONB_COLUMNS:
        if column in record:
            record[column] = json.dumps(record[column], ensure_ascii=False, default=str, separators=(",", ":"))
    return record


def _placeholder(column: str) -> str:
    if column in JSONB_COLUMNS:
        return f"CAST(:{column} AS jsonb)"
    return f":{column}"


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
