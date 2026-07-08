from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.margin import MarginRepository


SUPPORTED_PROVIDERS = {"exchange", "akshare"}
SUPPORTED_DATASETS = {"summary", "detail", "account"}


def run(context: PipelineContext, request: JobRequest) -> dict:
    requested_trade_date = _parse_date(request.params.get("trade_date"))
    trade_date = requested_trade_date or date.today()
    providers = _normalize_list(request.params.get("providers") or request.params.get("provider_priority") or ["exchange", "akshare"])
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
        "provider_results": {},
    }
    if request.dry_run:
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    clients = _provider_clients(context, providers)
    if requested_trade_date is None:
        trade_date = _latest_trade_date(clients, date.today()) or trade_date
        result["trade_date"] = trade_date.isoformat()
    repo = MarginRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repo.create_table()

    if "summary" in datasets:
        provider, rows = _fetch_with_fallback(clients, "fetch_margin_market_summary", trade_date)
        result["provider_results"]["summary"] = provider
        result["rows_read"] += len(rows)
        result["summary_rows"] = repo.replace_market_summary(trade_date=trade_date, rows=rows)

    if "detail" in datasets:
        provider, rows = _fetch_with_fallback(clients, "fetch_margin_stock_detail", trade_date)
        result["provider_results"]["detail"] = provider
        result["rows_read"] += len(rows)
        result["detail_rows"] = repo.replace_stock_detail(trade_date=trade_date, rows=rows)

    if "account" in datasets:
        provider, rows = _fetch_with_fallback(clients, "fetch_margin_account_stats", trade_date)
        result["provider_results"]["account"] = provider
        result["rows_read"] += len(rows)
        result["account_rows"] = repo.replace_account_stats(rows=rows)

    result["rows_written"] = result["summary_rows"] + result["detail_rows"] + result["account_rows"]
    return result


def _provider_clients(context: PipelineContext, providers: list[str]) -> list[tuple[str, Any]]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    clients: list[tuple[str, Any]] = []
    for provider in providers:
        if provider == "exchange":
            clients.append((provider, context.hooks.exchange_margin.get_client()))
        elif provider == "akshare":
            clients.append((provider, context.hooks.akshare.get_client()))
    return clients


def _latest_trade_date(clients: list[tuple[str, Any]], end_date: date) -> date | None:
    for _, client in clients:
        fetch = getattr(client, "fetch_latest_margin_trade_date", None)
        if not callable(fetch):
            continue
        latest = fetch(end_date)
        if latest is not None:
            return latest
    return None


def _fetch_with_fallback(clients: list[tuple[str, Any]], method_name: str, trade_date: date) -> tuple[str | None, list[dict[str, Any]]]:
    last_errors: list[str] = []
    for provider, client in clients:
        fetch = getattr(client, method_name, None)
        if not callable(fetch):
            continue
        try:
            rows = fetch(trade_date)
        except Exception as exc:  # noqa: BLE001
            last_errors.append(f"{provider}: {type(exc).__name__}: {exc}")
            continue
        if rows:
            return provider, rows
    if last_errors:
        raise RuntimeError(f"all margin providers failed for {method_name}: " + " | ".join(last_errors))
    return None, []


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
