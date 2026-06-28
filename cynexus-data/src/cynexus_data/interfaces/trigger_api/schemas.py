from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class TriggerRequest(BaseModel):
    trigger_request_id: str | None = None
    triggered_by: str | None = None
    dry_run: bool = False
    params: dict[str, Any] = Field(default_factory=dict)


class TriggerResponse(BaseModel):
    accepted: bool
    run_id: str
    job_key: str
    status: str
    result: dict[str, Any]

