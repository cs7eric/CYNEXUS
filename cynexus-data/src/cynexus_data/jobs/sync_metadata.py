from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.jobs.sync_stock_basic import run as run_stock_basic
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    return run_stock_basic(context, JobRequest(job_key=request.job_key, dry_run=request.dry_run, params=request.params), run_id)


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job("sync_metadata", "Run sync_metadata.")


if __name__ == "__main__":
    main()
