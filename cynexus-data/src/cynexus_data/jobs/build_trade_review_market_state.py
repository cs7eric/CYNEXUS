from __future__ import annotations

from cynexus_data.jobs._pipeline_job import run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market.build_trade_review_market_state import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job("build_trade_review_market_state", "Build Trade Review minute and daily market-state projections.")


if __name__ == "__main__":
    main()
