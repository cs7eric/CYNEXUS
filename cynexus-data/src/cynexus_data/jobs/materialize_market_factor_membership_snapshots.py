from cynexus_data.jobs._pipeline_job import main_for_job, run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.a_stock.materialize_market_factor_membership_snapshots import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


if __name__ == "__main__":
    main_for_job("materialize_market_factor_membership_snapshots", "Materialize point-in-time THS memberships.")
