from __future__ import annotations

from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_stock_basic_tables.sql"


class TradeCalendarRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def upsert_many(self, records: list[dict[str, Any]], *, source: str = "local_rules") -> int:
        if not records:
            return 0
        with self.client.session() as db:
            for record in records:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.sec_trade_calendar (
                            id, trade_date, is_trading_day, reason, source, ingested_at, updated_at
                        ) VALUES (
                            :id, :trade_date, :is_trading_day, :reason, :source, now(), now()
                        )
                        ON CONFLICT (trade_date) DO UPDATE SET
                            is_trading_day = EXCLUDED.is_trading_day,
                            reason = EXCLUDED.reason,
                            source = EXCLUDED.source,
                            updated_at = now()
                        """
                    ),
                    {**record, "id": next_bigint_id(), "source": source},
                )
        return len(records)
