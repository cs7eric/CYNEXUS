from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.margin import MarginRepository
from cynexus_data.sources.eastmoney.a_stock import margin_trading
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient


SUPPORTED_PROVIDERS = {"exchange", "akshare"}
SUPPORTED_DATASETS = {"summary", "detail", "account"}


def run(context: PipelineContext, request: JobRequest) -> dict:
    if str(request.params.get("mode") or "").strip().lower() == "stock_history":
        return _run_stock_history(context, request)
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


def _run_stock_history(context: PipelineContext, request: JobRequest) -> dict:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    codes = _margin_history_codes(context, request.params)
    page_size = max(1, min(int(request.params.get("page_size") or 120), 500))
    repo = MarginRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repo.create_table()
    rows_written = 0
    provider_results: dict[str, str] = {}
    client = EastMoneyHttpClient(session=context.hooks.eastmoney.get_session())
    for code in codes:
        try:
            history = margin_trading(client, code, page_size=page_size)
            rows = [_eastmoney_stock_margin_row(code, item) for item in history if item.get("date")]
            rows_written += repo.upsert_stock_history(rows=rows) if not request.dry_run else 0
            provider_results[code] = "eastmoney.datacenter"
        except Exception as exc:  # noqa: BLE001 - daily exchange/AkShare collection remains the bounded fallback.
            provider_results[code] = f"exchange,akshare fallback: {type(exc).__name__}: {exc}"
    return {
        "pipeline": "margin.sync_margin_stock_history",
        "dry_run": request.dry_run,
        "mode": "stock_history",
        "codes": codes,
        "rows_written": rows_written,
        "provider_results": provider_results,
    }


def _margin_history_codes(context: PipelineContext, params: dict[str, Any]) -> list[str]:
    explicit = _normalize_list(params.get("codes") or params.get("code"))
    if explicit:
        return explicit
    from sqlalchemy import text

    limit = max(1, int(params.get("limit_codes") or 30))
    with context.hooks.postgres.get_client().session() as db:
        rows = db.execute(text("""
            WITH selected AS (
                SELECT DISTINCT symbol AS code
                  FROM cynexus_appl_market.self_selected_list_items
                 WHERE deleted_at IS NULL AND status = 'active' AND target_type = 'stock'
            ), coverage AS (
                SELECT stock_code, count(*) AS row_count
                  FROM cynexus_appl_market.margin_stock_detail
                 WHERE deleted_at IS NULL AND source = 'eastmoney.datacenter.margin'
                 GROUP BY stock_code
            )
            SELECT selected.code
              FROM selected
              LEFT JOIN coverage ON coverage.stock_code = selected.code
             WHERE coalesce(coverage.row_count, 0) < 100
             ORDER BY coalesce(coverage.row_count, 0), selected.code
             LIMIT :limit
        """), {"limit": limit}).all()
    return [str(row[0]).strip() for row in rows if row[0]]


def _eastmoney_stock_margin_row(code: str, item: dict[str, Any]) -> dict[str, Any]:
    return {
        "trade_date": date.fromisoformat(str(item["date"])[:10]),
        "exchange": "SSE" if str(code).startswith(("5", "6", "9")) else "SZSE",
        "stock_code": str(code)[-6:],
        "stock_name": None,
        "source": "eastmoney.datacenter.margin",
        "financing_balance": item.get("rzye"),
        "financing_buy_amount": item.get("rzmre"),
        "financing_repay_amount": item.get("rzche"),
        "securities_lending_balance": item.get("rqye"),
        "securities_lending_balance_amount": None,
        "securities_lending_sell_volume": item.get("rqmcl"),
        "securities_lending_repay_volume": item.get("rqchl"),
        "margin_balance": item.get("rzrqye"),
        "extra": {"provider": "eastmoney", "report": "RPTA_WEB_RZRQ_GGMX"},
    }


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
