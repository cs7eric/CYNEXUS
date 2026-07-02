from __future__ import annotations

from pathlib import Path

import pandas as pd

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.progress import mark_parent_step_progress
from cynexus_data.domain.stock.kline_records import daily_provider_rows_to_frame
from cynexus_data.sources.provider_fallback import fetch_daily_kline_with_fallback, provider_plan
from cynexus_data.sources.tdx.local_day import discover_day_files, iter_tdx_day_frames
from cynexus_data.storage.clickhouse.writers import ClickHouseDailyKlineSink


def run(context: PipelineContext, request: JobRequest) -> dict:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    params = request.params
    codes = _normalize_codes(params.get("codes") or params.get("symbols"))
    providers = provider_plan("kline_daily", requested=params.get("providers"))
    root = Path(str(params.get("day_root") or params.get("tdx_root") or context.hooks.tdx.get_root_path()))
    markets = _normalize_markets(params.get("markets") or params.get("market") or ["sh", "sz", "bj"])
    start_date = params.get("start_date") or params.get("trade_date")
    end_date = params.get("end_date") or params.get("trade_date")
    limit_files = params.get("limit_files")
    unit_scale = int(params.get("unit_scale") or 1)
    batch_size = int(params.get("batch_size") or 100_000)
    flush_rows = int(params.get("flush_rows") or 0)
    replace_scope = str(params.get("replace_scope") or "keys")
    create_table = _truthy(params.get("create_table"))
    target_table = str(params.get("target_table") or "daily_raw")
    replace_existing = params.get("replace_existing")
    replace_existing = True if replace_existing is None else _truthy(replace_existing)
    resume = _truthy(params.get("resume"))
    if resume and flush_rows > 0:
        raise ValueError("flush_rows cannot be used with resume because per-file checkpoints would be ambiguous")
    checkpoint_prefix = str(params.get("checkpoint_prefix") or "kline.sync_daily")

    files = []
    if "tdx" in providers:
        files = discover_day_files(root, markets=markets)
        if codes:
            code_set = set(codes)
            files = [file for file in files if file.symbol in code_set or file.tdx_code in code_set]
        if limit_files is not None:
            files = files[: int(limit_files)]
    result = {
        "pipeline": "kline.sync_daily",
        "dry_run": request.dry_run,
        "providers": providers,
        "day_root": str(root),
        "markets": markets,
        "planned_codes": len(codes or []),
        "discovered_files": len(files),
        "start_date": start_date,
        "end_date": end_date,
        "rows_read": 0,
        "rows_written": 0,
        "target_table": target_table,
        "replace_existing": replace_existing,
        "replace_scope": replace_scope,
        "flush_rows": flush_rows,
        "file_results": [],
        "batch_results": [],
        "provider_results": [],
        "resume": resume,
        "skipped_checkpoints": 0,
    }
    if request.dry_run:
        if codes and any(provider != "tdx" for provider in providers):
            result["network_provider_plan"] = [provider for provider in providers if provider != "tdx"]
        return result

    sink = ClickHouseDailyKlineSink(context.hooks.clickhouse.get_config(), table=target_table)
    if create_table:
        sink.create_table()
    total_rows = 0
    total_written = 0
    processed_files = 0
    pending_frames: list[pd.DataFrame] = []
    pending_rows = 0
    pending_files: list[str] = []
    _mark_parent_progress(
        context,
        params,
        discovered_files=len(files),
        processed_files=processed_files,
        rows_read=total_rows,
        rows_written=total_written,
        message="initial_backfill_refresh discovered files",
    )
    for file, frame in iter_tdx_day_frames(files, unit_scale=unit_scale, start_date=start_date, end_date=end_date):
        checkpoint_key = _file_checkpoint_key(checkpoint_prefix, file, start_date, end_date)
        if resume and _is_completed_checkpoint(context, checkpoint_key):
            processed_files += 1
            result["skipped_checkpoints"] += 1
            result["file_results"].append(
                {
                    "path": str(file.path),
                    "market": file.market,
                    "symbol": file.symbol,
                    "rows_read": 0,
                    "rows_written": 0,
                    "skipped": True,
                    "skip_reason": "checkpoint_completed",
                }
            )
            _mark_parent_progress(
                context,
                params,
                discovered_files=len(files),
                processed_files=processed_files,
                rows_read=total_rows,
                rows_written=total_written,
            )
            continue
        rows = len(frame)
        total_rows += rows
        if flush_rows > 0:
            if rows:
                pending_frames.append(frame)
                pending_rows += rows
                pending_files.append(str(file.path))
            written = 0
            if pending_rows >= flush_rows:
                written = _flush_daily_frames(
                    sink,
                    pending_frames,
                    batch_size=batch_size,
                    replace_existing=replace_existing,
                    replace_scope=replace_scope,
                )
                result["batch_results"].append(
                    {"files": len(pending_files), "rows_read": pending_rows, "rows_written": written}
                )
                total_written += written
                pending_frames = []
                pending_rows = 0
                pending_files = []
        else:
            written = sink.insert_daily(frame, batch_size=batch_size, replace_existing=replace_existing, replace_scope=replace_scope)
            total_written += written
            if resume:
                context.checkpoints.put(
                    checkpoint_key,
                    request.job_key,
                    {
                        "status": "completed",
                        "path": str(file.path),
                        "market": file.market,
                        "symbol": file.symbol,
                        "rows_read": rows,
                        "rows_written": written,
                        "start_date": start_date,
                        "end_date": end_date,
                    },
                )
        result["file_results"].append(
            {
                "path": str(file.path),
                "market": file.market,
                "symbol": file.symbol,
                "rows_read": rows,
                "rows_written": written,
                "batched": flush_rows > 0,
            }
            )
        processed_files += 1
        _mark_parent_progress(
            context,
            params,
            discovered_files=len(files),
            processed_files=processed_files,
            rows_read=total_rows,
            rows_written=total_written,
        )
    if pending_frames:
        written = _flush_daily_frames(
            sink,
            pending_frames,
            batch_size=batch_size,
            replace_existing=replace_existing,
            replace_scope=replace_scope,
        )
        result["batch_results"].append(
            {"files": len(pending_files), "rows_read": pending_rows, "rows_written": written}
        )
        total_written += written
        _mark_parent_progress(
            context,
            params,
            discovered_files=len(files),
            processed_files=processed_files,
            rows_read=total_rows,
            rows_written=total_written,
            message="initial_backfill_refresh flushed pending rows",
            force=True,
        )
    if codes and any(provider != "tdx" for provider in providers) and ("tdx" not in providers or not files):
        for code in codes:
            checkpoint_key = _provider_checkpoint_key(checkpoint_prefix, code, start_date, end_date, providers)
            if resume and _is_completed_checkpoint(context, checkpoint_key):
                result["skipped_checkpoints"] += 1
                result["provider_results"].append({"symbol": code, "rows_read": 0, "rows_written": 0, "skipped": True, "skip_reason": "checkpoint_completed"})
                continue
            fetched = fetch_daily_kline_with_fallback(
                context.hooks,
                symbol=code,
                providers=providers,
                adjust=str(params.get("adjust") or "qfq"),
                limit=int(params.get("limit") or 2000),
            )
            frame = daily_provider_rows_to_frame(code, fetched.items, source=fetched.source, unit_scale=unit_scale)
            written = sink.insert_daily(frame, batch_size=batch_size, replace_existing=replace_existing)
            total_rows += len(frame)
            total_written += written
            if resume:
                context.checkpoints.put(
                    checkpoint_key,
                    request.job_key,
                    {
                        "status": "completed",
                        "symbol": code,
                        "source": fetched.source,
                        "rows_read": len(fetched.items),
                        "rows_written": written,
                        "start_date": start_date,
                        "end_date": end_date,
                    },
                )
            result["provider_results"].append(
                {
                    "symbol": code,
                    "rows_read": len(fetched.items),
                    "rows_written": written,
                    **fetched.meta(),
                }
            )
    result["rows_read"] = total_rows
    result["rows_written"] = total_written
    _mark_parent_progress(
        context,
        params,
        discovered_files=len(files),
        processed_files=processed_files,
        rows_read=total_rows,
        rows_written=total_written,
        message="initial_backfill_refresh finished",
        force=True,
    )
    return {
        **result,
        "file_results": result["file_results"][:20],
        "file_results_truncated": len(result["file_results"]) > 20,
        "batch_results": result["batch_results"][:20],
        "batch_results_truncated": len(result["batch_results"]) > 20,
        "provider_results": result["provider_results"][:20],
        "provider_results_truncated": len(result["provider_results"]) > 20,
    }


