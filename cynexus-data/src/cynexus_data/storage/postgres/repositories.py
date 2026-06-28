from __future__ import annotations

from cynexus_data.storage.postgres.client import PostgresClient


class JobRunRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def get(self, run_id: str) -> dict | None:
        return self.client.get_job_run(run_id)

