from __future__ import annotations

from datetime import date, timedelta
from pathlib import Path
from typing import Any

import pandas as pd

from cynexus_data.core.market_symbol import normalize_market_symbol
from cynexus_data.domain.stock.kline_records import minute_provider_rows_to_frame
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.provider_fallback import fetch_minute_kline_with_fallback, provider_plan
from cynexus_data.sources.tdx.local_minute import (
    discover_1m_files,
    iter_tdx_1m_frames,
    latest_tdx_1m_date,
)
from cynexus_data.storage.clickhouse.readers import get_latest_effective_kline_trade_date
from cynexus_data.storage.clickhouse.writers import ClickHouseKlineSink


_INCREMENTAL_FLUSH_ROWS = 500_000


def run(context: PipelineContext, request: JobRequest) -> dict:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    root = Path(str(request.params.get("tdx_root") or context.hooks.tdx.get_root_path()))
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    code_set = {_normalize_code_key(code) for code in codes or []}
    providers = provider_plan("kline_minute", requested=request.params.get("providers"))
    incremental = (
        _truthy(request.params.get("incremental"))
        if "incremental" in request.params
        else "tdx" in providers
    )
    limit_files = request.params.get("limit_files")
    batch_size = int(request.params.get("batch_size") or 50_000)
    create_table = _truthy(request.params.get("create_table"))
    replace_existing = _truthy(request.params.get("replace_existing"))
    period = int(request.params.get("period") or 1)
    target_type = str(request.params.get("target_type") or "auto").lower()
    trade_date = _parse_date(request.params.get("trade_date") or request.params.get("date") or request.params.get("snapshot_date"))
    start_date = _parse_date(request.params.get("start_date")) or trade_date
    end_date = _parse_date(request.params.get("end_date")) or trade_date
    files = discover_1m_files(root) if "tdx" in providers and str(root) else []
    if code_set:
        files = [file for file in files if _file_key(file.market, file.symbol) in code_set]
    if limit_files is not None:
        files = files[: int(limit_files)]
    sink: ClickHouseKlineSink | None = None
    effective_trade_date: date | None = None
    if incremental and "tdx" in providers and start_date is None and end_date is None:
        latest_source_date = latest_tdx_1m_date(files)
        if not request.dry_run:
            sink = ClickHouseKlineSink(context.hooks.clickhouse.get_config())
            effective_trade_date = get_latest_effective_kline_trade_date(
                sink.client,
                expected_symbols=len({file.tdx_code for file in files}),
            )
        start_date = (
            effective_trade_date + timedelta(days=1)
            if effective_trade_date
            else latest_source_date
        )
        end_date = latest_source_date
    result = {
        "pipeline": "kline.sync_minute",
        "dry_run": request.dry_run,
        "providers": providers,
        "tdx_root": str(root),
        "target_type": target_type,
        "trade_date": trade_date.isoformat() if trade_date else None,
        "start_date": start_date.isoformat() if start_date else None,
        "end_date": end_date.isoformat() if end_date else None,
        "planned_codes": len(codes or []),
        "discovered_files": len(files),
        "rows_read": 0,
        "rows_written": 0,
        "file_results": [],
        "provider_results": [],
        "replace_existing": replace_existing,
        "incremental": incremental,
        "effective_trade_date": effective_trade_date.isoformat() if effective_trade_date else None,
    }
    if request.dry_run:
        if codes and any(provider != "tdx" for provider in providers):
            result["network_provider_plan"] = [provider for provider in providers if provider != "tdx"]
        return result

    # Do not turn an empty incremental range into an ALTER ... DELETE mutation.
    # A MergeTree mutation is planned against every active part even when its
    # predicate cannot match a row.  On this table that has previously caused
    # a no-op close job to advance every historical part to a new mutation
    # version.  Apart from the needless I/O, that makes an interrupted host
    # substantially more likely to leave a part unrecoverable.
    if (
        incremental
        and "tdx" in providers
        and start_date is not None
        and end_date is not None
        and start_date > end_date
    ):
        return {
            **result,
            "status": "up_to_date",
            "reason": "effective_trade_date_is_not_behind_latest_source_date",
        }

    if sink is None:
        sink = ClickHouseKlineSink(context.hooks.clickhouse.get_config())
    if create_table:
        sink.create_table()
    local_replace_existing = replace_existing
    if (
        incremental
        and replace_existing
        and "tdx" in providers
        and start_date is not None
        and end_date is not None
        and files
    ):
        sink.delete_1m_range(
            start_date,
            end_date,
            source="tdx_local",
            period=period,
            tdx_codes=[file.tdx_code for file in files],
        )
        local_replace_existing = False
    total_rows = 0
    total_written = 0
    local_frames: list[tuple[dict[str, object], pd.DataFrame]] = []
    local_frame_rows = 0
    for file, frame in iter_tdx_1m_frames(files, start_date=start_date, end_date=end_date):
        frame = _filter_frame_dates(frame, start_date=start_date, end_date=end_date)
        rows = len(frame)
        total_rows += rows
        file_result = {
            "path": str(file.path),
            "market": file.market,
            "symbol": file.symbol,
            "rows_read": rows,
            "rows_written": 0,
        }
        result["file_results"].append(file_result)
        if incremental:
            if not frame.empty:
                local_frames.append((file_result, frame))
                local_frame_rows += len(frame)
            if local_frame_rows >= _INCREMENTAL_FLUSH_ROWS:
                total_written += _flush_incremental_frames(
                    sink,
                    local_frames,
                    batch_size=batch_size,
                    replace_existing=local_replace_existing,
                )
                local_frame_rows = 0
        else:
            written = sink.insert_1m(frame, batch_size=batch_size, replace_existing=local_replace_existing)
            total_written += written
            file_result["rows_written"] = written
    if incremental and local_frames:
        total_written += _flush_incremental_frames(
            sink,
            local_frames,
            batch_size=batch_size,
            replace_existing=replace_existing,
        )
    if codes and any(provider != "tdx" for provider in providers) and ("tdx" not in providers or not files):
        for code in codes:
            fetched = fetch_minute_kline_with_fallback(
                context.hooks,
                symbol=code,
                providers=providers,
                period=period,
                limit=int(request.params.get("limit") or 500),
                target_type=_resolve_target_type(code, target_type),
            )
            frame = minute_provider_rows_to_frame(code, fetched.items, source=fetched.source, period=period)
            frame = _filter_frame_dates(frame, start_date=start_date, end_date=end_date)
            written = sink.insert_1m(frame, batch_size=batch_size, replace_existing=replace_existing)
            total_rows += len(frame)
            total_written += written
            result["provider_results"].append(
                {
                    "symbol": code,
                    "target_type": _resolve_target_type(code, target_type),
                    "rows_read": len(fetched.items),
                    "rows_written": written,
                    **fetched.meta(),
                }
            )
    result["rows_read"] = total_rows
    result["rows_written"] = total_written
    return {
        **result,
        "file_results": result["file_results"][:20],
        "file_results_truncated": len(result["file_results"]) > 20,
        "provider_results": result["provider_results"][:20],
        "provider_results_truncated": len(result["provider_results"]) > 20,
    }


