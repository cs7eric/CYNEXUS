from __future__ import annotations

from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_stock_sector_tables.sql"


class StockSectorRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_stock_mappings(self, code: str, mappings: list[dict[str, Any]]) -> int:
        with self.client.session() as db:
            db.execute(text(f"DELETE FROM {self.client.schema}.sec_stock_sector WHERE code=:code"), {"code": code})
            for mapping in mappings:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.sec_sector (
                            sector_id, category_raw, category_label, name, topic_id, source, updated_at
                        ) VALUES (
                            :sector_id, :category_raw, :category_label, :name, :topic_id, :source, now()
                        )
                        ON CONFLICT (sector_id) DO UPDATE SET
                            category_raw = EXCLUDED.category_raw,
                            category_label = EXCLUDED.category_label,
                            name = EXCLUDED.name,
                            topic_id = EXCLUDED.topic_id,
                            source = EXCLUDED.source,
                            updated_at = now()
                        """
                    ),
                    mapping,
                )
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.sec_stock_sector (
                            code, sector_id, relation_level, reason, source, updated_at
                        ) VALUES (
                            :code, :sector_id, :relation_level, :reason, :source, now()
                        )
                        ON CONFLICT (code, sector_id) DO UPDATE SET
                            relation_level = EXCLUDED.relation_level,
                            reason = EXCLUDED.reason,
                            source = EXCLUDED.source,
                            updated_at = now()
                        """
                    ),
                    mapping,
                )
        return len(mappings)
