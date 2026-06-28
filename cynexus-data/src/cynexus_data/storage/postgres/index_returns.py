from __future__ import annotations

from datetime import date
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_index_return_tables.sql"


class IndexReturnRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def save_many(self, *, trade_date: date, window_days: int, items: list[dict[str, Any]], source: str = "clickhouse.index_daily_raw") -> int:
        count = 0
        with self.client.session() as db:
            for item in items:
                if not item.get("available") or item.get("currentDate") is None:
                    continue
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.mkt_index_return_daily (
                            trade_date, index_code, index_name, window_days, current_close,
                            current_trade_date, base_close, base_date, return_pct, source,
                            ingested_at, updated_at, deleted_at
                        ) VALUES (
                            :trade_date, :index_code, :index_name, :window_days, :current_close,
                            :current_trade_date, :base_close, :base_date, :return_pct, :source,
                            now(), now(), NULL
                        )
                        ON CONFLICT (trade_date, index_code, window_days) DO UPDATE SET
                            index_name = EXCLUDED.index_name,
                            current_close = EXCLUDED.current_close,
                            current_trade_date = EXCLUDED.current_trade_date,
                            base_close = EXCLUDED.base_close,
                            base_date = EXCLUDED.base_date,
                            return_pct = EXCLUDED.return_pct,
                            source = EXCLUDED.source,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "trade_date": trade_date,
                        "index_code": str(item.get("fullCode") or ""),
                        "index_name": str(item.get("name") or ""),
                        "window_days": int(window_days),
                        "current_close": item.get("current"),
                        "current_trade_date": _date_or_none(item.get("currentDate")),
                        "base_close": item.get("baseClose"),
                        "base_date": _date_or_none(item.get("baseDate")),
                        "return_pct": item.get("returnPct"),
                        "source": source,
                    },
                )
                count += 1
        return count

    def get_many(self, *, trade_date: date, window_days: int) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT index_code, index_name, current_close, current_trade_date,
                           base_close, base_date, return_pct
                      FROM {self.client.schema}.mkt_index_return_daily
                     WHERE trade_date = :trade_date
                       AND window_days = :window_days
                       AND deleted_at IS NULL
                     ORDER BY index_code ASC
                    """
                ),
                {"trade_date": trade_date, "window_days": int(window_days)},
            ).all()
        return [
            {
                "name": row[1],
                "code": _short_code(row[0]),
                "fullCode": row[0],
                "current": _float_or_none(row[2]),
                "currentDate": row[3].isoformat() if row[3] else None,
                "baseClose": _float_or_none(row[4]),
                "baseDate": row[5].isoformat() if row[5] else None,
                "returnPct": round(float(row[6]), 4) if row[6] is not None else None,
                "available": True,
                "fromCache": True,
            }
            for row in rows
        ]


def _date_or_none(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _float_or_none(value: Any) -> float | None:
    return float(value) if value is not None else None


def _short_code(full_code: str | None) -> str:
    if not full_code:
        return ""
    value = str(full_code)
    return value[2:] if value[:2].lower() in {"sh", "sz", "bj"} else value
