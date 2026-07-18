from __future__ import annotations

from datetime import datetime, timezone

from cynexus_data.domain.market.market_fund_flow import normalize_market_fund_flow_row
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.market_overview import MarketOverviewRepository


SOURCE = "eastmoney.market_fund_flow"


def run(context: PipelineContext, request: JobRequest) -> dict:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    limit = _int(request.params.get("limit")) or 120
    client = context.hooks.eastmoney.get_client()
    fetched_at = datetime.now(timezone.utc).isoformat()
    try:
        raw_rows = client.fetch_market_fund_flow(limit=limit)
    except Exception as exc:  # noqa: BLE001 - return a failed job result and preserve existing values.
        fetch_metadata = dict(getattr(client, "last_market_fund_flow_metadata", {}) or {})
        error = str(fetch_metadata.get("error") or exc)
        result = {
            "pipeline": "market.sync_market_fund_flow",
            "dry_run": request.dry_run,
            "source": SOURCE,
            "requested_limit": limit,
            "rows_read": 0,
            "rows_written": 0,
            "trade_date_start": None,
            "trade_date_end": None,
            "latest": None,
            "fallback_used": bool(fetch_metadata.get("fallback_used", True)),
            "source_mode": str(fetch_metadata.get("source_mode") or "failed"),
            "stale": True,
            "error": error,
            "fetched_at": fetched_at,
            "last_successful_trade_date": None,
            "success": False,
            "status": "failed",
            "errors": [error],
        }
        if not request.dry_run:
            repository = MarketOverviewRepository(context.hooks.postgres.get_client())
            if _truthy(request.params.get("create_table")):
                repository.create_table()
            result["last_successful_trade_date"] = repository.record_source_failure(
                source=SOURCE,
                error=error,
                attempted_at=fetched_at,
            )
        return result

    fetch_metadata = dict(getattr(client, "last_market_fund_flow_metadata", {}) or {})
    rows = [normalize_market_fund_flow_row(row) for row in raw_rows]
    rows.sort(key=lambda row: row["trade_date"])
    latest_trade_date = rows[-1]["trade_date"].isoformat() if rows else None

    result = {
        "pipeline": "market.sync_market_fund_flow",
        "dry_run": request.dry_run,
        "source": SOURCE,
        "requested_limit": limit,
        "rows_read": len(rows),
        "rows_written": 0,
        "trade_date_start": rows[0]["trade_date"].isoformat() if rows else None,
        "trade_date_end": latest_trade_date,
        "latest": rows[-1] if rows else None,
        "fallback_used": bool(fetch_metadata.get("fallback_used", False)),
        "source_mode": str(fetch_metadata.get("source_mode") or "unknown"),
        "stale": False,
        "error": None,
        "fetched_at": fetched_at,
        "last_successful_trade_date": latest_trade_date,
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped PostgreSQL writes"]
        return result

    repository = MarketOverviewRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repository.create_table()
    for row in rows:
        repository.upsert_fields(
            row,
            source=SOURCE,
            clear_manual_override=True,
            extra={
                "job": request.job_key,
                "provider": "eastmoney",
                "raw_payload": row.get("raw_payload"),
                "market_fund_flow": {
                    "fallback_used": result["fallback_used"],
                    "source_mode": result["source_mode"],
                    "stale": False,
                    "error": None,
                    "fetched_at": fetched_at,
                    "last_successful_fetched_at": fetched_at,
                    "last_successful_trade_date": latest_trade_date,
                },
            },
        )
    result["rows_written"] = len(rows)
    return result


def _int(value) -> int | None:
    if value in (None, ""):
        return None
    return int(value)


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
