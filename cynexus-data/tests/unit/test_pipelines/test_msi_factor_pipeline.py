from __future__ import annotations

import json

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi import build_ma_count, build_profit_effect
from cynexus_data.runtime.context import PipelineContext


def test_build_ma_count_dry_run_rows() -> None:
    rows = [
        {"close": 12, "ma20": 10, "ma60": 11, "close_1d_ago": 11, "close_5d_ago": 10, "close_59d_ago": 15, "min_60d": 9, "close_251d_ago": 8, "max_252d": 12},
        {"close": 8, "ma20": 10, "ma60": 9, "close_1d_ago": 9, "close_5d_ago": 9, "close_59d_ago": 11, "min_60d": 8, "close_251d_ago": 10, "max_252d": 14},
    ]

    result = build_ma_count.run(
        PipelineContext(),
        JobRequest(job_key="build_ma_count", dry_run=True, params={"trade_date": "2026-06-26", "rows": json.dumps(rows)}),
    )

    assert result["rows_read"] == 2
    assert result["payload"]["breadthRaw"] == 50.0


def test_build_profit_effect_dry_run_params() -> None:
    result = build_profit_effect.run(
        PipelineContext(),
        JobRequest(job_key="build_profit_effect", dry_run=True, params={"trade_date": "2026-06-26", "up_5d_pct": 60, "new_low_60d_pct": 10}),
    )

    assert result["payload"]["score"] == 72.0
    assert result["rows_written"] == 0
