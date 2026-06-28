from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.msi import WEIGHTS, calculate_market_sentiment_index
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.msi import MsiRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    require_full = _truthy(request.params.get("require_full"))
    force = _truthy(request.params.get("force"))
    create_table = _truthy(request.params.get("create_table"))
    components = _parse_components(request.params)
    result = {
        "pipeline": "msi.build_msi",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "require_full": require_full,
        "force": force,
        "rows_read": 0,
        "rows_written": 0,
    }
    repo: MsiRepository | None = None
    if not request.dry_run:
        if context.hooks is None:
            raise RuntimeError("PipelineContext.hooks is required")
        repo = MsiRepository(context.hooks.postgres.get_client())
        if create_table:
            repo.create_table()
        if not force and not components:
            cached = repo.get_index(trade_date)
            if cached is not None:
                return {**result, "rows_read": 1, "payload": cached, "from_cache": True}
        if not components:
            components = repo.read_component_scores(trade_date)
            result["component_source"] = "postgres.msi_component_tables"
    else:
        result["notes"] = ["dry_run skipped Postgres access"]
    payload = calculate_market_sentiment_index(trade_date=trade_date, components=components, require_full=require_full)
    if payload is None:
        return {**result, "skipped": True, "skip_reason": "no_components_or_require_full_not_met", "components": components}
    result["rows_read"] = len([value for value in payload["components"].values() if value is not None])
    result["payload"] = payload
    if repo is not None:
        result["rows_written"] = repo.save_index(payload)
    return result


def _parse_components(params: dict[str, Any]) -> dict[str, float | None]:
    out: dict[str, float | None] = {}
    raw = params.get("components")
    if isinstance(raw, str) and raw.strip():
        parsed = json.loads(raw)
        if isinstance(parsed, dict):
            out.update({key: _optional_float(parsed.get(key)) for key in WEIGHTS})
    elif isinstance(raw, dict):
        out.update({key: _optional_float(raw.get(key)) for key in WEIGHTS})
    for key in WEIGHTS:
        if key in params:
            out[key] = _optional_float(params.get(key))
    return out


def _optional_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    return float(value)


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value))


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
