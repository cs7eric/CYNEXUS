from __future__ import annotations

import json
from typing import Any

from cynexus_data.domain.market.transmission_validation import replay_captures
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    del context
    raw = request.params.get("captures") or request.params.get("captures_json")
    if request.dry_run or raw in (None, "", []):
        return {"pipeline": "market.market_indicator_offline_validation", "dry_run": request.dry_run, "success": True,
                "checked_captures": 0, "checked_signals": 0, "checked_episodes": 0,
                "notes": ["pass captures as a JSON array of capture-row arrays to run deterministic replay validation"]}
    try:
        captures = raw if isinstance(raw, list) else json.loads(str(raw))
        if not isinstance(captures, list):
            raise ValueError("captures must be a JSON array")
        report = replay_captures(captures)
    except (TypeError, ValueError, json.JSONDecodeError) as exc:
        return {"pipeline": "market.market_indicator_offline_validation", "success": False, "error": str(exc)}
    return {
        "pipeline": "market.market_indicator_offline_validation", "success": report.passed,
        "checked_captures": report.checked_captures, "checked_signals": report.checked_signals,
        "checked_episodes": report.checked_episodes,
        "metrics": report.metrics or {},
        "issues": [issue.__dict__ for issue in report.issues],
    }
