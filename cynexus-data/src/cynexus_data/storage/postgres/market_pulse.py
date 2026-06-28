from __future__ import annotations

import json
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_pulse_tables.sql"


class MarketPulseRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshot(
        self,
        *,
        trade_date: date,
        rows: list[dict[str, Any]],
        source_kind: str = "fund_flow_snapshot",
        source_name: str = "sector_fund_flow_daily_snapshot",
        fetched_at: datetime | None = None,
        extra: dict[str, Any] | None = None,
    ) -> int:
        captured_at = fetched_at or datetime.now(timezone.utc)
        status = "success" if len(rows) >= 80 else "partial"
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.market_pulse_sector_daily_snapshot
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.market_pulse_capture_batch
                       SET deleted_at = now(), updated_at = now(), status = 'partial'
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            batch_id = db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.market_pulse_capture_batch (
                        trade_date, source_kind, status, source_name, row_count, fetched_at, extra, updated_at
                    ) VALUES (
                        :trade_date, :source_kind, :status, :source_name, :row_count, :fetched_at,
                        CAST(:extra AS jsonb), now()
                    )
                    RETURNING batch_id
                    """
                ),
                {
                    "trade_date": trade_date,
                    "source_kind": source_kind,
                    "status": status,
                    "source_name": source_name,
                    "row_count": len(rows),
                    "fetched_at": captured_at,
                    "extra": json.dumps(extra or {}, ensure_ascii=False, default=str),
                },
            ).scalar_one()
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.market_pulse_sector_daily_snapshot (
                            batch_id, trade_date, sector_name, sector_index, rank_by_change, change_pct,
                            inflow, outflow, main_net, stock_count, leading_stock, leading_change_pct,
                            leading_price, source_kind, source_name, captured_at, extra, updated_at
                        ) VALUES (
                            :batch_id, :trade_date, :sector_name, :sector_index, :rank_by_change, :change_pct,
                            :inflow, :outflow, :main_net, :stock_count, :leading_stock, :leading_change_pct,
                            :leading_price, :source_kind, :source_name, :captured_at, CAST(:extra AS jsonb), now()
                        )
                        """
                    ),
                    _row_params(row, batch_id=batch_id, source_kind=source_kind, source_name=source_name, captured_at=captured_at),
                )
        return len(rows)

    def get_rows(self, trade_date: date) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT trade_date, sector_name, sector_index, rank_by_change, change_pct, inflow,
                           outflow, main_net, stock_count, leading_stock, leading_change_pct,
                           leading_price, source_kind, source_name
                      FROM {self.client.schema}.market_pulse_sector_daily_snapshot
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     ORDER BY rank_by_change ASC NULLS LAST, change_pct DESC NULLS LAST, sector_name ASC
                    """
                ),
                {"trade_date": trade_date},
            ).mappings().all()
        return [dict(row) for row in rows]


def _row_params(row: dict[str, Any], *, batch_id: Any, source_kind: str, source_name: str, captured_at: datetime) -> dict[str, Any]:
    payload = dict(row)
    payload["batch_id"] = batch_id
    payload["source_kind"] = source_kind
    payload["source_name"] = source_name
    payload["captured_at"] = captured_at
    payload["extra"] = json.dumps(payload.get("extra") or {}, ensure_ascii=False, default=str)
    return payload
