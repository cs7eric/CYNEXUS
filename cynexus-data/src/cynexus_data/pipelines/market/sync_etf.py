from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Any

from cynexus_data.domain.market.etf import normalize_spot_row
from cynexus_data.domain.market.etf_mapping import build_etf_mapping_rows_for_catalog
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.akshare.etf import (
    fetch_etf_classification,
    fetch_etf_share_sse,
    fetch_etf_share_szse,
    fetch_etf_spot,
)
from cynexus_data.storage.postgres.market_etf import MarketEtfRepository


SOURCE = "akshare.fund_etf_spot_em"
SHARE_SOURCE = "exchange.sse_szse.etf_share"


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    trade_date = _parse_date(request.params.get("trade_date") or request.params.get("snapshot_date")) or date.today()
    captured_at = datetime.now(timezone.utc).isoformat()
    if request.dry_run:
        return {
            "pipeline": "market.sync_etf",
            "dry_run": True,
            "trade_date": trade_date.isoformat(),
            "rows_read": 2,
            "rows_written": 0,
            "sample": [{"code": "510300", "category": "broad"}, {"code": "512480", "category": "sector"}],
            "notes": ["dry_run skipped AkShare and PostgreSQL access"],
        }

    ak = context.hooks.akshare.get_client().require_module()
    raw_rows = fetch_etf_spot(ak)
    trade_date = _parse_date(request.params.get("trade_date") or request.params.get("snapshot_date")) or _infer_trade_date(raw_rows) or date.today()
    classification_map: dict[str, dict[str, str]] = {}
    classification_errors: list[str] = []
    if _truthy(request.params.get("refresh_classification")):
        classification_map, classification_errors = fetch_etf_classification(ak)
    rows = [normalize_spot_row(row, captured_at=captured_at, classification_map=classification_map) for row in raw_rows]
    rows = [row for row in rows if row["code"] and row["category"] in {"broad", "sector", "other"}]
    if not rows:
        raise RuntimeError("AkShare ETF spot returned no rows")

    share_map: dict[str, float | None] = {}
    share_errors: list[str] = []
    try:
        share_map.update(fetch_etf_share_sse(ak, trade_date))
    except Exception as exc:  # noqa: BLE001 - keep spot data usable if an exchange endpoint is unavailable.
        share_errors.append(f"SSE: {exc}")
    try:
        share_map.update(fetch_etf_share_szse(ak))
    except Exception as exc:  # noqa: BLE001 - keep spot data usable if an exchange endpoint is unavailable.
        share_errors.append(f"SZSE: {exc}")
    for row in rows:
        if row["code"] in share_map:
            row["latest_shares"] = share_map[row["code"]]

    repository = MarketEtfRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repository.create_table()
    rows_written = repository.upsert_snapshot(rows, trade_date=trade_date, source=SOURCE)
    daily_written = repository.upsert_daily(rows, trade_date=trade_date, source=SHARE_SOURCE)
    catalog_written = repository.upsert_catalog(rows, source=SOURCE)
    mapping_written = repository.upsert_mappings(build_etf_mapping_rows_for_catalog(rows))
    return {
        "pipeline": "market.sync_etf",
        "dry_run": False,
        "trade_date": trade_date.isoformat(),
        "source": SOURCE,
        "share_source": SHARE_SOURCE,
        "rows_read": len(rows),
        "rows_written": rows_written + daily_written + catalog_written,
        "snapshot_rows_written": rows_written,
        "daily_rows_written": daily_written,
        "catalog_rows_written": catalog_written,
        "mapping_rows_written": mapping_written,
        "broad_count": sum(row["category"] == "broad" for row in rows),
        "sector_count": sum(row["category"] == "sector" for row in rows),
        "share_rows": len(share_map),
        "share_errors": share_errors,
        "classification_rows": len(classification_map),
        "classification_errors": classification_errors,
        "classification_fallback_rows": sum(row["classification_source"] == "fallback.name_keyword" for row in rows),
        "captured_at": captured_at,
    }


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    return date.fromisoformat(str(value)[:10])


def _infer_trade_date(rows: list[dict[str, Any]]) -> date | None:
    candidates = [_parse_date(row.get("数据日期") or row.get("trade_date")) for row in rows]
    valid = [value for value in candidates if value is not None]
    return max(valid) if valid else None


def _truthy(value: Any) -> bool:
    return value is True or str(value).lower() in {"1", "true", "yes", "y", "on"}
