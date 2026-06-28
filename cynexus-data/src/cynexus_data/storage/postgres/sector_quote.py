from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_sector_quote_detail_tables.sql"


class TdxSectorQuoteDetailRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshots(
        self,
        *,
        snapshot_date,
        rows: list[dict[str, Any]],
        source: str = "eltdx.f10.theme_market",
    ) -> int:
        code6_values = [str(row.get("code6") or "") for row in rows if row.get("code6")]
        with self.client.session() as db:
            if code6_values:
                db.execute(
                    text(
                        f"""
                        UPDATE {self.client.schema}.tdx_sector_quote_detail_snapshot
                           SET deleted_at = now(), updated_at = now()
                         WHERE snapshot_date = :snapshot_date
                           AND source = :source
                           AND code6 = ANY(:code6_values)
                           AND deleted_at IS NULL
                        """
                    ),
                    {"snapshot_date": snapshot_date, "source": source, "code6_values": code6_values},
                )
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.tdx_sector_quote_detail_snapshot (
                            snapshot_date, code6, name, seed_code, source, payload,
                            section_counts, generated_at, updated_at, deleted_at
                        ) VALUES (
                            :snapshot_date, :code6, :name, :seed_code, :source, CAST(:payload AS jsonb),
                            CAST(:section_counts AS jsonb), now(), now(), NULL
                        )
                        ON CONFLICT (snapshot_date, code6, source) DO UPDATE SET
                            name = EXCLUDED.name,
                            seed_code = EXCLUDED.seed_code,
                            payload = EXCLUDED.payload,
                            section_counts = EXCLUDED.section_counts,
                            generated_at = EXCLUDED.generated_at,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    {
                        "snapshot_date": snapshot_date,
                        "code6": row.get("code6"),
                        "name": row.get("name"),
                        "seed_code": row.get("seed_code"),
                        "source": source,
                        "payload": json.dumps(row.get("payload") or {}, ensure_ascii=False, default=str),
                        "section_counts": json.dumps(row.get("section_counts") or {}, ensure_ascii=False, default=str),
                    },
                )
        return len(rows)

    def get_snapshot(self, snapshot_date, *, code6: str, source: str = "eltdx.f10.theme_market") -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT snapshot_date, code6, name, seed_code, source, payload, section_counts, generated_at
                      FROM {self.client.schema}.tdx_sector_quote_detail_snapshot
                     WHERE snapshot_date = :snapshot_date
                       AND code6 = :code6
                       AND source = :source
                       AND deleted_at IS NULL
                    """
                ),
                {"snapshot_date": snapshot_date, "code6": code6, "source": source},
            ).mappings().first()
        return dict(row) if row else None
