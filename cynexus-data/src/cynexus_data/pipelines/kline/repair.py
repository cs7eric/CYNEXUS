from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest) -> dict:
    return {"pipeline": "kline.repair", "dry_run": request.dry_run, "dataset": request.params.get("dataset", "kline"), "rows_read": 0, "rows_written": 0}

