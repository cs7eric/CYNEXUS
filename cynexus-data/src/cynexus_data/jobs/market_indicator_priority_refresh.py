from __future__ import annotations

from cynexus_data.jobs._pipeline_job import main_for_job, run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market.market_indicator_refresh import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    """Run the low-latency priority radar over the attention cohort.

    This job intentionally publishes a Redis-only partial read model. The
    full-market refresh remains the authority for complete industry
    confirmation and PostgreSQL/replay persistence.
    """
    params = dict(request.params)
    params.setdefault("universe_mode", "historical_limit")
    params.setdefault("limit_codes", 1500)
    params.setdefault("target_cadence_seconds", 10)
    params.setdefault("scope", "priority")
    params.setdefault("redis_prefix", "cynexus:market-indicator:priority")
    params.setdefault("persist_postgres", False)
    params.setdefault("full_confirmation_required", True)
    params.setdefault("redis_ttl_seconds", 7200)
    params.setdefault("batch_size", 320)
    params.setdefault("quote_retry_attempts", 2)
    params.setdefault("quote_retry_backoff_seconds", 0.25)
    params.setdefault("quote_request_spacing_seconds", 0.02)
    params.setdefault("quote_fallback_batch_size", 80)
    return run_pipeline_job(
        context,
        JobRequest(job_key="market_indicator_priority_refresh", triggered_by=request.triggered_by, params=params, dry_run=request.dry_run),
        run_id,
        pipeline_run,
    )


def main() -> None:
    main_for_job("market_indicator_priority_refresh", "Refresh the low-latency priority market indicator radar.")


if __name__ == "__main__":
    main()
