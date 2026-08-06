from __future__ import annotations

from cynexus_data.jobs._pipeline_job import main_for_job, run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market.market_indicator_benchmark import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


def main() -> None:
    main_for_job("market_indicator_benchmark", "Benchmark full-market market indicator capture and compute latency.")


if __name__ == "__main__":
    main()
