from __future__ import annotations

import json
from datetime import date
from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


class StockScreeningFactRepository:
    """Persistence boundary for the durable daily Screener projection."""

    SOURCE = "clickhouse.daily_raw"

    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def upsert_many(self, rows: list[dict[str, Any]], *, source: str | None = None) -> int:
        if not rows:
            return 0
        resolved_source = str(source or self.SOURCE)
        records = [_to_record(row, source=resolved_source) for row in rows]
        sql = text(
            f"""
            INSERT INTO {self.client.schema}.a_stock_daily_screening_fact (
                id, source, code, trade_date, open_price, high_price, low_price,
                close_price, prev_close, change_pct, amplitude_pct, volume, amount,
                turnover_rate_pct, total_market_cap, float_market_cap, adjusted_close,
                raw_payload, updated_at
            ) VALUES (
                :id, :source, :code, :trade_date, :open_price, :high_price, :low_price,
                :close_price, :prev_close, :change_pct, :amplitude_pct, :volume, :amount,
                :turnover_rate_pct, :total_market_cap, :float_market_cap, :adjusted_close,
                CAST(:raw_payload AS jsonb), now()
            )
            ON CONFLICT (source, code, trade_date) DO UPDATE SET
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                prev_close = EXCLUDED.prev_close,
                change_pct = EXCLUDED.change_pct,
                amplitude_pct = EXCLUDED.amplitude_pct,
                volume = EXCLUDED.volume,
                amount = EXCLUDED.amount,
                turnover_rate_pct = EXCLUDED.turnover_rate_pct,
                total_market_cap = EXCLUDED.total_market_cap,
                float_market_cap = EXCLUDED.float_market_cap,
                adjusted_close = EXCLUDED.adjusted_close,
                raw_payload = EXCLUDED.raw_payload,
                updated_at = now()
            """
        )
        with self.client.session() as db:
            db.execute(sql, records)
        return len(records)

    def replace_range(
        self,
        rows: list[dict[str, Any]],
        *,
        start_date: date | str,
        end_date: date | str,
        source: str | None = None,
        batch_size: int = 2_000,
    ) -> int:
        """Atomically replace one bounded projection range after its source read succeeds."""
        resolved_source = str(source or self.SOURCE)
        records = [_to_record(row, source=resolved_source) for row in rows]
        sql = text(
            f"""
            INSERT INTO {self.client.schema}.a_stock_daily_screening_fact (
                id, source, code, trade_date, open_price, high_price, low_price,
                close_price, prev_close, change_pct, amplitude_pct, volume, amount,
                turnover_rate_pct, total_market_cap, float_market_cap, adjusted_close,
                raw_payload, updated_at
            ) VALUES (
                :id, :source, :code, :trade_date, :open_price, :high_price, :low_price,
                :close_price, :prev_close, :change_pct, :amplitude_pct, :volume, :amount,
                :turnover_rate_pct, :total_market_cap, :float_market_cap, :adjusted_close,
                CAST(:raw_payload AS jsonb), now()
            )
            ON CONFLICT (source, code, trade_date) DO UPDATE SET
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                prev_close = EXCLUDED.prev_close,
                change_pct = EXCLUDED.change_pct,
                amplitude_pct = EXCLUDED.amplitude_pct,
                volume = EXCLUDED.volume,
                amount = EXCLUDED.amount,
                turnover_rate_pct = EXCLUDED.turnover_rate_pct,
                total_market_cap = EXCLUDED.total_market_cap,
                float_market_cap = EXCLUDED.float_market_cap,
                adjusted_close = EXCLUDED.adjusted_close,
                raw_payload = EXCLUDED.raw_payload,
                updated_at = now()
            """
        )
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    DELETE FROM {self.client.schema}.a_stock_daily_screening_fact
                    WHERE source = :source
                      AND trade_date BETWEEN :start_date AND :end_date
                    """
                ),
                {"source": resolved_source, "start_date": start_date, "end_date": end_date},
            )
            for offset in range(0, len(records), max(1, int(batch_size))):
                db.execute(sql, records[offset : offset + max(1, int(batch_size))])
        return len(records)

    def count_rows(self, *, start_date: date | str, end_date: date | str, source: str | None = None) -> int:
        with self.client.engine.connect() as connection:
            row = connection.execute(
                text(
                    f"""
                    SELECT count(*)
                    FROM {self.client.schema}.a_stock_daily_screening_fact
                    WHERE source = :source
                      AND trade_date BETWEEN :start_date AND :end_date
                    """
                ),
                {"source": str(source or self.SOURCE), "start_date": start_date, "end_date": end_date},
            ).scalar_one()
        return int(row)


def _to_record(row: dict[str, Any], *, source: str) -> dict[str, Any]:
    code = str(row["code"]).strip()
    trade_date = str(row["trade_date"])[:10]
    if len(code) != 6 or not code.isdigit():
        raise ValueError(f"screening fact requires a six-digit stock code: {row}")
    raw_payload = {
        "market": row.get("market"),
        "source_table": "daily_raw",
        "projection_version": 1,
    }
    return {
        "id": stable_bigint_id("a_stock_daily_screening_fact", source, code, trade_date),
        "source": source,
        "code": code,
        "trade_date": trade_date,
        "open_price": row.get("open_price"),
        "high_price": row.get("high_price"),
        "low_price": row.get("low_price"),
        "close_price": row.get("close_price"),
        "prev_close": row.get("prev_close"),
        "change_pct": row.get("change_pct"),
        "amplitude_pct": row.get("amplitude_pct"),
        "volume": row.get("volume"),
        "amount": row.get("amount"),
        "turnover_rate_pct": row.get("turnover_rate_pct"),
        "total_market_cap": row.get("total_market_cap"),
        "float_market_cap": row.get("float_market_cap"),
        "adjusted_close": row.get("adjusted_close"),
        "raw_payload": json.dumps(raw_payload, ensure_ascii=False, separators=(",", ":")),
    }
