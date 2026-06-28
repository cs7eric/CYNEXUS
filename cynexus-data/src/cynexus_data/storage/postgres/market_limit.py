from __future__ import annotations

from datetime import date
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_limit_tables.sql"
STOCK_TABLE = "market_limit_daily_stocks"


class MarketLimitRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_stock_snapshots(
        self,
        *,
        trade_date: date,
        rows: list[dict[str, Any]],
        source: str = "clickhouse.daily_raw",
    ) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{STOCK_TABLE}
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date
                       AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            count = 0
            for row in rows:
                code = str(row.get("code") or "").zfill(6)
                category = str(row.get("category") or "")
                if not code or code == "000000" or not category:
                    continue
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.{STOCK_TABLE} (
                            id, trade_date, code, name, category, streak, change_pct,
                            limit_up_price, limit_down_price, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :code, :name, :category, :streak, :change_pct,
                            :limit_up_price, :limit_down_price, now(), NULL
                        )
                        ON CONFLICT (trade_date, code) WHERE deleted_at IS NULL DO UPDATE SET
                            name = EXCLUDED.name,
                            category = EXCLUDED.category,
                            streak = EXCLUDED.streak,
                            change_pct = EXCLUDED.change_pct,
                            limit_up_price = EXCLUDED.limit_up_price,
                            limit_down_price = EXCLUDED.limit_down_price,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "id": next_bigint_id(),
                        "trade_date": trade_date,
                        "code": code,
                        "name": row.get("name"),
                        "category": category,
                        "streak": row.get("streak"),
                        "change_pct": row.get("changePct"),
                        "limit_up_price": row.get("limitUpPrice"),
                        "limit_down_price": row.get("limitDownPrice"),
                    },
                )
                count += 1
        return count

    def get_stock_snapshots(
        self,
        *,
        trade_date: date,
        category: str | None = None,
        source: str = "clickhouse.daily_raw",
    ) -> list[dict[str, Any]]:
        category_sql = "AND category = :category" if category else ""
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT trade_date, code, name, category, streak, change_pct,
                           limit_up_price, limit_down_price
                      FROM {self.client.schema}.{STOCK_TABLE}
                     WHERE trade_date = :trade_date
                       AND deleted_at IS NULL
                       {category_sql}
                     ORDER BY streak DESC NULLS LAST, code
                    """
                ),
                {"trade_date": trade_date, "category": category},
            ).mappings().all()
        return [dict(row) for row in rows]
