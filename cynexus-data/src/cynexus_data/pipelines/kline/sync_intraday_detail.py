from __future__ import annotations

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.intraday import (
    auction_points_to_frame,
    fetch_stock_auction,
    fetch_stock_history_timeshare,
    timeshare_to_frame,
)
from cynexus_data.storage.clickhouse.writers import ClickHouseIntradayDetailSink
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


def run(context: PipelineContext, request: JobRequest) -> dict:
    datasets = _normalize_datasets(request.params.get("datasets") or request.params.get("dataset") or ["auction", "timeshare"])
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    trade_date = request.params.get("trade_date")
    target_type = request.params.get("target_type")
    timeout = float(request.params.get("timeout_seconds") or request.params.get("eltdx_timeout") or 20.0)
    batch_size = int(request.params.get("batch_size") or 50_000)
    create_table = _truthy(request.params.get("create_table"))
    resume = _truthy(request.params.get("resume"))
    limit_codes = request.params.get("limit_codes") or request.params.get("limit")
    checkpoint_prefix = str(request.params.get("checkpoint_prefix") or "kline.sync_intraday_detail")
    if codes and limit_codes is not None:
        codes = codes[: int(limit_codes)]

    result = {
        "pipeline": "kline.sync_intraday_detail",
        "provider": "eltdx",
        "dry_run": request.dry_run,
        "datasets": datasets,
        "planned_codes": len(codes or []),
        "trade_date": trade_date,
        "target_type": target_type,
        "rows_read": 0,
        "rows_written": 0,
        "code_results": [],
        "resume": resume,
        "skipped_checkpoints": 0,
    }
    if request.dry_run:
        result["notes"] = ["dry_run skipped eltdx and ClickHouse access"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    if not codes:
        codes = StockBasicRepository(context.hooks.postgres.get_client()).list_codes(active_only=True)
        if limit_codes is not None:
            codes = codes[: int(limit_codes)]
        result["planned_codes"] = len(codes)
        result["code_source"] = "postgres.sec_stock_basic"
    if not codes:
        raise ValueError("codes/symbols are required or sec_stock_basic must contain active codes")

    client = context.hooks.tdx.get_intraday_detail_client(timeout=timeout)
    sink = ClickHouseIntradayDetailSink(context.hooks.clickhouse.get_config())
    if create_table:
        sink.create_tables()

    rows_read = 0
    rows_written = 0
    for code in codes:
        code_result = {"code": code, "auction_rows": 0, "timeshare_rows": 0, "rows_written": 0}
        for dataset in datasets:
            checkpoint_key = _checkpoint_key(checkpoint_prefix, dataset, code, trade_date)
            if resume and _is_completed_checkpoint(context, checkpoint_key):
                result["skipped_checkpoints"] += 1
                continue
            if dataset == "auction":
                auction_payload = fetch_stock_auction(client, code, trade_date, target_type=target_type)
                frame = auction_points_to_frame(auction_payload["points"])
                written = sink.insert_auction_points(frame, batch_size=batch_size)
                code_result["auction_rows"] = len(frame)
            else:
                rows = fetch_stock_history_timeshare(client, code, trade_date, target_type=target_type)
                frame = timeshare_to_frame(rows)
                written = sink.insert_timeshare(frame, batch_size=batch_size)
                code_result["timeshare_rows"] = len(frame)
            rows_read += len(frame)
            rows_written += written
            code_result["rows_written"] += written
            if resume:
                context.checkpoints.put(
                    checkpoint_key,
                    request.job_key,
                    {
                        "status": "completed",
                        "dataset": dataset,
                        "code": code,
                        "trade_date": trade_date,
                        "rows_read": len(frame),
                        "rows_written": written,
                    },
                )
        result["code_results"].append(code_result)
    result["rows_read"] = rows_read
    result["rows_written"] = rows_written
    return {**result, "code_results": result["code_results"][:50], "code_results_truncated": len(result["code_results"]) > 50}


def _normalize_datasets(value) -> list[str]:
    if isinstance(value, str):
        items = [item.strip().lower() for item in value.split(",") if item.strip()]
    else:
        items = [str(item).strip().lower() for item in value if str(item).strip()]
    if "all" in items:
        items = ["auction", "timeshare"]
    allowed = {"auction", "timeshare"}
    for item in items:
        if item not in allowed:
            raise ValueError("datasets must contain auction, timeshare, or all")
    return sorted(set(items))


def _normalize_codes(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def _checkpoint_key(prefix: str, dataset: str, code: str, trade_date) -> str:
    return f"{prefix}:{dataset}:{code}:{trade_date or ''}"


def _is_completed_checkpoint(context: PipelineContext, key: str) -> bool:
    checkpoint = context.checkpoints.get(key)
    return bool(checkpoint and checkpoint.get("status") == "completed")
