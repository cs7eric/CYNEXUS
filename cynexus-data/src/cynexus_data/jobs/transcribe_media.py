from __future__ import annotations

import argparse
import json

from cynexus_data.jobs._pipeline_job import run_pipeline_job
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.transcription.transcribe_media import run as pipeline_run
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest, run_id: str):
    return run_pipeline_job(context, request, run_id, pipeline_run)


def main() -> None:
    parser = argparse.ArgumentParser(description="Transcribe media to transcript only.")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--input-uri", required=True)
    parser.add_argument("--language")
    args = parser.parse_args()
    from cynexus_data.jobs.registry import JOB_REGISTRY
    from cynexus_data.runtime.job_runner import JobRunner

    result = JobRunner(JOB_REGISTRY).run(
        JobRequest(job_key="transcribe_media", dry_run=args.dry_run, params={"input_uri": args.input_uri, "language": args.language})
    )
    print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2, default=str))


if __name__ == "__main__":
    main()

