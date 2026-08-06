from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import date
import threading
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.margin import MarginRepository
from cynexus_data.sources.eastmoney.a_stock import margin_trading, margin_trading_latest
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
        enrichment_provider = None
        if rows and _truthy(request.params.get("enrich_detail", True)):
            rows, enrichment_provider = _enrich_stock_detail(context, trade_date, rows)
        result["provider_results"]["detail"] = provider
        if enrichment_provider:
            result["provider_results"]["detail_enrichment"] = enrichment_provider
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
    workers = max(1, min(int(request.params.get("workers") or request.params.get("pool_size") or 8), 16))
    request_interval = max(0.0, min(float(request.params.get("request_interval") or 0.35), 5.0))
    repo = MarginRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repo.create_table()
    rows_written = 0
    rows_read = 0
    failed_codes: list[str] = []
    empty_codes: list[str] = []
    worker_local = threading.local()

    def fetch(code: str) -> tuple[str, list[dict[str, Any]], Exception | None]:
        try:
            client = getattr(worker_local, "client", None)
            if client is None:
                client = EastMoneyHttpClient(session=context.hooks.eastmoney.get_session())
                # Each worker owns its session and throttle state. This keeps the
                # request rate bounded without serializing the entire universe.
                client.min_interval = request_interval
                client.jitter = (0.05, 0.2) if request_interval else (0.0, 0.0)
                worker_local.client = client
            history = margin_trading(client, code, page_size=page_size)
            rows = [_eastmoney_stock_margin_row(code, item) for item in history if item.get("date")]
            return code, rows, None
        except Exception as exc:  # noqa: BLE001 - preserve per-stock progress for a bulk backfill.
            return code, [], exc

    with ThreadPoolExecutor(max_workers=min(workers, max(1, len(codes)))) as executor:
        futures = {executor.submit(fetch, code): code for code in codes}
        for future in as_completed(futures):
            code, rows, error = future.result()
            if error is not None:
                failed_codes.append(code)
                continue
            rows_read += len(rows)
            if not rows:
                empty_codes.append(code)
                continue
            rows_written += repo.upsert_stock_history(rows=rows) if not request.dry_run else 0
    return {
        "pipeline": "margin.sync_margin_stock_history",
        "dry_run": request.dry_run,
        "mode": "stock_history",
        "codes": codes,
        "codes_requested": len(codes),
        "rows_read": rows_read,
        "rows_written": rows_written,
        "failed_codes": failed_codes,
        "empty_codes": empty_codes,
        "codes_with_rows": len(codes) - len(failed_codes) - len(empty_codes),
        "workers": workers,
        "request_interval": request_interval,
        "provider": "eastmoney.datacenter",
    }


def _margin_history_codes(context: PipelineContext, params: dict[str, Any]) -> list[str]:
    explicit = _normalize_list(params.get("codes") or params.get("code"))
    if explicit:
        return explicit
    from sqlalchemy import text

    raw_limit = params.get("limit_codes")
    if raw_limit in (None, ""):
        raw_limit = params.get("batch_size")
    limit = None if raw_limit in (None, "", 0, "0") else max(1, int(raw_limit))
    history_days = max(1, min(int(params.get("history_days") or 120), 500))
    code_source = str(params.get("code_source") or "self_selected").strip().lower()
    if code_source in {"all", "active", "stock_basic", "sec_stock_basic"}:
        code_source = "stock_basic"
    elif code_source in {"self_selected", "watchlist"}:
        code_source = "self_selected"
    else:
        raise ValueError(f"unsupported margin history code_source: {code_source}")
    source_sql = """
            SELECT DISTINCT b.code
              FROM cynexus_appl_market.sec_stock_basic b
             WHERE b.is_active = TRUE
               AND b.code ~ '^[0-9]{6}$'
        """ if code_source == "stock_basic" else """
            SELECT DISTINCT right(regexp_replace(symbol, '[^0-9]', '', 'g'), 6) AS code
              FROM cynexus_appl_market.self_selected_list_items
             WHERE deleted_at IS NULL AND status = 'active' AND target_type = 'stock'
               AND regexp_replace(symbol, '[^0-9]', '', 'g') ~ '^[0-9]{1,6}$'
        """
    with context.hooks.postgres.get_client().session() as db:
        rows = db.execute(text("""
            WITH selected AS (
        """ + source_sql + """
            ), coverage AS (
                SELECT stock_code, count(*) AS row_count
                  FROM cynexus_appl_market.margin_stock_detail
                 WHERE deleted_at IS NULL AND source = 'eastmoney.datacenter.margin'
                 GROUP BY stock_code
            )
            SELECT selected.code
              FROM selected
              LEFT JOIN coverage ON coverage.stock_code = selected.code
             WHERE coalesce(coverage.row_count, 0) < :history_days
             ORDER BY coalesce(coverage.row_count, 0), selected.code
        """ + (" LIMIT :limit" if limit is not None else "")),
            {**{"history_days": history_days}, **({"limit": limit} if limit is not None else {})},
        ).all()
    return [str(row[0]).strip() for row in rows if row[0]]


