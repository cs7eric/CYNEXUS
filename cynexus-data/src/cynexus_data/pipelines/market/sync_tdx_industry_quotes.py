from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.index_codes import TDX_INDUSTRY_56
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.industry_quotes import create_eltdx_industry_client, fetch_tdx_industry_snapshots
from cynexus_data.storage.postgres.tdx_industry_quotes import TdxIndustryQuoteRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    snapshot_date = parse_date(request.params.get("snapshot_date") or request.params.get("trade_date")) or date.today()
    rows = _parse_rows(request.params.get("rows"))
    mapping = _parse_mapping(request.params.get("mapping")) or TDX_INDUSTRY_56
    batch_size = parse_int(request.params.get("batch_size"), 20)
    create_table = truthy(request.params.get("create_table"))
    result = {
        "pipeline": "market.sync_tdx_industry_quotes",
        "dry_run": request.dry_run,
        "snapshot_date": snapshot_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
        "planned_industries": len(mapping),
    }
    if request.dry_run:
        planned_rows = rows or _sample_rows(mapping)
        return {**result, "rows_read": len(planned_rows), "sample": planned_rows[:5], "notes": ["dry_run skipped eltdx and Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    if not rows:
        pool_size = parse_int(request.params.get("pool_size"), 8)
        timeout = float(request.params.get("timeout") or request.params.get("timeout_seconds") or 8.0)
        client = create_eltdx_industry_client(pool_size=pool_size, timeout=timeout)
        try:
            rows = fetch_tdx_industry_snapshots(client, mapping=mapping, batch_size=batch_size)
        finally:
            close = getattr(client, "close", None)
            if callable(close):
                close()
    repo = TdxIndustryQuoteRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    saved = repo.replace_snapshot(snapshot_date, rows, source=str(request.params.get("source") or "eltdx"))
    result["rows_read"] = len(rows)
    result["rows_written"] = int(saved["rows_written"])
    result["batch"] = saved
    return result


def _parse_rows(value: Any) -> list[dict[str, Any]]:
    if value in (None, ""):
        return []
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, list):
        raise ValueError("rows must be a JSON array")
    return [dict(item) for item in raw if isinstance(item, dict)]


def _parse_mapping(value: Any) -> dict[str, str] | None:
    if value in (None, ""):
        return None
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, dict):
        raise ValueError("mapping must be a JSON object")
    return {str(code): str(name) for code, name in raw.items()}


def _sample_rows(mapping: dict[str, str]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for index, (code6, name) in enumerate(mapping.items()):
        rows.append(
            {
                "code6": code6,
                "name": name,
                "full_code": f"sh{code6}",
                "exchange": "sh",
                "last_price": 1000 + index,
                "pre_close_price": 998 + index,
                "change": 2,
                "change_pct": round(0.2 + index * 0.01, 4),
                "amount": 100000000 + index,
                "total_hand": 10000 + index,
                "current_hand": 100 + index,
            }
        )
    return rows
