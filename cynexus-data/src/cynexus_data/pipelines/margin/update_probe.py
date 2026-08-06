from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.margin import MarginRepository

from cynexus_data.pipelines.margin import sync_margin


DEFAULT_PROVIDERS = ["exchange", "akshare"]
DEFAULT_DATASETS = ["summary", "detail", "account"]
FRESHNESS_DATASETS = ("summary", "detail")
SYNC_LOCK_KEY = "job:sync_margin:margin"


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    providers = sync_margin._normalize_list(request.params.get("providers") or DEFAULT_PROVIDERS)
    datasets = sync_margin._normalize_list(request.params.get("datasets") or DEFAULT_DATASETS)
    clients = sync_margin._provider_clients(context, providers)
    source_trade_date = sync_margin._latest_trade_date(clients, date.today())

    repo = MarginRepository(context.hooks.postgres.get_client())
    if sync_margin._truthy(request.params.get("create_table", True)):
        repo.create_table()
    stored_dates = repo.latest_trade_dates()
    freshness = {
        dataset: stored_dates.get(dataset).isoformat() if stored_dates.get(dataset) else None
        for dataset in FRESHNESS_DATASETS
    }

    if source_trade_date is None:
        return _skipped_result(
            source_trade_date=None,
            freshness=freshness,
            reason="source_not_updated",
        )

    lagging = [
        dataset
        for dataset in FRESHNESS_DATASETS
        if stored_dates.get(dataset) is None or stored_dates[dataset] < source_trade_date
    ]
    if not lagging:
        return _skipped_result(
            source_trade_date=source_trade_date,
            freshness=freshness,
            reason="already_current",
        )

    probe_metadata = {
        "source_trade_date": source_trade_date.isoformat(),
        "stored_trade_dates": freshness,
        "lagging_datasets": lagging,
        "provider": providers[0] if providers else None,
    }
    if request.dry_run:
        return {
            "pipeline": "margin.update_probe",
            "dry_run": True,
            "status": "skipped",
            "success": True,
            "skip_reason": "would_sync",
            **probe_metadata,
        }

    owner = request.trigger_request_id or "margin_update_probe"
    with context.locks.acquire(SYNC_LOCK_KEY, owner, ttl_seconds=7200) as acquired:
        if not acquired:
            return _skipped_result(
                source_trade_date=source_trade_date,
                freshness=freshness,
                reason="sync_locked",
                lagging_datasets=lagging,
            )
        sync_result = sync_margin.run(
            context,
            JobRequest(
                job_key="sync_margin",
                trigger_request_id=f"{owner}:sync:{source_trade_date.isoformat()}",
                triggered_by="margin_update_probe",
                params={
                    "trade_date": source_trade_date.isoformat(),
                    "providers": providers,
                    "provider_priority": providers,
                    "datasets": datasets,
                    "create_table": False,
                    "enrich_detail": request.params.get("enrich_detail", True),
                },
            ),
        )

    rows_written = int(sync_result.get("rows_written") or 0)
    if rows_written <= 0:
        return {
            "pipeline": "margin.update_probe",
            "status": "failed",
            "success": False,
            "errors": ["source reported a newer trade date but margin sync wrote no rows"],
            **probe_metadata,
            "sync_result": sync_result,
        }
    return {
        "pipeline": "margin.update_probe",
        "status": "success",
        "success": True,
        "rows_read": int(sync_result.get("rows_read") or 0),
        "rows_written": rows_written,
        **probe_metadata,
        "sync_result": sync_result,
    }


def _skipped_result(
    *,
    source_trade_date: date | None,
    freshness: dict[str, str | None],
    reason: str,
    lagging_datasets: list[str] | None = None,
) -> dict[str, Any]:
    return {
        "pipeline": "margin.update_probe",
        "status": "skipped",
        "success": True,
        "skip_reason": reason,
        "source_trade_date": source_trade_date.isoformat() if source_trade_date else None,
        "stored_trade_dates": freshness,
        "lagging_datasets": lagging_datasets or [],
        "rows_read": 0,
        "rows_written": 0,
    }
