from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.interfaces.trigger_api.schemas import TriggerRequest, TriggerResponse
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.jobs.registry import JOB_REGISTRY, job_descriptions
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.job_runner import JobRunner


router = APIRouter()
_context = PipelineContext.default(persist_job_state=True)
ASYNC_TRIGGER_JOBS = {
    "market_sentiment_chain_refresh",
    "a_stock_fund_flow_history_backfill",
}


@router.get("/internal/health")
def health() -> dict:
    return {"ok": True, "service": "cynexus-data"}


@router.get("/internal/jobs")
def jobs() -> dict:
    return {"jobs": job_descriptions()}


@router.get("/internal/jobs/{job_key}")
def job_detail(job_key: str) -> dict:
    detail = job_descriptions().get(job_key)
    if detail is None:
        raise HTTPException(status_code=404, detail="job not found")
    return detail


@router.post("/internal/jobs/{job_key}/trigger", response_model=TriggerResponse, dependencies=[Depends(require_internal_token)])
def trigger_job(job_key: str, payload: TriggerRequest, background_tasks: BackgroundTasks) -> TriggerResponse:
    runner = JobRunner(JOB_REGISTRY, context=_context)
    request = JobRequest(
        job_key=job_key,
        trigger_request_id=payload.trigger_request_id,
        triggered_by=payload.triggered_by,
        dry_run=payload.dry_run,
        params=payload.params,
    )
    is_margin_history = job_key == "sync_margin" and str(payload.params.get("mode") or "").strip().lower() == "stock_history"
    if job_key in ASYNC_TRIGGER_JOBS or is_margin_history:
        result = runner.start(request)
        if result.status == "running":
            background_tasks.add_task(runner.run_started, request, result.run_id, result.started_at)
    else:
        result = runner.run(request)
    return TriggerResponse(
        accepted=result.status not in {"unknown_job", "locked"},
        run_id=result.run_id,
        job_key=result.job_key,
        status=result.status,
        result=result.to_dict(),
    )


@router.get("/internal/job-runs/{run_id}")
def get_job_run(run_id: str) -> dict:
    record = _context.job_store.get(run_id)
    if record is None:
        raise HTTPException(status_code=404, detail="job run not found")
    return record
