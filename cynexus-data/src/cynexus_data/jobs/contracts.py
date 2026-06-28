from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any


@dataclass(frozen=True)
class JobRequest:
    job_key: str
    trigger_request_id: str | None = None
    triggered_by: str | None = None
    dry_run: bool = False
    params: dict[str, Any] = field(default_factory=dict)


@dataclass
class JobResult:
    run_id: str
    job_key: str
    success: bool
    status: str
    started_at: datetime
    finished_at: datetime | None = None
    rows_read: int = 0
    rows_written: int = 0
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {
            "run_id": self.run_id,
            "job_key": self.job_key,
            "success": self.success,
            "status": self.status,
            "started_at": self.started_at.isoformat(),
            "finished_at": self.finished_at.isoformat() if self.finished_at else None,
            "rows_read": self.rows_read,
            "rows_written": self.rows_written,
            "warnings": self.warnings,
            "errors": self.errors,
            "metadata": self.metadata,
        }

