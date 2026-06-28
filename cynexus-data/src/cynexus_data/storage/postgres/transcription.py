from __future__ import annotations

import json
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_job_executor_tables.sql"


class TranscriptRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(DDL_PATH)

    def get(self, transcript_id: str) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT transcript_id, source_uri, transcript, language, duration_seconds, metadata, created_at
                      FROM {self.client.schema}.media_transcript
                     WHERE transcript_id = :transcript_id
                    """
                ),
                {"transcript_id": transcript_id},
            ).mappings().first()
        return dict(row) if row else None

    def upsert(self, record: dict[str, Any]) -> int:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.media_transcript (
                        transcript_id, source_uri, transcript, language, duration_seconds, metadata, created_at
                    ) VALUES (
                        :transcript_id, :source_uri, :transcript, :language, :duration_seconds,
                        CAST(:metadata AS jsonb), now()
                    )
                    ON CONFLICT (transcript_id) DO UPDATE SET
                        source_uri = EXCLUDED.source_uri,
                        transcript = EXCLUDED.transcript,
                        language = EXCLUDED.language,
                        duration_seconds = EXCLUDED.duration_seconds,
                        metadata = EXCLUDED.metadata
                    """
                ),
                {
                    "transcript_id": record["transcript_id"],
                    "source_uri": record["source_uri"],
                    "transcript": record["transcript"],
                    "language": record.get("language"),
                    "duration_seconds": record.get("duration_seconds"),
                    "metadata": json.dumps(record.get("metadata") or {}, ensure_ascii=False, default=str),
                },
            )
        return 1
