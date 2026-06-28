from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_heatmap_tables.sql"


class MarketHeatmapRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def upsert_snapshot(
        self,
        *,
        snapshot_date,
        kind: str,
        source: str,
        payload: dict[str, Any],
    ) -> dict[str, Any]:
        item_count = len(payload.get("items") or [])
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.market_heatmap_snapshot (
                        snapshot_date, kind, source, payload, item_count,
                        generated_at, updated_at, deleted_at
                    ) VALUES (
                        :snapshot_date, :kind, :source, CAST(:payload AS jsonb), :item_count,
                        now(), now(), NULL
                    )
                    ON CONFLICT (snapshot_date, kind, source) DO UPDATE SET
                        payload = EXCLUDED.payload,
                        item_count = EXCLUDED.item_count,
                        generated_at = EXCLUDED.generated_at,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "snapshot_date": snapshot_date,
                    "kind": kind,
                    "source": source,
                    "payload": json.dumps(payload, ensure_ascii=False, default=str),
                    "item_count": item_count,
                },
            )
        return self.get_snapshot(snapshot_date, kind=kind, source=source) or {
            "snapshot_date": snapshot_date,
            "kind": kind,
            "source": source,
            "payload": payload,
            "item_count": item_count,
        }

    def get_snapshot(self, snapshot_date, *, kind: str, source: str) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT snapshot_date, kind, source, payload, item_count, generated_at
                      FROM {self.client.schema}.market_heatmap_snapshot
                     WHERE snapshot_date = :snapshot_date
                       AND kind = :kind
                       AND source = :source
                       AND deleted_at IS NULL
                    """
                ),
                {"snapshot_date": snapshot_date, "kind": kind, "source": source},
            ).mappings().first()
        return dict(row) if row else None
