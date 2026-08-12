from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260811_create_market_indicator_limit_board_snapshots.sql"
TABLE = "market_indicator_limit_board_snapshots"


class MarketLimitBoardRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def save_snapshots(self, rows: list[dict[str, Any]], *, source: str = "eltdx.quotes") -> int:
        if not rows:
            return 0
        columns = _columns()
        assignments = ", ".join(f"{column} = EXCLUDED.{column}" for column in columns if column not in {"id", "trade_date", "code", "captured_at"})
        assignments += ", updated_at = now(), deleted_at = NULL"
        values = ", ".join("CAST(:payload AS jsonb)" if column == "payload" else f":{column}" for column in columns)
        with self.client.session() as db:
            for row in rows:
                params = _snapshot_params(row, source=source)
                db.execute(
                    text(f"""
                        INSERT INTO {self.client.schema}.{TABLE} ({", ".join(columns)})
                        VALUES ({values})
                        ON CONFLICT (trade_date, captured_at, code) WHERE deleted_at IS NULL
                        DO UPDATE SET {assignments}
                    """),
                    params,
                )
        return len(rows)


def _columns() -> list[str]:
    columns = ["id", "trade_date", "code", "name", "market", "direction", "board_status", "is_sealed",
               "last_price", "prev_close", "change_pct", "limit_price", "queue_side", "queue_price",
               "queue_volume", "sealed_amount"]
    columns.extend(f"{side}_{field}_1" for side in ("bid", "ask") for field in ("price", "volume"))
    columns.extend(["provider_time_raw", "source", "captured_at", "payload"])
    return columns


def _snapshot_params(row: dict[str, Any], *, source: str) -> dict[str, Any]:
    captured_at = row["captured_at"]
    code = str(row.get("code") or "").strip()[-6:].zfill(6)
    params = {column: row.get(column) for column in _columns()}
    params.update({
        "id": stable_bigint_id("market_indicator_limit_board", captured_at, code),
        "trade_date": captured_at.date(),
        "code": code,
        "source": source,
        "payload": json.dumps(row.get("payload") or {}, ensure_ascii=False, default=str),
    })
    return params
