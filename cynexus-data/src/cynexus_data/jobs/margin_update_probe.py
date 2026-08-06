from __future__ import annotations

from cynexus_data.jobs._pipeline_job import run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.margin.update_probe import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job("margin_update_probe", "Probe margin data freshness and sync after update.")


if __name__ == "__main__":
    main()
