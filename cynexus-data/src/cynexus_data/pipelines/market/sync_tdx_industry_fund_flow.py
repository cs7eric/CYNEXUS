from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.index_codes import TDX_INDUSTRY_56, TDX_INDUSTRY_SEEDS
from cynexus_data.domain.market.tdx_industry_fund_flow import build_tdx_industry_fund_flow_snapshots
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.sector_quote import create_eltdx_sector_quote_client, fetch_tdx_industry_sector_quote_details
from cynexus_data.storage.postgres.tdx_industry_fund_flow import TdxIndustryFundFlowRepository


DEFAULT_SOURCE = "eltdx.f10.theme_market.200742"


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    snapshot_date = parse_date(request.params.get("snapshot_date") or request.params.get("trade_date")) or date.today()
    source = str(request.params.get("source") or DEFAULT_SOURCE)
    codes = _parse_csv(request.params.get("codes") or request.params.get("code"))
    mapping = _filter_mapping(TDX_INDUSTRY_56, codes)
    rows = _parse_rows(request.params.get("rows"))
    create_table = truthy(request.params.get("create_table"))
    result = {
        "pipeline": "market.sync_tdx_industry_fund_flow",
        "dry_run": request.dry_run,
        "snapshot_date": snapshot_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
        "planned_industries": len(mapping),
    }
    if request.dry_run:
        planned_rows = rows or _sample_rows(mapping, snapshot_date=snapshot_date)
        snapshots = build_tdx_industry_fund_flow_snapshots(planned_rows, snapshot_date=snapshot_date, source=source)
        return {
            **result,
            "rows_read": len(planned_rows),
            "planned_rows": len(snapshots),
            "sample": snapshots[:3],
            "notes": ["dry_run skipped eltdx and Postgres access"],
        }
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    if rows is None:
        pool_size = parse_int(request.params.get("pool_size"), 4)
        timeout = float(request.params.get("timeout") or request.params.get("timeout_seconds") or 8.0)
        client = create_eltdx_sector_quote_client(pool_size=pool_size, timeout=timeout)
        try:
            rows = fetch_tdx_industry_sector_quote_details(
                client,
                mapping=mapping,
                seed_mapping=TDX_INDUSTRY_SEEDS,
                datasets=["main_capital_flow"],
            )
        finally:
            close = getattr(client, "close", None)
            if callable(close):
                close()
            else:
                disconnect = getattr(client, "disconnect", None)
                if callable(disconnect):
                    disconnect()
    snapshots = build_tdx_industry_fund_flow_snapshots(rows, snapshot_date=snapshot_date, source=source)
    if not snapshots:
        return {**result, "skipped": True, "skip_reason": "no_tdx_industry_fund_flow_rows"}
    repo = TdxIndustryFundFlowRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    write_result = repo.replace_snapshot(trade_date=snapshot_date, rows=snapshots, source=source)
    result["rows_read"] = len(rows)
    result["rows_written"] = write_result["rows_written"]
    result["ok_count"] = write_result["ok_count"]
    result["empty_count"] = write_result["empty_count"]
    result["error_count"] = write_result["error_count"]
    return result


def _parse_rows(value: Any) -> list[dict[str, Any]] | None:
    if value in (None, ""):
        return None
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, list):
        raise ValueError("rows must be a JSON array")
    return [dict(item) for item in raw if isinstance(item, dict)]


def _parse_csv(value: Any) -> list[str] | None:
    if value in (None, ""):
        return None
    if isinstance(value, list):
        parts = value
    else:
        parts = str(value).split(",")
    cleaned = [str(part).strip() for part in parts if str(part).strip()]
    return cleaned or None


def _filter_mapping(mapping: dict[str, str], codes: list[str] | None) -> dict[str, str]:
    if not codes:
        return dict(mapping)
    wanted = {_strip_market_prefix(code) for code in codes}
    return {code6: name for code6, name in mapping.items() if code6 in wanted}


def _strip_market_prefix(value: str) -> str:
    text = str(value or "").strip().lower()
    for prefix in ("sh", "sz", "bj"):
        if text.startswith(prefix):
            return text[len(prefix) :]
    return text


def _sample_rows(mapping: dict[str, str], *, snapshot_date: date) -> list[dict[str, Any]]:
    trade_text = snapshot_date.strftime("%Y%m%d")
    rows: list[dict[str, Any]] = []
    for code6, name in list(mapping.items())[:3]:
        rows.append(
            {
                "code6": code6,
                "name": name,
                "seed_code": (TDX_INDUSTRY_SEEDS.get(code6) or ["sh600000"])[0],
                "ok": True,
                "sections": {
                    "main_capital_flow": [
                        {
                            "sector_name": name,
                            "date": trade_text,
                            "main_net": 1250000.0,
                            "large_order_net": 880000.0,
                            "medium_order_net": 250000.0,
                            "small_order_net": 120000.0,
                        }
                    ]
                },
                "source": DEFAULT_SOURCE,
            }
        )
    return rows
