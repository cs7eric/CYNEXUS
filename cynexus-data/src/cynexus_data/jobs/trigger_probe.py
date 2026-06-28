from __future__ import annotations

from datetime import datetime, timezone

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    now = datetime.now(timezone.utc)
    return JobResult(
        run_id=run_id,
        job_key=request.job_key,
        success=True,
        status="success",
        started_at=now,
        finished_at=now,
        metadata={
            "probe": True,
            "trigger_request_id": request.trigger_request_id,
            "triggered_by": request.triggered_by,
            "dry_run": request.dry_run,
            "params": request.params,
        },
    )


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job("trigger_probe", "Run trigger_probe.")


if __name__ == "__main__":
    main()
