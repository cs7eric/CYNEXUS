from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from typing import Callable

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.runtime.context import PipelineContext


PipelineCallable = Callable[[PipelineContext, JobRequest], dict]


def run_pipeline_job(context: PipelineContext, request: JobRequest, run_id: str, pipeline: PipelineCallable) -> JobResult:
    started_at = datetime.now(timezone.utc)
    payload = pipeline(context, request)
    success = bool(payload.get("success", True))
    errors = _string_list(payload.get("errors"))
    if not success and not errors and payload.get("error"):
        errors = [str(payload["error"])]
    return JobResult(
        run_id=run_id,
        job_key=request.job_key,
        success=success,
        status=str(payload.get("status") or ("success" if success else "failed")),
        started_at=started_at,
        finished_at=datetime.now(timezone.utc),
        rows_read=int(payload.get("rows_read") or 0),
        rows_written=int(payload.get("rows_written") or 0),
        warnings=_string_list(payload.get("warnings")),
        errors=errors,
        metadata=payload,
    )


def _string_list(value: object) -> list[str]:
    if value is None:
        return []
    if isinstance(value, (list, tuple, set)):
        return [str(item) for item in value if item is not None]
    return [str(value)]


def main_for_job(job_key: str, description: str | None = None) -> None:
    parser = argparse.ArgumentParser(description=description or f"Run {job_key}.")
    parser.add_argument("--dry-run", action="store_true")
    known, unknown = parser.parse_known_args()

    from cynexus_data.jobs.registry import JOB_REGISTRY
    from cynexus_data.runtime.job_runner import JobRunner

    request = JobRequest(job_key=job_key, dry_run=known.dry_run, params=_parse_unknown_params(unknown))
    result = JobRunner(JOB_REGISTRY).run(request)
    print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2, default=str))


def _parse_unknown_params(args: list[str]) -> dict[str, object]:
    params: dict[str, object] = {}
    index = 0
    while index < len(args):
        token = args[index]
        if not token.startswith("--"):
            index += 1
            continue
        key = token[2:].replace("-", "_")
        if "=" in key:
            name, value = key.split("=", 1)
            params[name] = value
            index += 1
            continue
        if index + 1 >= len(args) or args[index + 1].startswith("--"):
            params[key] = True
            index += 1
            continue
        value = args[index + 1]
        existing = params.get(key)
        if existing is None:
            params[key] = value
        elif isinstance(existing, list):
            existing.append(value)
        else:
            params[key] = [existing, value]
        index += 2
    return params
