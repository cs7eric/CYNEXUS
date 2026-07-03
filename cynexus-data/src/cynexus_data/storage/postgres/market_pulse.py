from __future__ import annotations

import json
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_pulse_tables.sql"
CAPTURE_TABLE = "market_pulse_capture_batches"
SNAPSHOT_TABLE = "market_pulse_sector_daily_snapshots"


class MarketPulseRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))
        self.ensure_runtime_compat()

    def ensure_runtime_compat(self) -> None:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    DO $$
                    DECLARE
                        constraint_def text;
                    BEGIN
                        SELECT pg_get_constraintdef(c.oid)
                          INTO constraint_def
                          FROM pg_constraint c
                          JOIN pg_class t ON c.conrelid = t.oid
                          JOIN pg_namespace n ON t.relnamespace = n.oid
                         WHERE n.nspname = '{self.client.schema}'
                           AND t.relname = '{CAPTURE_TABLE}'
                           AND c.conname = 'ck_market_pulse_capture_batches_source_kind';
                        IF constraint_def IS NULL OR position('fund_flow_snapshot' in constraint_def) = 0 THEN
                            ALTER TABLE {self.client.schema}.{CAPTURE_TABLE}
                                DROP CONSTRAINT IF EXISTS ck_market_pulse_capture_batches_source_kind;
                            ALTER TABLE {self.client.schema}.{CAPTURE_TABLE}
                                ADD CONSTRAINT ck_market_pulse_capture_batches_source_kind
                                CHECK (source_kind IN ('fund_flow_snapshot', 'live_capture', 'duckdb_import', 'json_import'));
                        END IF;
                    END $$;
                    """
                )
            )

    def replace_snapshot(
        self,
        *,
        trade_date: date,
        rows: list[dict[str, Any]],
        source_kind: str = "fund_flow_snapshot",
        source_name: str = "sector_fund_flow_daily_snapshots",
        fetched_at: datetime | None = None,
        extra: dict[str, Any] | None = None,
    ) -> int:
        self.ensure_runtime_compat()
        captured_at = fetched_at or datetime.now(timezone.utc)
        status = "success" if len(rows) >= 80 else "partial"
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{SNAPSHOT_TABLE}
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{CAPTURE_TABLE}
                       SET deleted_at = now(), updated_at = now(), status = 'partial'
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            batch_id = db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.{CAPTURE_TABLE} (
                        id, trade_date, source_kind, status, source_name, row_count, fetched_at, extra, updated_at
                    ) VALUES (
                        :id, :trade_date, :source_kind, :status, :source_name, :row_count, :fetched_at,
                        CAST(:extra AS jsonb), now()
                    )
                    RETURNING id
                    """
                ),
                {
                    "trade_date": trade_date,
                    "id": next_bigint_id(),
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
                        INSERT INTO {self.client.schema}.{SNAPSHOT_TABLE} (
                            id, batch_id, trade_date, sector_name, sector_index, rank_by_change, change_pct,
                            inflow, outflow, main_net, stock_count, leading_stock, leading_change_pct,
                            leading_price, source_kind, source_name, captured_at, extra, updated_at
                        ) VALUES (
                            :id, :batch_id, :trade_date, :sector_name, :sector_index, :rank_by_change, :change_pct,
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
                      FROM {self.client.schema}.{SNAPSHOT_TABLE}
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     ORDER BY rank_by_change ASC NULLS LAST, change_pct DESC NULLS LAST, sector_name ASC
                    """
                ),
                {"trade_date": trade_date},
            ).mappings().all()
        return [dict(row) for row in rows]


def _row_params(row: dict[str, Any], *, batch_id: Any, source_kind: str, source_name: str, captured_at: datetime) -> dict[str, Any]:
    payload = dict(row)
    payload["id"] = next_bigint_id()
    payload["batch_id"] = batch_id
    payload["source_kind"] = source_kind
    payload["source_name"] = source_name
    payload["captured_at"] = captured_at
    payload["extra"] = json.dumps(payload.get("extra") or {}, ensure_ascii=False, default=str)
    return payload
