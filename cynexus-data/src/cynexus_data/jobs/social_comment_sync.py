from __future__ import annotations

from cynexus_data.jobs._pipeline_job import main_for_job, run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.pipelines.social.comment_sync import run as run_pipeline
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    return run_pipeline_job(context, request, run_id, run_pipeline)


def main() -> None:
    main_for_job("social_comment_sync", "Collect social post comments.")


if __name__ == "__main__":
    main()
