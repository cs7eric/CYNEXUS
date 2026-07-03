from __future__ import annotations

from datetime import datetime, timezone
from typing import Callable
from uuid import uuid4

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.runtime.context import PipelineContext


JobCallable = Callable[[PipelineContext, JobRequest, str], JobResult]


class JobRunner:
    def __init__(self, registry: dict[str, JobCallable], context: PipelineContext | None = None) -> None:
        self.registry = registry
        self.context = context or PipelineContext.default()

    def run(self, request: JobRequest) -> JobResult:
        job = self.registry.get(request.job_key)
        if job is None:
            now = datetime.now(timezone.utc)
            return JobResult(
                run_id=f"job_run_{uuid4().hex}",
                job_key=request.job_key,
                success=False,
                status="unknown_job",
                started_at=now,
                finished_at=now,
                errors=[f"unknown job_key: {request.job_key}"],
            )

        existing = self.context.job_store.find_by_trigger(request.job_key, request.trigger_request_id)
        if existing and existing.get("status") in {"success", "skipped"}:
            now = datetime.now(timezone.utc)
            return JobResult(
                run_id=str(existing["run_id"]),
                job_key=request.job_key,
                success=bool(existing.get("success")),
                status=str(existing.get("status")),
                started_at=existing.get("started_at") if isinstance(existing.get("started_at"), datetime) else now,
                finished_at=existing.get("finished_at") if isinstance(existing.get("finished_at"), datetime) else now,
                metadata={"idempotent_replay": True},
            )

        run_id = f"job_run_{uuid4().hex}"
        started_at = datetime.now(timezone.utc)
        self.context.job_store.mark_started(run_id, request, started_at)
        return self.run_started(request, run_id, started_at)

    def start(self, request: JobRequest) -> JobResult:
        job = self.registry.get(request.job_key)
        now = datetime.now(timezone.utc)
        if job is None:
            return JobResult(
                run_id=f"job_run_{uuid4().hex}",
                job_key=request.job_key,
                success=False,
                status="unknown_job",
                started_at=now,
                finished_at=now,
                errors=[f"unknown job_key: {request.job_key}"],
            )

        existing = self.context.job_store.find_by_trigger(request.job_key, request.trigger_request_id)
        if existing and existing.get("status") in {"success", "skipped"}:
            return JobResult(
                run_id=str(existing["run_id"]),
                job_key=request.job_key,
                success=bool(existing.get("success")),
                status=str(existing.get("status")),
                started_at=existing.get("started_at") if isinstance(existing.get("started_at"), datetime) else now,
                finished_at=existing.get("finished_at") if isinstance(existing.get("finished_at"), datetime) else now,
                metadata={"idempotent_replay": True},
            )

        run_id = f"job_run_{uuid4().hex}"
        self.context.job_store.mark_started(run_id, request, now)
        return JobResult(
            run_id=run_id,
            job_key=request.job_key,
            success=False,
            status="running",
            started_at=now,
            finished_at=None,
            metadata={"accepted_async": True},
        )

    def run_started(self, request: JobRequest, run_id: str, started_at: datetime) -> JobResult:
        job = self.registry.get(request.job_key)
        if job is None:
            result = JobResult(
                run_id=run_id,
                job_key=request.job_key,
                success=False,
                status="unknown_job",
                started_at=started_at,
                finished_at=datetime.now(timezone.utc),
                errors=[f"unknown job_key: {request.job_key}"],
            )
            self.context.job_store.mark_finished(result)
            return result
        lock_key = self._build_lock_key(request)
        with self.context.locks.acquire(lock_key, run_id) as acquired:
            if not acquired:
                result = JobResult(
                    run_id=run_id,
                    job_key=request.job_key,
                    success=False,
                    status="locked",
                    started_at=started_at,
                    finished_at=datetime.now(timezone.utc),
                    errors=[f"job is already running: {request.job_key}"],
                )
                self.context.job_store.mark_finished(result)
                return result
            try:
                result = job(self.context, request, run_id)
            except Exception as exc:  # noqa: BLE001
                result = JobResult(
                    run_id=run_id,
                    job_key=request.job_key,
                    success=False,
                    status="failed",
                    started_at=started_at,
                    finished_at=datetime.now(timezone.utc),
                    errors=[f"{type(exc).__name__}: {exc}"],
                )
        if result.finished_at is None:
            result.finished_at = datetime.now(timezone.utc)
        self.context.job_store.mark_finished(result)
        return result

    @staticmethod
    def _build_lock_key(request: JobRequest) -> str:
        params = request.params or {}
        scope = params.get("lock_scope")
        if scope is None:
            return f"job:{request.job_key}"
        text = str(scope).strip()
        if not text:
            return f"job:{request.job_key}"
        return f"job:{request.job_key}:{text}"
