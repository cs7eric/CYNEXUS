from __future__ import annotations

import json
from datetime import date
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_style_sector_tables.sql"
TABLE_NAME = "mkt_style_sector_snapshot"


class StyleSectorRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshots(self, *, trade_date: date, rows: list[dict[str, Any]], source: str = "tencent.qt") -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{TABLE_NAME}
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date
                       AND source = :source
                       AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date, "source": source},
            )
            count = 0
            for rank, row in enumerate(rows, start=1):
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.{TABLE_NAME} (
                            id, trade_date, name, rank_by_change, change_pct, valid_size,
                            sample_size, source, payload, ingested_at, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :name, :rank_by_change, :change_pct, :valid_size,
                            :sample_size, :source, CAST(:payload AS jsonb), now(), now(), NULL
                        )
                        ON CONFLICT (trade_date, name, source) WHERE deleted_at IS NULL DO UPDATE SET
                            rank_by_change = EXCLUDED.rank_by_change,
                            change_pct = EXCLUDED.change_pct,
                            valid_size = EXCLUDED.valid_size,
                            sample_size = EXCLUDED.sample_size,
                            payload = EXCLUDED.payload,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "trade_date": trade_date,
                        "id": next_bigint_id(),
                        "name": row.get("name"),
                        "rank_by_change": rank,
                        "change_pct": row.get("change_pct"),
                        "valid_size": int(row.get("valid_size") or 0),
                        "sample_size": int(row.get("sample_size") or 0),
                        "source": source,
                        "payload": json.dumps(row, ensure_ascii=False, default=str),
                    },
                )
                count += 1
        return count
