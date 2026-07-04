from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.margin import MarginRepository


SUPPORTED_PROVIDERS = {"akshare"}
SUPPORTED_DATASETS = {"summary", "detail", "account"}


def run(context: PipelineContext, request: JobRequest) -> dict:
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    providers = _normalize_list(request.params.get("providers") or ["akshare"])
    datasets = _normalize_list(request.params.get("datasets") or ["summary", "detail", "account"])
    unsupported_providers = [provider for provider in providers if provider not in SUPPORTED_PROVIDERS]
    unsupported_datasets = [dataset for dataset in datasets if dataset not in SUPPORTED_DATASETS]
    if unsupported_providers:
        raise ValueError(f"unsupported margin providers: {unsupported_providers}")
    if unsupported_datasets:
        raise ValueError(f"unsupported margin datasets: {unsupported_datasets}")

    result = {
        "pipeline": "margin.sync_margin",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "providers": providers,
        "datasets": datasets,
        "rows_read": 0,
        "rows_written": 0,
        "summary_rows": 0,
        "detail_rows": 0,
        "account_rows": 0,
    }
    if request.dry_run:
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    akshare = context.hooks.akshare.get_client()
    repo = MarginRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repo.create_table()

    if "summary" in datasets:
        rows = akshare.fetch_margin_market_summary(trade_date)
        result["rows_read"] += len(rows)
        result["summary_rows"] = repo.replace_market_summary(trade_date=trade_date, rows=rows)

    if "detail" in datasets:
        rows = akshare.fetch_margin_stock_detail(trade_date)
        result["rows_read"] += len(rows)
        result["detail_rows"] = repo.replace_stock_detail(trade_date=trade_date, rows=rows)

    if "account" in datasets:
        rows = akshare.fetch_margin_account_stats(trade_date)
        result["rows_read"] += len(rows)
        result["account_rows"] = repo.replace_account_stats(rows=rows)

    result["rows_written"] = result["summary_rows"] + result["detail_rows"] + result["account_rows"]
    return result


def _normalize_list(value: Any) -> list[str]:
    if value in (None, "", []):
        return []
    if isinstance(value, str):
        return [item.strip().lower() for item in value.split(",") if item.strip()]
    return [str(item).strip().lower() for item in value if str(item).strip()]


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    text = str(value).strip()
    if len(text) == 8 and text.isdigit():
        return date.fromisoformat(f"{text[:4]}-{text[4:6]}-{text[6:]}")
    return date.fromisoformat(text)


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}
