from __future__ import annotations

from datetime import datetime, timezone

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.job_store import JobStore


def test_memory_job_recovery_is_scoped_and_marks_expired() -> None:
    store = JobStore.memory()
    now = datetime.now(timezone.utc)
    store.mark_started("social-run", JobRequest(job_key="social_creator_discovery"), now)
    store.mark_started("market-run", JobRequest(job_key="sync_margin"), now)

    assert store.mark_interrupted_running_jobs("agent restarted", {"social_creator_discovery", "social_comment_sync"}) == 1
    assert store.get("social-run")["status"] == "expired"
    assert store.get("market-run")["status"] == "running"
