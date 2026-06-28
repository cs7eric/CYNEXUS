from __future__ import annotations

from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_tdx_industry_quote_tables.sql"


class TdxIndustryQuoteRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshot(
        self,
        snapshot_date: date,
        rows: list[dict[str, Any]],
        *,
        source: str = "eltdx",
        batch_id: str | None = None,
    ) -> dict[str, Any]:
        batch = batch_id or f"tdx_industry_quote_{snapshot_date.isoformat()}_{uuid4().hex[:12]}"
        fetched_at = datetime.now(timezone.utc)
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.tdx_industry_quote_snapshot
                       SET deleted_at = now(), updated_at = now()
                     WHERE snapshot_date = :snapshot_date AND source = :source AND deleted_at IS NULL
                    """
                ),
                {"snapshot_date": snapshot_date, "source": source},
            )
            batch_pk = db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.tdx_industry_quote_batch (
                        id, batch_key, snapshot_date, source, row_count, fetched_at, updated_at, deleted_at
                    ) VALUES (
                        :id, :batch_key, :snapshot_date, :source, :row_count, :fetched_at, now(), NULL
                    )
                    ON CONFLICT (batch_key) WHERE deleted_at IS NULL DO UPDATE SET
                        row_count = EXCLUDED.row_count,
                        fetched_at = EXCLUDED.fetched_at,
                        updated_at = now(),
                        deleted_at = NULL
                    RETURNING id
                    """
                ),
                {"id": next_bigint_id(), "batch_key": batch, "snapshot_date": snapshot_date, "source": source, "row_count": len(rows), "fetched_at": fetched_at},
            ).scalar_one()
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.tdx_industry_quote_snapshot (
                            id, batch_id, snapshot_date, code6, name, full_code, exchange,
                            last_price, pre_close_price, open_price, high_price, low_price,
                            change, change_pct, amount, total_hand, current_hand,
                            source, fetched_at, updated_at, deleted_at
                        ) VALUES (
                            :id, :batch_id, :snapshot_date, :code6, :name, :full_code, :exchange,
                            :last_price, :pre_close_price, :open_price, :high_price, :low_price,
                            :change, :change_pct, :amount, :total_hand, :current_hand,
                            :source, :fetched_at, now(), NULL
                        )
                        ON CONFLICT (snapshot_date, code6, source) WHERE deleted_at IS NULL DO UPDATE SET
                            batch_id = EXCLUDED.batch_id,
                            name = EXCLUDED.name,
                            full_code = EXCLUDED.full_code,
                            exchange = EXCLUDED.exchange,
                            last_price = EXCLUDED.last_price,
                            pre_close_price = EXCLUDED.pre_close_price,
                            open_price = EXCLUDED.open_price,
                            high_price = EXCLUDED.high_price,
                            low_price = EXCLUDED.low_price,
                            change = EXCLUDED.change,
                            change_pct = EXCLUDED.change_pct,
                            amount = EXCLUDED.amount,
                            total_hand = EXCLUDED.total_hand,
                            current_hand = EXCLUDED.current_hand,
                            fetched_at = EXCLUDED.fetched_at,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "id": next_bigint_id(),
                        "batch_id": batch_pk,
                        "snapshot_date": snapshot_date,
                        "code6": str(row.get("code6") or ""),
                        "name": str(row.get("name") or ""),
                        "full_code": str(row.get("full_code") or ""),
                        "exchange": row.get("exchange"),
                        "last_price": row.get("last_price"),
                        "pre_close_price": row.get("pre_close_price"),
                        "open_price": row.get("open_price"),
                        "high_price": row.get("high_price"),
                        "low_price": row.get("low_price"),
                        "change": row.get("change"),
                        "change_pct": row.get("change_pct"),
                        "amount": row.get("amount"),
                        "total_hand": row.get("total_hand"),
                        "current_hand": row.get("current_hand"),
                        "source": source,
                        "fetched_at": fetched_at,
                    },
                )
        return {"batch_id": batch, "snapshot_date": snapshot_date.isoformat(), "rows_written": len(rows), "source": source}

    def get_snapshot(self, snapshot_date: date, *, source: str = "eltdx") -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT code6, name, full_code, exchange, last_price, pre_close_price,
                           open_price, high_price, low_price, change, change_pct,
                           amount, total_hand, current_hand, source
                      FROM {self.client.schema}.tdx_industry_quote_snapshot
                     WHERE snapshot_date = :snapshot_date AND source = :source AND deleted_at IS NULL
                     ORDER BY change_pct DESC NULLS LAST, code6 ASC
                    """
                ),
                {"snapshot_date": snapshot_date, "source": source},
            ).all()
        return [
            {
                "code6": row[0],
                "name": row[1],
                "full_code": row[2],
                "exchange": row[3],
                "last_price": _float_or_none(row[4]),
                "pre_close_price": _float_or_none(row[5]),
                "open_price": _float_or_none(row[6]),
                "high_price": _float_or_none(row[7]),
                "low_price": _float_or_none(row[8]),
                "change": _float_or_none(row[9]),
                "change_pct": _float_or_none(row[10]),
                "amount": _float_or_none(row[11]),
                "total_hand": _float_or_none(row[12]),
                "current_hand": _float_or_none(row[13]),
                "source": row[14],
            }
            for row in rows
        ]


def _float_or_none(value: Any) -> float | None:
    return float(value) if value is not None else None