def _normalize_codes(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _normalize_code_key(value: str) -> str:
    return normalize_market_symbol(value)


def _file_key(market: str, symbol: str) -> str:
    return f"{str(market).lower()}{str(symbol)[-6:].zfill(6)}"


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    text = str(value).strip()
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text[:10])


def _filter_frame_dates(frame, *, start_date: date | None, end_date: date | None):
    if frame.empty or (start_date is None and end_date is None):
        return frame
    out = frame
    if start_date is not None:
        out = out[out["trade_date"] >= start_date]
    if end_date is not None:
        out = out[out["trade_date"] <= end_date]
    return out


def _resolve_target_type(symbol: str, target_type: str) -> str:
    if target_type and target_type != "auto":
        return target_type
    normalized = normalize_market_symbol(symbol)
    code = normalized[2:]
    if code.startswith(("000", "399", "880", "881", "882", "883", "884", "885", "886", "887", "899")):
        return "index"
    if code.startswith(("5", "1")):
        return "etf"
    return "stock"


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _flush_incremental_frames(
    sink: ClickHouseKlineSink,
    frames: list[tuple[dict[str, object], pd.DataFrame]],
    *,
    batch_size: int,
    replace_existing: bool,
) -> int:
    if not frames:
        return 0
    combined = pd.concat([frame for _, frame in frames], ignore_index=True)
    written = sink.insert_1m(combined, batch_size=batch_size, replace_existing=replace_existing)
    for file_result, frame in frames:
        file_result["rows_written"] = len(frame)
    frames.clear()
    return written
