from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market_pulse import build_market_pulse
from cynexus_data.runtime.context import PipelineContext


def test_build_market_pulse_dry_run_manual_rows() -> None:
    rows = [
        {"sector_name": "银行", "change_pct": -0.2, "net": -3},
        {"sector_name": "软件服务", "change_pct": 1.2, "net": 6},
    ]

    result = build_market_pulse.run(
        PipelineContext(),
        JobRequest(job_key="build_market_pulse", dry_run=True, params={"trade_date": "2026-06-26", "rows": json.dumps(rows, ensure_ascii=False)}),
    )

    assert result["rows_read"] == 2
    assert result["rows_written"] == 0
    assert result["planned_rows"] == 2
    assert result["payload"]["strong"]["top"][0]["sector_name"] == "软件服务"