def _flush_daily_frames(
    sink: ClickHouseDailyKlineSink,
    frames: list[pd.DataFrame],
    *,
    batch_size: int,
    replace_existing: bool,
    replace_scope: str,
) -> int:
    if not frames:
        return 0
    combined = pd.concat(frames, ignore_index=True)
    return sink.insert_daily(combined, batch_size=batch_size, replace_existing=replace_existing, replace_scope=replace_scope)


def _mark_parent_progress(
    context: PipelineContext,
    params: dict,
    *,
    discovered_files: int,
    processed_files: int,
    rows_read: int,
    rows_written: int,
    message: str | None = None,
    force: bool = False,
) -> None:
    mark_parent_step_progress(
        context,
        params,
        processed=processed_files,
        total=discovered_files,
        rows_read=rows_read,
        rows_written=rows_written,
        message=message,
        unit_label="文件",
        force=force,
    )


def _normalize_codes(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _normalize_markets(value) -> list[str]:
    if isinstance(value, str):
        items = [item.strip().lower() for item in value.split(",") if item.strip()]
    else:
        items = [str(item).strip().lower() for item in value if str(item).strip()]
    if "all" in items:
        return ["sh", "sz", "bj"]
    return items


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _is_completed_checkpoint(context: PipelineContext, key: str) -> bool:
    checkpoint = context.checkpoints.get(key)
    return bool(checkpoint and checkpoint.get("status") == "completed")


def _file_checkpoint_key(prefix: str, file, start_date, end_date) -> str:
    return f"{prefix}:file:{file.market}:{file.symbol}:{start_date or ''}:{end_date or ''}:{file.path}"


def _provider_checkpoint_key(prefix: str, code: str, start_date, end_date, providers: list[str]) -> str:
    return f"{prefix}:provider:{code}:{start_date or ''}:{end_date or ''}:{','.join(providers)}"
