from __future__ import annotations

from cynexus_data.jobs._pipeline_job import run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.sync_index_daily_raw_from_daily_raw import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job("sync_index_daily_raw_from_daily_raw", "Sync index_daily_raw from daily_raw index rows.")


if __name__ == "__main__":
    main()
