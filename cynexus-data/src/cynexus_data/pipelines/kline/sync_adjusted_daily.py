from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.adjusted import create_eltdx_client, fetch_adjusted_daily_frame
from cynexus_data.storage.clickhouse.writers import ClickHouseAdjustedDailyKlineSink
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    adjusts = _normalize_adjusts(request.params.get("adjust") or request.params.get("adjusts") or ["qfq", "hfq"])
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    start_date = request.params.get("start_date") or request.params.get("trade_date")
    end_date = request.params.get("end_date") or request.params.get("trade_date")
    limit_codes = request.params.get("limit_codes")
    batch_size = int(request.params.get("batch_size") or 100_000)
    create_table = _truthy(request.params.get("create_table"))
    page_size = int(request.params.get("page_size") or 800)
    max_pages = int(request.params.get("max_pages") or 200)
    resume = _truthy(request.params.get("resume"))
    checkpoint_prefix = str(request.params.get("checkpoint_prefix") or "kline.sync_adjusted_daily")
    if codes and limit_codes is not None:
        codes = codes[: int(limit_codes)]
    result = {
        "pipeline": "kline.sync_adjusted_daily",
        "dry_run": request.dry_run,
        "provider": "eltdx",
        "adjusts": adjusts,
        "planned_codes": len(codes or []),
        "start_date": start_date,
        "end_date": end_date,
        "rows_read": 0,
        "rows_written": 0,
        "code_results": [],
        "resume": resume,
        "skipped_checkpoints": 0,
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped eltdx and ClickHouse access"]
        return result
    if not codes:
        if context.hooks is None:
            raise RuntimeError("PipelineContext.hooks is required")
        codes = StockBasicRepository(context.hooks.postgres.get_client()).list_codes(active_only=True)
        if limit_codes is not None:
            codes = codes[: int(limit_codes)]
        result["planned_codes"] = len(codes)
        result["code_source"] = "postgres.sec_stock_basic"
    if not codes:
        raise ValueError("codes/symbols are required or sec_stock_basic must contain active codes")
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    client = create_eltdx_client()
    total_read = 0
    total_written = 0
    sinks = {adjust: ClickHouseAdjustedDailyKlineSink(context.hooks.clickhouse.get_config(), adjust=adjust) for adjust in adjusts}
    if create_table:
        for sink in sinks.values():
            sink.create_table()
    for adjust in adjusts:
        sink = sinks[adjust]
        for code in codes:
            checkpoint_key = _checkpoint_key(checkpoint_prefix, adjust, code, start_date, end_date)
            if resume and _is_completed_checkpoint(context, checkpoint_key):
                result["skipped_checkpoints"] += 1
                result["code_results"].append({"code": code, "adjust": adjust, "rows_read": 0, "rows_written": 0, "skipped": True, "skip_reason": "checkpoint_completed"})
                continue
            frame = fetch_adjusted_daily_frame(
                client,
                code,
                adjust,
                start_date=start_date,
                end_date=end_date,
                page_size=page_size,
                max_pages=max_pages,
            )
            rows = len(frame)
            written = sink.replace_daily(frame, batch_size=batch_size)
            total_read += rows
            total_written += written
            if resume:
                context.checkpoints.put(
                    checkpoint_key,
                    request.job_key,
                    {
                        "status": "completed",
                        "code": code,
                        "adjust": adjust,
                        "rows_read": rows,
                        "rows_written": written,
                        "start_date": start_date,
                        "end_date": end_date,
                    },
                )
            result["code_results"].append({"code": code, "adjust": adjust, "rows_read": rows, "rows_written": written})
    result["rows_read"] = total_read
    result["rows_written"] = total_written
    return {**result, "code_results": result["code_results"][:50], "code_results_truncated": len(result["code_results"]) > 50}


def _normalize_codes(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _normalize_adjusts(value) -> list[str]:
    if isinstance(value, str):
        items = [item.strip().lower() for item in value.split(",") if item.strip()]
    else:
        items = [str(item).strip().lower() for item in value if str(item).strip()]
    if "both" in items:
        items = ["qfq", "hfq"]
    for item in items:
        if item not in {"qfq", "hfq"}:
            raise ValueError("adjust must be qfq, hfq, or both")
    return sorted(set(items))


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _checkpoint_key(prefix: str, adjust: str, code: str, start_date, end_date) -> str:
    return f"{prefix}:{adjust}:{code}:{start_date or ''}:{end_date or ''}"


def _is_completed_checkpoint(context: PipelineContext, key: str) -> bool:
    checkpoint = context.checkpoints.get(key)
    return bool(checkpoint and checkpoint.get("status") == "completed")