def _eastmoney_stock_margin_row(code: str, item: dict[str, Any]) -> dict[str, Any]:
    return {
        "trade_date": date.fromisoformat(str(item["date"])[:10]),
        "exchange": "SSE" if str(code).startswith(("5", "6", "9")) else "SZSE",
        "stock_code": str(code)[-6:],
        "stock_name": item.get("stock_name"),
        "source": "eastmoney.datacenter.margin",
        "financing_balance": item.get("rzye"),
        "financing_buy_amount": item.get("rzmre"),
        "financing_repay_amount": item.get("rzche"),
        "securities_lending_balance": item.get("rqyl"),
        "securities_lending_balance_amount": item.get("rqye"),
        "securities_lending_sell_volume": item.get("rqmcl"),
        "securities_lending_repay_volume": item.get("rqchl"),
        "margin_balance": item.get("rzrqye"),
        "extra": {"provider": "eastmoney", "report": "RPTA_WEB_RZRQ_GGMX"},
    }


def _enrich_stock_detail(
    context: PipelineContext,
    trade_date: date,
    rows: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], str | None]:
    """Fill provider-specific nulls without replacing the official exchange row."""
    try:
        client = EastMoneyHttpClient(session=context.hooks.eastmoney.get_session())
        enrichment = {
            str(item.get("stock_code") or "")[-6:]: item
            for item in margin_trading_latest(client, trade_date)
            if item.get("stock_code")
        }
    except Exception:
        return rows, None
    if not enrichment:
        return rows, None

    fields = (
        "financing_balance",
        "financing_buy_amount",
        "financing_repay_amount",
        "securities_lending_balance",
        "securities_lending_balance_amount",
        "securities_lending_sell_volume",
        "securities_lending_repay_volume",
        "margin_balance",
    )
    key_map = {
        "financing_balance": "rzye",
        "financing_buy_amount": "rzmre",
        "financing_repay_amount": "rzche",
        "securities_lending_balance": "rqyl",
        "securities_lending_balance_amount": "rqye",
        "securities_lending_sell_volume": "rqmcl",
        "securities_lending_repay_volume": "rqchl",
        "margin_balance": "rzrqye",
    }
    enriched_rows = []
    enriched_count = 0
    for row in rows:
        item = enrichment.get(str(row.get("stock_code") or "")[-6:])
        if item is None:
            enriched_rows.append(row)
            continue
        updated = dict(row)
        changed = False
        for field in fields:
            value = item.get(key_map[field])
            if value is not None and updated.get(field) != value:
                updated[field] = value
                changed = True
        if changed:
            extra = dict(updated.get("extra") or {})
            extra["enriched_by"] = "eastmoney.datacenter.margin"
            updated["extra"] = extra
            enriched_count += 1
        enriched_rows.append(updated)
    return enriched_rows, f"eastmoney.datacenter.margin ({enriched_count}/{len(rows)})"


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
