from __future__ import annotations

import json
from datetime import date
from typing import Any

from cynexus_data.domain.market.market_overview import normalize_market_overview_fields
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.market_overview import fetch_eltdx_market_overview
from cynexus_data.storage.clickhouse.readers import read_market_overview_fact_row
from cynexus_data.storage.postgres.market_overview import MarketOverviewRepository
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    source = str(request.params.get("source") or "clickhouse_daily_qfq")
    actual_source = source
    create_table = _truthy(request.params.get("create_table"))
    raw_fields = _collect_fields(request.params)
    should_fetch_live = _truthy(request.params.get("fetch_live")) or (source in {"akshare", "eltdx", "eastmoney", "tencent"} and not _has_overview_field_values(raw_fields))
    fetch_errors: list[str] = []
    if not request.dry_run and should_fetch_live:
        actual_source, live_fields, fetch_errors = _fetch_live_fields_with_fallback(context, source=source, params=request.params)
        raw_fields.update(live_fields)
    trade_date = _parse_date(request.params.get("trade_date") or raw_fields.get("trade_date") or raw_fields.get("tradingDate"))
    if not request.dry_run and actual_source in {"clickhouse_daily_qfq", "clickhouse.daily_qfq"} and not _has_overview_field_values(raw_fields):
        if trade_date is None:
            trade_date = date.today()
        raw_fields.update(_fetch_clickhouse_fields(context, trade_date=trade_date, params=request.params))
    if should_fetch_live and trade_date is None:
        trade_date = date.today()
    fields = normalize_market_overview_fields(raw_fields, trade_date=trade_date)
    result = {
        "pipeline": "market_overview.build_market_overview",
        "dry_run": request.dry_run,
        "trade_date": fields["trade_date"].isoformat(),
        "source": actual_source,
        "requested_source": source,
        "rows_read": 1,
        "rows_written": 0,
        "fetch_live": should_fetch_live,
        "fetch_errors": fetch_errors,
        "fields": {key: value for key, value in fields.items() if key != "trade_date"},
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped Postgres access"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = MarketOverviewRepository(context.hooks.postgres.get_client())
    if create_table:
        repo.create_table()
    row = repo.upsert_fields(fields, source=actual_source, extra={"job": request.job_key, "requested_source": source, "fetch_errors": fetch_errors})
    result["rows_written"] = 1
    result["snapshot"] = _jsonable(row)
    return result


def _fetch_live_fields_with_fallback(context: PipelineContext, *, source: str, params: dict[str, Any]) -> tuple[str, dict[str, Any], list[str]]:
    errors: list[str] = []
    sources = [source]
    for fallback_source in _fallback_sources(source, params):
        if fallback_source not in sources:
            sources.append(fallback_source)
    for candidate in sources:
        try:
            if candidate in {"clickhouse_daily_qfq", "clickhouse.daily_qfq"}:
                trade_date = _parse_date(params.get("trade_date") or params.get("target_date") or params.get("date")) or date.today()
                return candidate, _fetch_clickhouse_fields(context, trade_date=trade_date, params=params), errors
            return candidate, _fetch_live_fields(context, source=candidate, params=params), errors
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{candidate}: {type(exc).__name__}: {exc}")
    raise RuntimeError("all market overview sources failed: " + " | ".join(errors))


def _fallback_sources(source: str, params: dict[str, Any]) -> list[str]:
    raw = params.get("fallback_sources")
    if raw in (None, ""):
        if source == "akshare":
            return ["tencent", "eastmoney", "eltdx", "clickhouse_daily_qfq"]
        return []
    if isinstance(raw, str):
        return [item.strip() for item in raw.split(",") if item.strip()]
    return [str(item).strip() for item in raw if str(item).strip()]


def _fetch_live_fields(context: PipelineContext, *, source: str, params: dict[str, Any]) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    if source == "akshare":
        return context.hooks.akshare.get_client().fetch_market_overview()
    if source == "eltdx":
        timeout = float(params.get("eltdx_timeout") or params.get("timeout_seconds") or 20)
        client = context.hooks.tdx.get_market_overview_client(timeout=timeout)
        if hasattr(client, "__enter__"):
            with client as active_client:
                return fetch_eltdx_market_overview(active_client)
        return fetch_eltdx_market_overview(client)
    if source == "eastmoney":
        page_size = int(params.get("page_size") or params.get("eastmoney_page_size") or 80)
        max_pages = int(params.get("max_pages") or params.get("eastmoney_max_pages") or 70)
        page_sleep = float(params.get("page_sleep") or params.get("eastmoney_page_sleep") or 0.0)
        return context.hooks.eastmoney.get_client().fetch_market_breadth(
            page_size=page_size,
            max_pages=max_pages,
            page_sleep=page_sleep,
        )
    if source == "tencent":
        codes = _normalize_codes(params.get("codes") or params.get("symbols"))
        if not codes:
            codes = StockBasicRepository(context.hooks.postgres.get_client()).list_codes(active_only=True)
        if not codes:
            raise ValueError("codes/symbols are required for tencent market overview or sec_stock_basic must contain active codes")
        batch_size = int(params.get("batch_size") or params.get("tencent_batch_size") or 500)
        batch_sleep = float(params.get("batch_sleep") or params.get("tencent_batch_sleep") or 0.22)
        return context.hooks.tencent.get_client().fetch_market_overview(codes, batch_size=batch_size, batch_sleep=batch_sleep)
    raise ValueError(f"unsupported live market overview source: {source}")


def _fetch_clickhouse_fields(context: PipelineContext, *, trade_date: date, params: dict[str, Any]) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    table = str(params.get("source_table") or "daily_qfq")
    row = read_market_overview_fact_row(context.hooks.clickhouse.get_client(), trade_date, table=table)
    if not row:
        raise ValueError(f"no ClickHouse market overview facts for {trade_date} from {table}")
    return row


def _collect_fields(params: dict[str, Any]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    raw = params.get("fields")
    if isinstance(raw, str) and raw.strip():
        parsed = json.loads(raw)
        if not isinstance(parsed, dict):
            raise ValueError("fields must be a JSON object")
        out.update(parsed)
    elif isinstance(raw, dict):
        out.update(raw)
    for key, value in params.items():
        if key not in {"fields", "source", "create_table", "force"}:
            out[key] = value
    return out


def _has_overview_field_values(fields: dict[str, Any]) -> bool:
    overview_keys = {
        "totalAmount",
        "totalVolume",
        "risingCount",
        "fallingCount",
        "flatCount",
        "limitUpCount",
        "limitDownCount",
        "stockCount",
        "mainNetInflow",
        "superLargeNetInflow",
        "largeNetInflow",
        "mediumNetInflow",
        "smallNetInflow",
        "mainNetInflowRatio",
        "superLargeNetInflowRatio",
        "largeNetInflowRatio",
        "mediumNetInflowRatio",
        "smallNetInflowRatio",
        "total_amount",
        "total_volume",
        "rising_count",
        "falling_count",
        "flat_count",
        "limit_up_count",
        "limit_down_count",
        "stock_count",
        "main_net_inflow",
        "super_large_net_inflow",
        "large_net_inflow",
        "medium_net_inflow",
        "small_net_inflow",
        "main_net_inflow_ratio",
        "super_large_net_ratio",
        "large_net_ratio",
        "medium_net_ratio",
        "small_net_ratio",
    }
    return any(key in overview_keys and value not in (None, "") for key, value in fields.items())


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    text = str(value)
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text)


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _normalize_codes(value: Any) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _jsonable(row: dict[str, Any]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for key, value in row.items():
        out[key] = value.isoformat() if hasattr(value, "isoformat") else value
    return out
