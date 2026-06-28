from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.market_heatmap import build_industry_heatmap_payload
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.market_heatmap import MarketHeatmapRepository
from cynexus_data.storage.postgres.tdx_industry_quotes import TdxIndustryQuoteRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    snapshot_date = parse_date(request.params.get("snapshot_date") or request.params.get("trade_date")) or date.today()
    kind = str(request.params.get("kind") or "industries")
    if kind != "industries":
        raise ValueError("build_market_heatmap currently supports kind=industries")
    source = str(request.params.get("source") or "tdx_industry_quote_snapshot")
    quote_source = str(request.params.get("quote_source") or "eltdx")
    top_n = parse_int(request.params.get("top_n") or request.params.get("topN"), 200)
    rows = _parse_rows(request.params.get("rows"))
    create_table = truthy(request.params.get("create_table"))
    result = {
        "pipeline": "market.build_market_heatmap",
        "dry_run": request.dry_run,
        "snapshot_date": snapshot_date.isoformat(),
        "kind": kind,
        "source": source,
        "rows_read": 0,
        "rows_written": 0,
    }
    if request.dry_run:
        planned_rows = rows or _sample_rows()
        payload = build_industry_heatmap_payload(
            planned_rows,
            snapshot_date=snapshot_date,
            top_n=top_n,
            source=source,
        )
        return {**result, "rows_read": len(planned_rows), "payload": payload, "notes": ["dry_run skipped Postgres access"]}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    quote_rows = rows if rows is not None else TdxIndustryQuoteRepository(pg).get_snapshot(snapshot_date, source=quote_source)
    if not quote_rows:
        return {**result, "skipped": True, "skip_reason": "no_industry_quote_rows"}
    payload = build_industry_heatmap_payload(
        quote_rows,
        snapshot_date=snapshot_date,
        top_n=top_n,
        source=f"postgres.tdx_industry_quote_snapshot:{quote_source}" if rows is None else source,
    )
    repo = MarketHeatmapRepository(pg)
    if create_table:
        repo.create_table()
    saved = repo.upsert_snapshot(snapshot_date=snapshot_date, kind=kind, source=source, payload=payload)
    result["rows_read"] = len(quote_rows)
    result["rows_written"] = int(saved.get("item_count") or len(payload.get("items") or []))
    result["payload"] = payload
    return result


def _parse_rows(value: Any) -> list[dict[str, Any]] | None:
    if value in (None, ""):
        return None
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, list):
        raise ValueError("rows must be a JSON array")
    return [dict(row) for row in raw if isinstance(row, dict)]


def _sample_rows() -> list[dict[str, Any]]:
    return [
        {"code6": "880493", "name": "软件服务", "full_code": "sh880493", "change_pct": 2.4, "amount": 3200000000, "last_price": 1500},
        {"code6": "880471", "name": "银行", "full_code": "sh880471", "change_pct": -0.5, "amount": 2200000000, "last_price": 980},
    ]
