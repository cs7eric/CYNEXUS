from __future__ import annotations

import json
from datetime import date, datetime
from pathlib import Path
from typing import Any

from cynexus_data.domain.market.fund_flow import normalize_sector_fund_flow_snapshot
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.fund_flow import SectorFundFlowRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    scope = str(request.params.get("scope") or "industry")
    provider = str(request.params.get("provider") or request.params.get("providers") or "ths").split(",")[0].strip()
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    history_path = request.params.get("history_path")
    create_table = _truthy(request.params.get("create_table"))
    max_pages = _optional_int(request.params.get("max_pages"))
    page_sleep = float(request.params.get("page_sleep") or 0.4)
    source = "ths.10jqka.com.cn" if provider == "ths" else provider
    result = {
        "pipeline": "fund_flow.sync_sector_flow",
        "dry_run": request.dry_run,
        "scope": scope,
        "provider": provider,
        "trade_date": trade_date.isoformat(),
        "history_path": str(history_path) if history_path else None,
        "rows_read": 0,
        "rows_written": 0,
        "snapshots_written": 0,
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped THS and Postgres access"]
        if history_path:
            result["planned_payloads"] = len(_discover_history_files(Path(str(history_path)), trade_date=trade_date))
        return result
    if provider != "ths":
        raise ValueError(f"unsupported sector fund-flow provider: {provider}")
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    pg = context.hooks.postgres.get_client()
    repo = SectorFundFlowRepository(pg)
    if create_table:
        repo.create_table()

    payloads = (
        _load_history_payloads(Path(str(history_path)), trade_date=trade_date)
        if history_path
        else [_fetch_live_payload(context, trade_date=trade_date, max_pages=max_pages, page_sleep=page_sleep)]
    )
    for payload_trade_date, payload in payloads:
        fetched_at = _parse_datetime(payload.get("fetchedAt"))
        rows = list(payload.get("rows") or [])
        records = normalize_sector_fund_flow_snapshot(
            rows,
            trade_date=payload_trade_date,
            scope=scope,
            source=source,
            source_type="json_import" if history_path else "crawler",
            captured_at=fetched_at,
        )
        written = repo.replace_snapshot(
            trade_date=payload_trade_date,
            records=records,
            scope=scope,
            source=source,
            source_type="json_import" if history_path else "crawler",
            total_pages=_optional_int(payload.get("totalPages")),
            page_row_counts=[int(v or 0) for v in payload.get("pageRowCounts") or []],
            fetched_at=fetched_at,
            extra={"rowCount": payload.get("rowCount"), "historyPath": str(history_path) if history_path else None},
        )
        result["rows_read"] += len(rows)
        result["rows_written"] += written
        result["snapshots_written"] += 1
    return result


def _fetch_live_payload(context: PipelineContext, *, trade_date: date, max_pages: int | None, page_sleep: float) -> tuple[date, dict[str, Any]]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    client = context.hooks.ths.get_client()
    return trade_date, client.fetch_industry_fund_flow_all(max_pages=max_pages, page_sleep=page_sleep)


def _discover_history_files(path: Path, *, trade_date: date) -> list[Path]:
    if path.is_file():
        return [path]
    if not path.exists():
        return []
    exact = path / f"{trade_date.isoformat()}.json"
    if exact.exists():
        return [exact]
    return sorted(path.glob("*.json"))


def _load_history_payloads(path: Path, *, trade_date: date) -> list[tuple[date, dict[str, Any]]]:
    payloads: list[tuple[date, dict[str, Any]]] = []
    for file_path in _discover_history_files(path, trade_date=trade_date):
        try:
            payload_trade_date = date.fromisoformat(file_path.stem)
        except ValueError:
            payload_trade_date = trade_date
        with file_path.open("r", encoding="utf-8") as fp:
            payload = json.load(fp)
        if isinstance(payload, dict):
            payloads.append((payload_trade_date, payload))
    return payloads


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value))


def _parse_datetime(value: Any) -> datetime | None:
    if value in (None, ""):
        return None
    if isinstance(value, datetime):
        return value
    try:
        return datetime.fromisoformat(str(value))
    except ValueError:
        return None


def _optional_int(value: Any) -> int | None:
    if value in (None, ""):
        return None
    return int(value)


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
