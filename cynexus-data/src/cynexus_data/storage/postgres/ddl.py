from __future__ import annotations

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


def init_job_executor_tables(client: PostgresClient) -> None:
    client.apply_sql_file(PROJECT_ROOT / "sql" / "postgres" / "20260628_create_job_executor_tables.sql")

