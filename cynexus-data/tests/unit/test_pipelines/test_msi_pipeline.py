from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi import build_msi, rebuild_msi
from cynexus_data.runtime.context import PipelineContext


def test_build_msi_dry_run_with_components() -> None:
    components = {"vol": 80, "turnover": 60}

    result = build_msi.run(
        PipelineContext(),
        JobRequest(job_key="build_msi", dry_run=True, params={"trade_date": "2026-06-26", "components": json.dumps(components)}),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 0
    assert result["payload"]["compositeScore"] == 56.0


def test_build_msi_dry_run_require_full_skips() -> None:
    result = build_msi.run(PipelineContext(), JobRequest(job_key="build_msi", dry_run=True, params={"trade_date": "2026-06-26", "vol": 80, "require_full": True}))

    assert result["skipped"] is True
    assert result["skip_reason"] == "no_components_or_require_full_not_met"


def test_rebuild_msi_dry_run_counts_dates() -> None:
    result = rebuild_msi.run(PipelineContext(), JobRequest(job_key="rebuild_msi", dry_run=True, params={"start_date": "2026-06-24", "end_date": "2026-06-26"}))

    assert result["planned_dates"] == 3
    assert result["rows_written"] == 0
