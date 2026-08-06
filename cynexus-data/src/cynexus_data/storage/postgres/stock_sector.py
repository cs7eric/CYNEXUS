from __future__ import annotations

from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


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
                            id, sector_id, category_raw, category_label, name, topic_id, source, updated_at
                        ) VALUES (
                            :id, :sector_id, :category_raw, :category_label, :name, :topic_id, :source, now()
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
                    {**mapping, "id": stable_bigint_id("sec_sector", mapping["sector_id"])},
                )
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.sec_stock_sector (
                            id, code, sector_id, relation_level, reason, source, updated_at
                        ) VALUES (
                            :id, :code, :sector_id, :relation_level, :reason, :source, now()
                        )
                        ON CONFLICT (code, sector_id) DO UPDATE SET
                            relation_level = EXCLUDED.relation_level,
                            reason = EXCLUDED.reason,
                            source = EXCLUDED.source,
                            updated_at = now()
                        """
                    ),
                    {**mapping, "id": stable_bigint_id("sec_stock_sector", code, mapping["sector_id"])},
                )
        return len(mappings)

    def list_sectors_by_category(self, category_raw: int) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT s.sector_id, s.category_raw, s.category_label, s.name, s.topic_id,
                           array_agg(ss.code ORDER BY ss.code) AS stock_codes,
                           count(ss.code) AS stock_count
                      FROM {self.client.schema}.sec_sector s
                      LEFT JOIN {self.client.schema}.sec_stock_sector ss
                        ON ss.sector_id = s.sector_id
                     WHERE s.category_raw = :category_raw
                     GROUP BY s.sector_id, s.category_raw, s.category_label, s.name, s.topic_id
                     ORDER BY count(ss.code) DESC, s.name
                    """
                ),
                {"category_raw": category_raw},
            ).mappings().all()
        return [
            {
                "sector_id": row["sector_id"],
                "category_raw": row["category_raw"],
                "category_label": row["category_label"],
                "name": row["name"],
                "topic_id": row["topic_id"],
                "stock_codes": list(row["stock_codes"] or []),
                "stock_count": int(row["stock_count"] or 0),
            }
            for row in rows
        ]
