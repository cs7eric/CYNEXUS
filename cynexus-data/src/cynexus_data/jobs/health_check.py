from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    checks: dict[str, dict] = {"runtime": {"ok": True}}
    if request.params.get("check_storage"):
        for name in ["clickhouse", "duckdb"]:
            try:
                hook = getattr(context.hooks, name)
                checks[name] = hook.get_client().health()
            except Exception as exc:  # noqa: BLE001
                checks[name] = {"ok": False, "error": str(exc)}
    ok = all(item.get("ok") for item in checks.values())
    now = datetime.now(timezone.utc)
    return JobResult(
        run_id=run_id,
        job_key=request.job_key,
        success=ok,
        status="success" if ok else "failed",
        started_at=now,
        finished_at=now,
        metadata={"checks": checks},
        errors=[] if ok else ["one or more health checks failed"],
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Run cynexus-data health check.")
    parser.add_argument("--check-storage", action="store_true")
    args = parser.parse_args()
    from cynexus_data.jobs.registry import JOB_REGISTRY
    from cynexus_data.runtime.job_runner import JobRunner

    result = JobRunner(JOB_REGISTRY).run(
        JobRequest(job_key="health_check", dry_run=True, params={"check_storage": args.check_storage})
    )
    print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2, default=str))


if __name__ == "__main__":
    main()

