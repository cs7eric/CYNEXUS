from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from cynexus_data.interfaces.trigger_api.auth import require_internal_token
from cynexus_data.interfaces.trigger_api.schemas import TriggerRequest, TriggerResponse
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.jobs.registry import JOB_REGISTRY, job_descriptions
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.job_runner import JobRunner


router = APIRouter()
_context = PipelineContext.default(persist_job_state=False)


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
def trigger_job(job_key: str, payload: TriggerRequest) -> TriggerResponse:
    runner = JobRunner(JOB_REGISTRY, context=_context)
    result = runner.run(
        JobRequest(
            job_key=job_key,
            trigger_request_id=payload.trigger_request_id,
            triggered_by=payload.triggered_by,
            dry_run=payload.dry_run,
            params=payload.params,
        )
    )
    return TriggerResponse(
        accepted=result.status != "unknown_job",
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

