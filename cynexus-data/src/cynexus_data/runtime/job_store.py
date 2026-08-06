from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

from cynexus_data.jobs.contracts import JobRequest, JobResult


@dataclass
class JobStore:
    _client: Any | None = None
    _runs: dict[str, dict[str, Any]] = field(default_factory=dict)

    @classmethod
    def memory(cls) -> "JobStore":
        return cls()

    @classmethod
    def postgres(cls, client: Any) -> "JobStore":
        return cls(_client=client)

    def find_by_trigger(self, job_key: str, trigger_request_id: str | None) -> dict[str, Any] | None:
        if not trigger_request_id:
            return None
        if self._client is not None:
            return self._client.find_job_run_by_trigger(job_key, trigger_request_id)
        for record in self._runs.values():
            if record.get("job_key") == job_key and record.get("trigger_request_id") == trigger_request_id:
                return record
        return None

    def mark_started(self, run_id: str, request: JobRequest, started_at: datetime) -> None:
        record = {
            "run_id": run_id,
            "job_key": request.job_key,
            "trigger_request_id": request.trigger_request_id,
            "triggered_by": request.triggered_by,
            "dry_run": request.dry_run,
            "status": "running",
            "success": False,
            "started_at": started_at,
            "params": request.params,
        }
        if self._client is not None:
            self._client.upsert_job_run(record)
        self._runs[run_id] = record

    def mark_finished(self, result: JobResult) -> None:
        record = {
            **self._runs.get(result.run_id, {}),
            **result.to_dict(),
            "started_at": result.started_at,
            "finished_at": result.finished_at,
        }
        if self._client is not None:
            self._client.upsert_job_run(record)
        self._runs[result.run_id] = record

    def mark_progress(self, run_id: str, updates: dict[str, Any]) -> None:
        record = {
            **self._runs.get(run_id, {}),
            **updates,
        }
        if self._client is not None:
            self._client.upsert_job_run(record)
        self._runs[run_id] = record

    def get(self, run_id: str) -> dict[str, Any] | None:
        if self._client is not None:
            return self._client.get_job_run(run_id)
        return self._runs.get(run_id)

    def mark_interrupted_running_jobs(self, message: str, job_keys: set[str] | None = None) -> int:
        if self._client is not None:
            return int(self._client.mark_interrupted_running_jobs(message))
        count = 0
        now = datetime.now(timezone.utc)
        for record in self._runs.values():
            if (
                record.get("status") == "running"
                and record.get("finished_at") is None
                and (not job_keys or record.get("job_key") in job_keys)
            ):
                record.update(
                    {
                        "status": "expired",
                        "success": False,
                        "finished_at": now,
                        "errors": [message],
                        "metadata": {**(record.get("metadata") or {}), "lastMessage": message, "interrupted": True},
                    }
                )
                count += 1
        return count
