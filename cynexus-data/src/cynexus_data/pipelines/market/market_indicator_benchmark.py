from __future__ import annotations

from datetime import datetime, timezone
from statistics import median
from time import monotonic
from typing import Any

from cynexus_data.domain.market.transmission import build_transmission_snapshot, to_jsonable
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.indicator import create_eltdx_indicator_client, fetch_indicator_snapshots
from cynexus_data.storage.postgres.market_indicator import MarketIndicatorRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = MarketIndicatorRepository(context.hooks.postgres.get_client())
    if request.dry_run:
        return {
            "pipeline": "market.market_indicator_benchmark",
            "dry_run": True,
            "success": True,
            "scenario_universe_sizes": [500, 1500, 3000, "full_market"],
            "target_cadence_seconds": parse_int(request.params.get("target_cadence_seconds"), 5),
        }
    metadata = repo.list_stock_metadata()
    if not metadata:
        return {"pipeline": "market.market_indicator_benchmark", "success": False, "error": "sec_stock_basic is empty"}
    requested = request.params.get("universe_sizes")
    sizes = _sizes(requested, len(metadata))
    rounds = max(1, min(600, parse_int(request.params.get("rounds"), 3)))
    batch_size = max(20, parse_int(request.params.get("batch_size"), 160))
    pool_size = max(1, parse_int(request.params.get("pool_size"), 8))
    timeout = float(request.params.get("timeout") or 8.0)
    provider = str(request.params.get("provider") or "eltdx")
    results: list[dict[str, Any]] = []
    for size in sizes:
        selected = dict(list(metadata.items())[:size])
        timings: list[float] = []
        coverages: list[float] = []
        client = create_eltdx_indicator_client(pool_size=pool_size, timeout=timeout, batch_size=batch_size)
        try:
            for _ in range(rounds):
                started = monotonic()
                rows = fetch_indicator_snapshots(client, selected, batch_size=batch_size, captured_at=datetime.now(timezone.utc))
                previous = {str(row["code"]): row for row in rows}
                build_transmission_snapshot(rows, previous, detected_at=datetime.now(timezone.utc))
                timings.append(monotonic() - started)
                coverages.append(len(rows) / max(1, len(selected)))
        finally:
            close = getattr(client, "close", None)
            if callable(close):
                close()
        timings_sorted = sorted(timings)
        result = {
            "universe_size": size,
            "rounds": rounds,
            "p50_seconds": _percentile(timings_sorted, 0.50),
            "p95_seconds": _percentile(timings_sorted, 0.95),
            "p99_seconds": _percentile(timings_sorted, 0.99),
            "coverage_ratio": round(median(coverages) if coverages else 0.0, 6),
            "fresh_ratio": round(median(coverages) if coverages else 0.0, 6),
        }
        payload = {
            "run_id": f"{datetime.now(timezone.utc).isoformat()}:{size}",
            "run_at": datetime.now(timezone.utc),
            "trade_date": datetime.now(timezone.utc).date(),
            "provider": provider,
            "target_cadence_seconds": parse_int(request.params.get("target_cadence_seconds"), 5),
            "universe_size": size,
            "rounds": rounds,
            **result,
        }
        if truthy(request.params.get("persist", True)):
            repo.save_benchmark_run(payload)
        results.append(payload)
    return {"pipeline": "market.market_indicator_benchmark", "success": True, "provider": provider, "results": [to_jsonable(item) for item in results]}


def _sizes(value: object, total: int) -> list[int]:
    if value in (None, "", []):
        return sorted({min(total, size) for size in (500, 1500, 3000, total)})
    if isinstance(value, list):
        return sorted({max(1, min(total, int(item))) for item in value})
    return sorted({max(1, min(total, int(item.strip()))) for item in str(value).split(",") if item.strip()})


def _percentile(values: list[float], percentile: float) -> float | None:
    if not values:
        return None
    index = min(len(values) - 1, max(0, int(round((len(values) - 1) * percentile))))
    return round(values[index], 4)
