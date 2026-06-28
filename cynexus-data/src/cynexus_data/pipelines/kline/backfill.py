from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.kline.sync_daily import run as sync_daily_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest) -> dict:
    dataset = request.params.get("dataset") or "kline_daily"
    if dataset not in {"kline", "kline_daily", "daily_raw"}:
        return {
            "pipeline": "kline.backfill",
            "dry_run": request.dry_run,
            "dataset": dataset,
            "rows_read": 0,
            "rows_written": 0,
            "warnings": [f"unsupported backfill dataset: {dataset}"],
        }
    result = sync_daily_run(context, request)
    result["pipeline"] = "kline.backfill"
    result["dataset"] = dataset
    return result
