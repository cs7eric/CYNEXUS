from __future__ import annotations

from cynexus_data.jobs._pipeline_job import main_for_job, run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market.sync_etf import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


JOB_KEY = "sync_market_etf"


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


def main() -> None:
    main_for_job(JOB_KEY, "Sync ETF spot, IOPV, premium, activity, and daily share-flow proxies.")


if __name__ == "__main__":
    main()
