from __future__ import annotations

from datetime import date
from typing import Any

from cynexus_data.domain.futures.position import normalize_position_rows
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.futures_position import FuturesPositionRepository


SUPPORTED_PROVIDERS = {"akshare", "sina"}
SUPPORTED_EXCHANGES = {"CFFEX", "SHFE", "CZCE", "DCE", "GFEX", "INE"}


def run(context: PipelineContext, request: JobRequest, run_id: str | None = None) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    providers = _list(request.params.get("providers") or request.params.get("provider") or ["akshare", "sina"])
    exchanges = [item.upper() for item in _list(request.params.get("exchanges") or sorted(SUPPORTED_EXCHANGES))]
    varieties = [item.upper() for item in _list(request.params.get("varieties") or [])]
    unsupported = [item for item in providers if item not in SUPPORTED_PROVIDERS]
    if unsupported:
        raise ValueError(f"unsupported futures position providers: {unsupported}")
    unsupported_exchanges = [item for item in exchanges if item not in SUPPORTED_EXCHANGES]
    if unsupported_exchanges:
        raise ValueError(f"unsupported futures exchanges: {unsupported_exchanges}")
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    result: dict[str, Any] = {
        "pipeline": "futures.sync_position",
        "run_id": run_id,
        "trade_date": trade_date.isoformat(),
        "providers": providers,
        "exchanges": exchanges,
        "rows_read": 0,
        "rows_written": 0,
        "exchange_results": {},
    }
    if request.dry_run:
        return result
    repo = FuturesPositionRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repo.create_table()
    raw_by_provider = _fetch_provider_rows(context, providers, exchanges, trade_date, varieties)
    for exchange in exchanges:
        provider, raw_rows = _select_exchange_rows(raw_by_provider, providers, exchange)
        normalized = normalize_position_rows(
            raw_rows, trade_date=trade_date, exchange_code=exchange, selected_source=provider,
        )
        instrument_count = len({row.instrument_code for row in normalized})
        batch_id = repo.begin_batch(trade_date=trade_date, exchange_code=exchange, provider=provider,
                                    expected_instrument_count=instrument_count)
        result["rows_read"] += len(raw_rows)
        if not normalized:
            repo.finish_batch(batch_id, status="EMPTY", instrument_count=0, row_count=0,
                              warnings=["provider returned no publishable rank rows"])
            result["exchange_results"][exchange] = {"provider": provider, "status": "EMPTY", "batch_id": batch_id}
            continue
        written = repo.replace_selected_snapshot(batch_id=batch_id, rows=normalized)
        catalog_rows = [{"exchange_code": exchange, "variety_code": row.variety_code,
                         "contract_code": row.contract_code or row.instrument_code}
                        for row in normalized if row.contract_code]
        catalog_written = repo.upsert_contract_catalog(catalog_rows, selected_source=provider)
        result["rows_written"] += written
        result["exchange_results"][exchange] = {
            "provider": provider, "status": "SUCCEEDED", "batch_id": batch_id,
            "instrument_count": instrument_count, "rank_rows": written, "catalog_rows": catalog_written,
        }
    statuses = [item["status"] for item in result["exchange_results"].values()]
    result["success"] = any(status == "SUCCEEDED" for status in statuses)
    result["status"] = "SUCCEEDED" if statuses and all(status == "SUCCEEDED" for status in statuses) else "PARTIAL"
    if not result["success"]:
        result["error"] = "no exchange returned publishable futures position rows"
    return result


def _fetch_provider_rows(context: PipelineContext, providers: list[str], exchanges: list[str],
                         trade_date: date, varieties: list[str]) -> dict[str, dict[str, list[dict[str, Any]]]]:
    result: dict[str, dict[str, list[dict[str, Any]]]] = {}
    for provider in providers:
        if provider == "akshare":
            result[provider] = context.hooks.akshare.get_client().fetch_futures_position_rows(
                trade_date, exchanges=exchanges, varieties=varieties,
            )
        elif provider == "sina" and "DCE" in exchanges:
            try:
                result[provider] = {"DCE": context.hooks.sina_futures.get_client().fetch_dce_position_rows(
                    trade_date, varieties=varieties,
                )}
            except Exception:  # noqa: BLE001
                # A fallback source outage must leave DCE explicitly EMPTY
                # without hiding successful data from other exchanges.
                result[provider] = {"DCE": []}
    return result


def _select_exchange_rows(raw_by_provider: dict[str, dict[str, list[dict[str, Any]]]], providers: list[str],
                          exchange: str) -> tuple[str, list[dict[str, Any]]]:
    for provider in providers:
        raw = raw_by_provider.get(provider, {}).get(exchange, [])
        if provider == "akshare":
            source = f"akshare.futures_position.{exchange.lower()}"
        elif provider == "sina" and exchange == "DCE":
            source = "sina.futures_position.dce"
        else:
            continue
        if raw:
            return source, raw
    return f"{providers[0]}.futures_position.{exchange.lower()}", []


def _list(value: Any) -> list[str]:
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
        text = f"{text[:4]}-{text[4:6]}-{text[6:]}"
    return date.fromisoformat(text)


def _truthy(value: Any) -> bool:
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}
