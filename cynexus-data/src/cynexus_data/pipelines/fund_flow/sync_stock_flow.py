from __future__ import annotations

import json
from datetime import date, datetime
from pathlib import Path
from typing import Any

from cynexus_data.core.market_symbol import normalize_market_symbol
from cynexus_data.domain.market.fund_flow import normalize_stock_fund_flow_snapshot
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.fund_flow import StockFundFlowRepository
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository
from cynexus_data.sources.tdx.easy_tdx import fetch_current_stock_fund_flow, fetch_current_stock_fund_flow_batch
from cynexus_data.sources.eastmoney import a_stock as eastmoney_a_stock
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient


_EASTMONEY_CLIENT = EastMoneyHttpClient()


def run(context: PipelineContext, request: JobRequest) -> dict:
    provider = str(request.params.get("provider") or request.params.get("providers") or "tencent").split(",")[0].strip().lower()
    trade_date = _parse_date(request.params.get("trade_date")) or date.today()
    history_path = request.params.get("history_path")
    create_table = _truthy(request.params.get("create_table"))
    batch_size = int(request.params.get("batch_size") or 120)
    codes = _resolve_codes(context, request.params)
    source = "tencent.qt" if provider == "tencent" else "easy_tdx.tdx" if provider == "easy_tdx" else provider
    source_type = "json_import" if history_path else "crawler"
    result = {
        "pipeline": "fund_flow.sync_stock_flow",
        "dry_run": request.dry_run,
        "provider": provider,
        "trade_date": trade_date.isoformat(),
        "history_path": str(history_path) if history_path else None,
        "codes": len(codes),
        "batch_size": batch_size,
        "rows_read": 0,
        "rows_written": 0,
        "snapshots_written": 0,
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped Tencent and Postgres access"]
        if history_path:
            result["planned_payloads"] = len(_discover_history_files(Path(str(history_path)), trade_date=trade_date))
        return result
    if provider not in {"tencent", "easy_tdx"}:
        raise ValueError(f"unsupported stock fund-flow provider: {provider}")
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    pg = context.hooks.postgres.get_client()
    repo = StockFundFlowRepository(pg)
    if create_table:
        repo.create_table()

    payloads = (
        _load_history_payloads(Path(str(history_path)), trade_date=trade_date)
        if history_path
        else [
            _fetch_live_payload(
                context,
                provider=provider,
                trade_date=trade_date,
                codes=codes or _load_active_codes(pg),
                batch_size=batch_size,
            )
        ]
    )
    for payload_trade_date, payload in payloads:
        result["trade_date"] = payload_trade_date.isoformat()
        rows = _payload_rows(payload)
        requested_codes = _parse_codes(payload.get("requestedCodes") or payload.get("requested_codes")) or codes
        captured_at = _parse_datetime(payload.get("fetchedAt") or payload.get("fetched_at"))
        rows_by_source: dict[str, list[dict[str, Any]]] = {}
        for row in rows:
            rows_by_source.setdefault(str(row.get("source") or source), []).append(row)
        written = 0
        for row_source, source_rows in rows_by_source.items():
            records = normalize_stock_fund_flow_snapshot(
                source_rows,
                trade_date=payload_trade_date,
                source=row_source,
                source_type=source_type,
                captured_at=captured_at,
            )
            written += repo.replace_snapshot(
                trade_date=payload_trade_date,
                records=records,
                source=row_source,
                source_type=source_type,
                requested_codes=requested_codes,
                fetched_at=captured_at,
                extra={"historyPath": str(history_path) if history_path else None, "provider": provider, "source": row_source},
            )
        result["rows_read"] += len(rows)
        result["rows_written"] += written
        result["snapshots_written"] += 1
        if payload.get("failures"):
            result.setdefault("errors", []).extend(payload["failures"])
    if result.get("errors"):
        result["status"] = "partial"
        result["success"] = False
    return result


def _fetch_live_payload(
    context: PipelineContext,
    *,
    provider: str,
    trade_date: date,
    codes: list[str],
    batch_size: int,
) -> tuple[date, dict[str, Any]]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    if not codes:
        raise ValueError("codes are required for live stock fund-flow sync when sec_stock_basic is empty")
    if provider == "easy_tdx":
        rows, failures = _fetch_easy_tdx_rows_with_fallback(codes, trade_date=trade_date, batch_size=batch_size)
        requested = [normalize_market_symbol(code) for code in codes]
        source_dates = [row.get("trade_date") for row in rows if row.get("trade_date") is not None]
        resolved_trade_date = max(source_dates, default=trade_date)
        return resolved_trade_date, {
            "rows": rows,
            "requestedCodes": requested,
            "fetchedAt": datetime.now().isoformat(timespec="seconds"),
            "rowCount": len(rows),
            "failures": failures,
            "source": "easy_tdx.tdx",
        }

    client = context.hooks.tencent.get_client()
    rows: list[dict[str, Any]] = []
    requested: list[str] = []
    fetched_at = datetime.now().isoformat(timespec="seconds")
    for chunk in _chunks(codes, batch_size):
        normalized_chunk = [normalize_market_symbol(code) for code in chunk]
        payload = client.fetch_stock_fund_flow_batch(normalized_chunk)
        for symbol, row in payload.items():
            row = dict(row)
            row.setdefault("symbol", symbol)
            rows.append(row)
        requested.extend(normalized_chunk)
    source_dates = [_parse_date(row.get("trade_date")) for row in rows]
    resolved_trade_date = max((value for value in source_dates if value is not None), default=trade_date)
    return resolved_trade_date, {"rows": rows, "requestedCodes": requested, "fetchedAt": fetched_at, "rowCount": len(rows)}


def _fetch_easy_tdx_rows_with_fallback(
    codes: list[str],
    *,
    trade_date: date,
    batch_size: int,
) -> tuple[list[dict[str, Any]], list[str]]:
    rows: list[dict[str, Any]] = []
    failures: list[str] = []
    for chunk in _chunks(codes, batch_size):
        try:
            batch_rows = fetch_current_stock_fund_flow_batch(chunk, trade_date=trade_date)
            rows.extend(batch_rows)
            returned = {str(row.get("symbol") or row.get("code") or "")[-6:] for row in batch_rows}
            missing = [code for code in chunk if normalize_market_symbol(code)[-6:] not in returned]
            if not missing:
                continue
            chunk = missing
        except Exception:
            pass
        for code in chunk:
            try:
                single = fetch_current_stock_fund_flow(code, trade_date=trade_date)
                if single:
                    rows.extend(single)
                    continue
            except Exception as easy_exc:  # noqa: BLE001 - EastMoney is the per-symbol fallback.
                easy_error = f"easy_tdx: {type(easy_exc).__name__}: {easy_exc}"
            else:
                easy_error = "easy_tdx: empty_response"
            try:
                payload = eastmoney_a_stock.fund_flow_120d(_EASTMONEY_CLIENT, code, mode="daily")
                items = payload.get("items") if isinstance(payload, dict) else []
                if items:
                    item = dict(items[-1])
                    item["symbol"] = code
                    item["code"] = code
                    item["source"] = str(payload.get("source") or "eastmoney.push2delay")
                    item["active_net_amount_wan"] = _number(item.get("main_net"), default=0.0) / 10000.0
                    rows.append(item)
                    continue
            except Exception as eastmoney_exc:  # noqa: BLE001 - preserve failure for the job result.
                failures.append(f"{code}: {easy_error}; eastmoney: {type(eastmoney_exc).__name__}: {eastmoney_exc}")
                continue
            failures.append(f"{code}: {easy_error}; eastmoney: empty_response")
    return rows, failures


def _number(value: Any, *, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _load_active_codes(pg_client) -> list[str]:
    return StockBasicRepository(pg_client).list_codes(active_only=True)


def _resolve_codes(context: PipelineContext, params: dict[str, Any]) -> list[str]:
    codes = _parse_codes(params.get("codes") or params.get("symbols"))
    if codes:
        return codes
    code_source = str(params.get("code_source") or "").strip().lower()
    if not code_source:
        return []
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required to resolve code_source")
    pg = context.hooks.postgres.get_client()
    if code_source in {"sec_stock_basic", "stock_basic", "all", "active"}:
        return _load_active_codes(pg)
    if code_source in {"self_selected", "watchlist"}:
        from sqlalchemy import text

        with pg.session() as db:
            rows = db.execute(
                text(
                    """
                    SELECT DISTINCT symbol
                      FROM cynexus_appl_market.self_selected_list_items
                     WHERE deleted_at IS NULL
                       AND status = 'active'
                       AND target_type = 'stock'
                       AND symbol IS NOT NULL
                       AND symbol <> ''
                     ORDER BY symbol
                    """
                )
            ).all()
        return [str(row[0]).strip() for row in rows if str(row[0]).strip()]
    raise ValueError(f"unsupported code_source: {code_source}")


def _discover_history_files(path: Path, *, trade_date: date) -> list[Path]:
    if path.is_file():
        return [path]
    if not path.exists():
        return []
    exact = path / f"{trade_date.isoformat()}.json"
    if exact.exists():
        return [exact]
    return sorted(path.glob("*.json"))


def _load_history_payloads(path: Path, *, trade_date: date) -> list[tuple[date, dict[str, Any]]]:
    payloads: list[tuple[date, dict[str, Any]]] = []
    for file_path in _discover_history_files(path, trade_date=trade_date):
        try:
            payload_trade_date = date.fromisoformat(file_path.stem)
        except ValueError:
            payload_trade_date = trade_date
        with file_path.open("r", encoding="utf-8") as fp:
            payload = json.load(fp)
        if isinstance(payload, list):
            payload = {"rows": payload}
        if isinstance(payload, dict):
            payloads.append((payload_trade_date, payload))
    return payloads


def _payload_rows(payload: dict[str, Any]) -> list[dict[str, Any]]:
    rows = payload.get("rows")
    if isinstance(rows, list):
        return [row for row in rows if isinstance(row, dict)]
    data = payload.get("data")
    if isinstance(data, dict):
        out: list[dict[str, Any]] = []
        for symbol, row in data.items():
            if isinstance(row, dict):
                item = dict(row)
                item.setdefault("symbol", symbol)
                out.append(item)
        return out
    keyed_rows = []
    for symbol, row in payload.items():
        if isinstance(row, dict):
            item = dict(row)
            item.setdefault("symbol", symbol)
            keyed_rows.append(item)
    return keyed_rows


def _parse_codes(value: Any) -> list[str]:
    if value in (None, ""):
        return []
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    return [str(value).strip()]


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


def _chunks(values: list[str], size: int) -> list[list[str]]:
    chunk_size = max(1, size)
    return [values[index : index + chunk_size] for index in range(0, len(values), chunk_size)]


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
