from __future__ import annotations

import json
from datetime import date, datetime
from pathlib import Path
from typing import Any

from cynexus_data.domain.stock.ths_industry_constituents import normalize_industry_constituent_snapshot
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.fund_flow import SectorFundFlowRepository
from cynexus_data.storage.postgres.ths_industry_constituents import ThsIndustryConstituentRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    snapshot_date = _parse_date(request.params.get("snapshot_date") or request.params.get("trade_date")) or date.today()
    industry_codes = _normalize_codes(request.params.get("industry_codes") or request.params.get("codes"))
    history_path = request.params.get("history_path")
    create_table = _truthy(request.params.get("create_table"))
    max_pages = _optional_int(request.params.get("max_pages"))
    page_sleep = float(request.params.get("page_sleep") or 0.4)
    source = "q.10jqka.com.cn"
    result = {
        "pipeline": "stock_basic.sync_ths_industry_constituents",
        "dry_run": request.dry_run,
        "snapshot_date": snapshot_date.isoformat(),
        "history_path": str(history_path) if history_path else None,
        "code_source": "params" if industry_codes else None,
        "planned_industry_codes": len(industry_codes or []),
        "rows_read": 0,
        "rows_written": 0,
        "snapshots_written": 0,
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped THS and Postgres access"]
        if history_path:
            result["planned_payloads"] = len(_discover_history_files(Path(str(history_path)), snapshot_date=snapshot_date, industry_codes=industry_codes))
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    pg = context.hooks.postgres.get_client()
    repo = ThsIndustryConstituentRepository(pg)
    if create_table:
        repo.create_table()
    if not industry_codes:
        industry_codes, code_source = _resolve_industry_codes(repo, SectorFundFlowRepository(pg))
        result["code_source"] = code_source
        result["planned_industry_codes"] = len(industry_codes or [])

    payloads = (
        _load_history_payloads(Path(str(history_path)), snapshot_date=snapshot_date, industry_codes=industry_codes)
        if history_path
        else _fetch_live_payloads(context, industry_codes=industry_codes, max_pages=max_pages, page_sleep=page_sleep)
    )
    if not payloads and not history_path:
        raise ValueError("industry_codes/codes are required for live THS constituents sync")

    records: list[dict[str, Any]] = []
    page_row_counts: dict[str, list[int]] = {}
    requested_codes: list[str] = []
    fetched_at: datetime | None = None
    for payload in payloads:
        industry_code = str(payload.get("code") or payload.get("industryCode") or "").strip()
        if not industry_code:
            continue
        requested_codes.append(industry_code)
        payload_fetched_at = _parse_datetime(payload.get("fetchedAt"))
        if payload_fetched_at and fetched_at is None:
            fetched_at = payload_fetched_at
        rows = list(payload.get("rows") or [])
        page_row_counts[industry_code] = [int(v or 0) for v in payload.get("pageRowCounts") or []]
        records.extend(
            normalize_industry_constituent_snapshot(
                rows,
                industry_code=industry_code,
                snapshot_date=snapshot_date,
                source=source,
                source_type="json_import" if history_path else "crawler",
                captured_at=payload_fetched_at,
            )
        )
        result["rows_read"] += len(rows)
        result["snapshots_written"] += 1

    written = repo.replace_snapshot(
        snapshot_date=snapshot_date,
        records=records,
        requested_industry_codes=requested_codes,
        source=source,
        source_type="json_import" if history_path else "crawler",
        page_row_counts=page_row_counts,
        fetched_at=fetched_at,
        extra={"historyPath": str(history_path) if history_path else None, "maxPages": max_pages},
    )
    result["planned_industry_codes"] = len(requested_codes)
    result["rows_written"] = written
    return result


def _resolve_industry_codes(
    repo: ThsIndustryConstituentRepository,
    sector_repo: SectorFundFlowRepository,
) -> tuple[list[str] | None, str | None]:
    codes = repo.list_latest_industry_codes()
    if codes:
        return codes, "ths_industry_constituent_snapshot"
    codes = sector_repo.latest_sector_codes(scope="industry", source="ths.10jqka.com.cn")
    if codes:
        return codes, "sector_fund_flow_daily_snapshots"
    return None, None


def _fetch_live_payloads(
    context: PipelineContext,
    *,
    industry_codes: list[str] | None,
    max_pages: int | None,
    page_sleep: float,
) -> list[dict[str, Any]]:
    if not industry_codes:
        return []
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    client = context.hooks.ths.get_client()
    return [
        client.fetch_industry_constituents_all(code, max_pages=max_pages, page_sleep=page_sleep)
        for code in industry_codes
    ]


def _discover_history_files(path: Path, *, snapshot_date: date, industry_codes: list[str] | None) -> list[Path]:
    allowed = set(industry_codes or [])
    if path.is_file():
        return [path] if not allowed or path.stem in allowed else []
    if not path.exists():
        return []
    dated_dir = path / snapshot_date.isoformat()
    search_root = dated_dir if dated_dir.exists() else path
    files = sorted(search_root.glob("*.json"))
    if allowed:
        files = [file_path for file_path in files if file_path.stem in allowed]
    return files


def _load_history_payloads(path: Path, *, snapshot_date: date, industry_codes: list[str] | None) -> list[dict[str, Any]]:
    payloads: list[dict[str, Any]] = []
    for file_path in _discover_history_files(path, snapshot_date=snapshot_date, industry_codes=industry_codes):
        with file_path.open("r", encoding="utf-8") as fp:
            payload = json.load(fp)
        if isinstance(payload, dict):
            payload = dict(payload)
            payload.setdefault("code", file_path.stem)
            payloads.append(payload)
    return payloads


def _normalize_codes(value: Any) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


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
