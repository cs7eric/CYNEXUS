from __future__ import annotations

import json
from datetime import date, timedelta
from typing import Any

from cynexus_data.domain.market.index_returns import INDEX_TARGETS, calculate_index_returns
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.clickhouse.readers import read_index_daily_bars
from cynexus_data.storage.postgres.index_returns import IndexReturnRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    window_days = parse_int(request.params.get("window_days") or request.params.get("days"), 5)
    source_table = str(request.params.get("source_table") or "index_daily_raw")
    lookback_days = parse_int(request.params.get("lookback_days"), window_days + 1)
    create_table = truthy(request.params.get("create_table"))
    targets = _parse_targets(request.params.get("targets")) or INDEX_TARGETS
    rows_by_code = _parse_rows_by_code(request.params.get("rows_by_code") or request.params.get("rows"))
    notes: list[str] = []

    if request.dry_run:
        if not rows_by_code:
            rows_by_code = {target["fullCode"]: _sample_index_bars(target["fullCode"], max(lookback_days, window_days + 1)) for target in targets}
            notes.append("dry_run used synthetic index rows")
    else:
        if context.hooks is None:
            raise RuntimeError("PipelineContext.hooks is required")
        if not rows_by_code:
            ch = context.hooks.clickhouse.get_client()
            rows_by_code = {
                target["fullCode"]: read_index_daily_bars(
                    ch,
                    target["fullCode"],
                    as_of_date=trade_date,
                    days=max(lookback_days, window_days + 1),
                    table=source_table,
                )
                for target in targets
            }

    items = calculate_index_returns(rows_by_code, window_days=window_days, targets=targets)
    result = {
        "pipeline": "market.build_index_returns",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "window_days": window_days,
        "source_table": source_table,
        "rows_read": sum(len(rows) for rows in rows_by_code.values()),
        "rows_written": 0,
        "items": items,
        "notes": notes,
    }
    if request.dry_run:
        return result

    repo = IndexReturnRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    result["rows_written"] = repo.save_many(trade_date=trade_date, window_days=window_days, items=items, source=f"clickhouse.{source_table}")
    return result


def _parse_targets(value: Any) -> list[dict[str, str]] | None:
    if value in (None, ""):
        return None
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, list):
        raise ValueError("targets must be a JSON array")
    out: list[dict[str, str]] = []
    for item in raw:
        if not isinstance(item, dict):
            continue
        full_code = str(item.get("fullCode") or item.get("full") or "").strip()
        if not full_code:
            continue
        out.append(
            {
                "name": str(item.get("name") or full_code),
                "code": str(item.get("code") or full_code[2:] if full_code[:2].lower() in {"sh", "sz", "bj"} else full_code),
                "fullCode": full_code,
                "exchange": str(item.get("exchange") or full_code[:2].lower()),
            }
        )
    return out


def _parse_rows_by_code(value: Any) -> dict[str, list[dict[str, Any]]]:
    if value in (None, ""):
        return {}
    raw = json.loads(value) if isinstance(value, str) else value
    if not isinstance(raw, dict):
        raise ValueError("rows_by_code must be a JSON object")
    return {str(code): [dict(row) for row in rows if isinstance(row, dict)] for code, rows in raw.items() if isinstance(rows, list)}


def _sample_index_bars(code: str, count: int) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    start = date(2026, 1, 1)
    price = 3000.0 + (sum(ord(ch) for ch in code) % 300)
    for index in range(count):
        price += 3.5 + ((index % 7) - 3) * 0.8
        rows.append(
            {
                "code": code,
                "trade_date": (start + timedelta(days=index)).isoformat(),
                "open": round(price - 4, 4),
                "high": round(price + 8, 4),
                "low": round(price - 8, 4),
                "close": round(price, 4),
                "volume": 100000 + index * 100,
                "amount": round((100000 + index * 100) * price, 4),
            }
        )
    return rows
