from __future__ import annotations

from datetime import datetime, timezone
from threading import Event, Thread

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.jobs._pipeline_job import run_pipeline_job
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.job_runner import JobRunner


def _ok_job(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    now = datetime.now(timezone.utc)
    return JobResult(run_id=run_id, job_key=request.job_key, success=True, status="success", started_at=now, finished_at=now)


def test_job_runner_runs_registered_job() -> None:
    result = JobRunner({"ok": _ok_job}).run(JobRequest(job_key="ok", dry_run=True))
    assert result.success is True
    assert result.status == "success"


def test_job_runner_replays_success_by_trigger_request_id() -> None:
    runner = JobRunner({"ok": _ok_job})
    request = JobRequest(job_key="ok", trigger_request_id="req-1", dry_run=True)
    first = runner.run(request)
    second = runner.run(request)
    assert first.run_id == second.run_id
    assert second.metadata["idempotent_replay"] is True


def test_job_runner_lock_scope_allows_parallel_scopes() -> None:
    entered = Event()
    release = Event()

    def _blocking_job(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
        entered.set()
        release.wait(timeout=5)
        now = datetime.now(timezone.utc)
        return JobResult(run_id=run_id, job_key=request.job_key, success=True, status="success", started_at=now, finished_at=now)

    runner = JobRunner({"ok": _blocking_job})
    first_request = JobRequest(job_key="ok", dry_run=False, params={"lock_scope": "akshare"})
    worker = Thread(target=lambda: runner.run(first_request), daemon=True)
    worker.start()
    assert entered.wait(timeout=2)

    second = runner.run(JobRequest(job_key="ok", dry_run=False, params={"lock_scope": "eltdx"}))
    release.set()
    worker.join(timeout=2)

    assert second.success is True
    assert second.status == "success"


def test_pipeline_job_maps_failure_payload_to_alertable_job_result() -> None:
    def failed_pipeline(context, request):
        return {"success": False, "status": "failed", "error": "source unavailable", "stale": True}

    result = run_pipeline_job(
        PipelineContext(),
        JobRequest(job_key="failed", dry_run=True),
        "run-1",
        failed_pipeline,
    )

    assert result.success is False
    assert result.status == "failed"
    assert result.errors == ["source unavailable"]
    assert result.metadata["stale"] is True
