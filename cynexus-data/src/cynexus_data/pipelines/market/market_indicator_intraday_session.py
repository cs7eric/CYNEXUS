from __future__ import annotations

from time import monotonic, sleep
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.market.market_indicator_refresh import run as refresh_run
from cynexus_data.pipelines.msi._params import parse_int
from cynexus_data.runtime.context import PipelineContext


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    """Run sequential capture cycles under one scheduler lease.

    The refresh pipeline remains the compatibility capture implementation, but
    this session provides the required no-overlap/adaptive cadence boundary so
    the scheduler does not launch independent five-second jobs.
    """
    target = max(1, parse_int(request.params.get("target_cadence_seconds"), 5))
    duration = max(1, parse_int(request.params.get("duration_seconds"), 300))
    max_cycles = max(1, parse_int(request.params.get("max_cycles"), duration // target + 1))
    if request.dry_run:
        return {
            "pipeline": "market.market_indicator_intraday_session",
            "dry_run": True,
            "success": True,
            "target_cadence_seconds": target,
            "duration_seconds": duration,
            "max_cycles": max_cycles,
            "notes": ["one session owns sequential cycles; cadence can rise from measured refresh latency"],
        }
    started = monotonic()
    cadence = target
    cycles: list[dict[str, Any]] = []
    while len(cycles) < max_cycles and monotonic() - started < duration:
        cycle_started = monotonic()
        params = dict(request.params)
        params["target_cadence_seconds"] = cadence
        params.pop("duration_seconds", None)
        params.pop("max_cycles", None)
        result = refresh_run(context, JobRequest(job_key="market_indicator_refresh", triggered_by="intraday-session", params=params))
        cycle_elapsed = monotonic() - cycle_started
        result = {**result, "session_cycle_elapsed_seconds": round(cycle_elapsed, 3), "session_cycle": len(cycles) + 1}
        cycles.append(result)
        measured = int(result.get("effective_cadence_seconds") or cadence)
        if result.get("benchmark_gate") == "degrade_cadence":
            cadence = max(cadence, measured)
        elif len(cycles) >= 6 and all(item.get("benchmark_gate") == "pass" for item in cycles[-6:]):
            cadence = max(target, cadence - 1)
        remaining = cadence - cycle_elapsed
        if remaining > 0 and len(cycles) < max_cycles:
            sleep(min(remaining, max(0.0, duration - (monotonic() - started))))
    return {
        "pipeline": "market.market_indicator_intraday_session",
        "success": all(bool(item.get("success")) for item in cycles) if cycles else False,
        "cycles": len(cycles),
        "target_cadence_seconds": target,
        "effective_cadence_seconds": cadence,
        "session_elapsed_seconds": round(monotonic() - started, 3),
        "cycle_results": cycles[-10:],
    }
