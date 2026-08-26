from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260811_create_market_indicator_limit_board_session_summaries.sql"
TABLE = "market_indicator_limit_board_session_summaries"


class MarketLimitBoardSessionRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def save_summaries(self, rows: list[dict[str, Any]], *, source: str = "eltdx.quotes") -> int:
        if not rows:
            return 0
        columns = _columns()
        assignments = ", ".join(
            f"{column} = EXCLUDED.{column}"
            for column in columns
            if column not in {"id", "trade_date", "trade_session", "code", "created_at"}
        )
        assignments += ", updated_at = now(), deleted_at = NULL"
        values = ", ".join("CAST(:trend_features AS jsonb)" if column == "trend_features" else f":{column}" for column in columns)
        with self.client.session() as db:
            for row in rows:
                db.execute(
                    text(f"""
                        INSERT INTO {self.client.schema}.{TABLE} ({", ".join(columns)})
                        VALUES ({values})
                        ON CONFLICT (trade_date, trade_session, code) WHERE deleted_at IS NULL
                        DO UPDATE SET {assignments}
                    """),
                    _summary_params(row, source=source),
                )
        return len(rows)


def _columns() -> list[str]:
    return [
        "id", "trade_date", "trade_session", "code", "name", "market", "direction", "closing_status",
        "is_sealed_at_close", "first_touch_at", "first_sealed_at", "last_sealed_at", "last_observed_at",
        "sealed_duration_seconds", "open_count", "reseal_count", "sample_count", "valid_sample_count",
        "gap_count", "coverage_ratio", "max_sealed_amount", "avg_sealed_amount", "median_sealed_amount",
        "peak_sealed_amount_at", "first_sealed_amount", "last_sealed_amount",
        "last_turnover_amount", "last_sealed_turnover_ratio", "max_sealed_turnover_ratio",
        "sealed_amount_change",
        "sealed_amount_slope", "trend_direction", "trend_series_origin_at", "trend_time_offsets",
        "trend_sealed_amounts", "source", "algorithm_version", "trend_features",
    ]


def _summary_params(row: dict[str, Any], *, source: str) -> dict[str, Any]:
    trade_date = row["trade_date"]
    trade_session = str(row["trade_session"])
    code = str(row.get("code") or "").strip()[-6:].zfill(6)
    params = {column: row.get(column) for column in _columns()}
    params.update({
        "id": stable_bigint_id("market_indicator_limit_board_session", trade_date, trade_session, code),
        "trade_date": trade_date,
        "trade_session": trade_session,
        "code": code,
        "source": row.get("source") or source,
        "trend_time_offsets": list(row.get("trend_time_offsets") or []),
        "trend_sealed_amounts": list(row.get("trend_sealed_amounts") or []),
        "trend_features": json.dumps(row.get("trend_features") or {}, ensure_ascii=False, default=str),
    })
    return params
