from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi import build_sector_breadth
from cynexus_data.runtime.context import PipelineContext


def test_build_sector_breadth_dry_run_manual_payload() -> None:
    result = build_sector_breadth.run(
        PipelineContext(),
        JobRequest(job_key="build_sector_breadth", dry_run=True, params={"trade_date": "2026-06-26", "advancing": 72, "declining": 17, "flat": 1, "total": 90}),
    )

    assert result["rows_read"] == 90
    assert result["rows_written"] == 0
    assert result["payload"]["advancePct"] == 0.8
    assert result["payload"]["score"] == 80.0
