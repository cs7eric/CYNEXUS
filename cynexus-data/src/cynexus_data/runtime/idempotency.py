from __future__ import annotations


def idempotency_key(job_key: str, trigger_request_id: str | None) -> str | None:
    if not trigger_request_id:
        return None
    return f"{job_key}:{trigger_request_id}"

