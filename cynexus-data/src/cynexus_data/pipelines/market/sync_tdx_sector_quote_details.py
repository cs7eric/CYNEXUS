from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.index_codes import TDX_INDUSTRY_56, TDX_INDUSTRY_SEEDS
from cynexus_data.domain.market.sector_quote import build_sector_quote_detail_snapshots
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.sector_quote import create_eltdx_sector_quote_client, fetch_tdx_industry_sector_quote_details
from cynexus_data.storage.postgres.sector_quote import TdxSectorQuoteDetailRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    snapshot_date = parse_date(request.params.get("snapshot_date") or request.params.get("trade_date")) or date.today()
    source = str(request.params.get("source") or "eltdx.f10.theme_market")
    datasets = _parse_csv(request.params.get("datasets") or request.params.get("dataset"))
    codes = _parse_csv(request.params.get("codes") or request.params.get("code"))
    mapping = _filter_mapping(_parse_mapping(request.params.get("mapping")) or TDX_INDUSTRY_56, codes)
    rows = _parse_rows(request.params.get("rows"))
    create_table = truthy(request.params.get("create_table"))
    result = {
        "pipeline": "market.sync_tdx_sector_quote_details",
        "dry_run": request.dry_run,
        "snapshot_date": snapshot_date.isoformat(),
        "rows_read": 0,
        "rows_written": 0,
        "planned_industries": len(mapping),
    }
    if request.dry_run:
        planned_rows = rows or _sample_rows(mapping, datasets=datasets)
        snapshots = build_sector_quote_detail_snapshots(planned_rows, snapshot_date=snapshot_date)
        return {
            **result,
            "rows_read": len(planned_rows),
            "planned_rows": len(snapshots),
            "sample": snapshots[:2],
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
                datasets=datasets,
            )
        finally:
            close = getattr(client, "close", None)
            if callable(close):
                close()
    snapshots = build_sector_quote_detail_snapshots(rows, snapshot_date=snapshot_date)
    if not snapshots:
        return {**result, "skipped": True, "skip_reason": "no_sector_quote_detail_rows"}
    repo = TdxSectorQuoteDetailRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    result["rows_read"] = len(rows)
    result["rows_written"] = repo.replace_snapshots(snapshot_date=snapshot_date, rows=snapshots, source=source)
    return result


def _parse_rows(value: Any) -> list[dict[str, Any]] | None:
    if value in (None, ""):
        return None
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
    return {_strip_market_prefix(str(code)): str(name) for code, name in raw.items()}


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
    return {code6: name for code6, name in mapping.items() if str(code6) in wanted}


def _strip_market_prefix(value: str) -> str:
    text = str(value or "").strip().lower()
    for prefix in ("sh", "sz", "bj"):
        if text.startswith(prefix):
            return text[len(prefix) :]
    return text


def _sample_rows(mapping: dict[str, str], *, datasets: list[str] | None = None) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    requested = set(datasets or ["period_stats", "main_capital_flow", "related", "constituents", "main_control"])
    for code6, name in list(mapping.items())[:2]:
        sections = {
            "period_stats": [{"range_code": "0", "range_label": "近1日", "value1": 1.2, "value2": 0.8, "value3": 0}],
            "main_capital_flow": [{"sector_name": name, "date": "2026-06-26", "main_net": 1.5}],
            "related": [{"market": "SH", "sector_code": code6, "sector_name": name, "change_pct": 1.2, "limit_up_count": 2}],
            "constituents": [{"market": "SH", "code": "600000", "name": "样例股票", "change_pct": 1.0, "last_price": 10.0}],
            "main_control": [{"date": "2026-06-26", "control_pct": 12.3}],
        }
        rows.append(
            {
                "code6": code6,
                "name": name,
                "seed_code": (TDX_INDUSTRY_SEEDS.get(code6) or ["sh600000"])[0],
                "ok": True,
                "sections": {key: value for key, value in sections.items() if key in requested},
                "source": "eltdx.f10.theme_market",
            }
        )
    return rows
