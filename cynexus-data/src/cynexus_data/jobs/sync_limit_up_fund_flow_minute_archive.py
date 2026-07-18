from __future__ import annotations

from cynexus_data.jobs._pipeline_job import main_for_job, run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock import sync_limit_up_fund_flow_minute_archive
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, sync_limit_up_fund_flow_minute_archive.run)


def main() -> None:
    main_for_job("sync_limit_up_fund_flow_minute_archive", "Archive EastMoney minute fund-flow for historical limit-up stocks.")


if __name__ == "__main__":
    main()
