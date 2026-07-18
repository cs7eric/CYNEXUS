from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import time
from typing import Any

from cynexus_data.jobs.contracts import JobRequest, JobResult
from cynexus_data.pipelines.a_stock import sync_a_stock_dataset
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.runtime.progress import mark_parent_step_progress
from cynexus_data.storage.postgres.market_limit import MarketLimitRepository
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository


JOB_KEY = "a_stock_fund_flow_history_backfill"
DATASET = "eastmoney_fund_flow_120d"
DEFAULT_SHARD_SIZE = 300
DEFAULT_HISTORY_COUNT = 5
DEFAULT_WORKERS = 6


def run(context: PipelineContext, request: JobRequest, run_id: str) -> JobResult:
    started_at = datetime.now(timezone.utc)
    params = dict(request.params or {})
    shard_size = _int(params.get("shard_size"), DEFAULT_SHARD_SIZE, minimum=0)
    history_count = _int(params.get("fund_flow_count"), DEFAULT_HISTORY_COUNT)
    workers = _bounded_int(params.get("fund_flow_workers") or params.get("workers"), DEFAULT_WORKERS, minimum=1, maximum=8)
    batch_sleep_seconds = _float(params.get("batch_sleep_seconds") or params.get("fund_flow_batch_sleep_seconds"), 0.0, minimum=0.0)
    scope = str(params.get("stock_scope") or params.get("scope") or params.get("checkpoint_scope") or "all-active")
    run_all = _bool(params.get("run_all") if "run_all" in params else params.get("complete_scope"), default=False) or shard_size == 0
    checkpoint_key = f"stock-fund-flow-history:{scope}:{history_count}"
    codes = _resolve_codes(context, params)
    state = context.checkpoints.get(checkpoint_key) or {}
    completed = set() if run_all else {str(code) for code in state.get("completed_codes") or []}
    failed_before = {str(code): str(message) for code, message in (state.get("failed_codes") or {}).items()}
    # A failed symbol remains pending, while successful symbols are never fetched again.
    pending = [code for code in codes if code not in completed]
    shard = pending if run_all else pending[:shard_size]
    total = len(codes)
    rows_read = 0
    rows_written = 0
    failures: dict[str, str] = {}
    processed = 0

    metadata: dict[str, Any] = {
        "pipeline": "stock_fund_flow.history_chain",
        "type": "chain",
        "planned_steps": 1,
        "checkpoint_key": checkpoint_key,
        "shard_size": shard_size,
        "run_all": run_all,
        "stock_scope": scope,
        "workers": workers,
        "batch_sleep_seconds": batch_sleep_seconds,
        "history_count": history_count,
        "total_codes": total,
        "completed_codes": len(completed),
        "pending_codes": len(pending),
        "currentStepProgress": _progress(0, len(shard), len(completed), len(failed_before), len(pending), total, 0, 0),
        "lastMessage": f"waiting: completed={len(completed)}, pending={len(pending)}",
    }
    context.job_store.mark_progress(run_id, {"status": "running", "success": False, "metadata": metadata})

    batches = _batches(shard, shard_size if run_all and shard_size > 0 else len(shard) or 1)
    for batch_index, batch in enumerate(batches, start=1):
        max_workers = min(workers, len(batch)) if batch else 1
        with ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="fund-flow-history") as executor:
            future_to_code = {
                executor.submit(_process_code, context, request, params, code, history_count): code
                for code in batch
            }
            for future in as_completed(future_to_code):
                code = future_to_code[future]
                try:
                    payload = future.result()
                    rows_read += int(payload.get("rows_read") or 0)
                    rows_written += int(payload.get("rows_written") or 0)
                    completed.add(code)
                    failed_before.pop(code, None)
                except Exception as exc:  # noqa: BLE001 - one stock must not abort the shard.
                    failures[code] = f"{type(exc).__name__}: {exc}"
                    failed_before[code] = failures[code]
                processed += 1
                _save_checkpoint(context, checkpoint_key, completed, failed_before, total=total, last_code=code)
                metadata.update(
                    {
                        "completed_codes": len(completed),
                        "pending_codes": max(0, total - len(completed)),
                        "failed_codes": len(failed_before),
                        "currentStepProgress": _progress(
                            processed,
                            len(shard),
                            len(completed),
                            len(failed_before),
                            max(0, total - len(completed)),
                            total,
                            rows_read,
                            rows_written,
                        ),
                        "lastMessage": f"backfill {processed}/{len(shard)}: {code}",
                        "rows_read": rows_read,
                        "rows_written": rows_written,
                    }
                )
                mark_parent_step_progress(
                    context,
                    {
                        "progress_run_id": run_id,
                        "progress_base_metadata": metadata,
                        "progress_step_code": "history_backfill",
                        "progress_update_every_items": 1,
                        "progress_extra": {
                            "completedCodes": len(completed),
                            "failedCodes": len(failed_before),
                            "pendingCodes": max(0, total - len(completed)),
                            "totalCodes": total,
                        },
                    },
                    processed=processed,
                    total=len(shard),
                    rows_read=rows_read,
                    rows_written=rows_written,
                    unit_label="股票",
                    message=metadata["lastMessage"],
                    force=True,
                )
        if batch_sleep_seconds > 0 and batch_index < len(batches):
            metadata.update({"lastMessage": f"batch {batch_index}/{len(batches)} done, sleeping {batch_sleep_seconds:g}s"})
            context.job_store.mark_progress(run_id, {"status": "running", "success": False, "metadata": metadata})
            time.sleep(batch_sleep_seconds)

    success = not failures
    status = "success" if success else "partial"
    metadata.update({"lastMessage": f"completed scope {scope}: {len(completed)}/{total}, failed={len(failed_before)}", "status": status})
    return JobResult(
        run_id=run_id,
        job_key=request.job_key,
        success=success,
        status=status,
        started_at=started_at,
        finished_at=datetime.now(timezone.utc),
        rows_read=rows_read,
        rows_written=rows_written,
        errors=[f"{code}: {message}" for code, message in failures.items()],
        metadata=metadata,
    )


def _process_code(context: PipelineContext, request: JobRequest, params: dict[str, Any], code: str, history_count: int) -> dict[str, Any]:
    payload = sync_a_stock_dataset.run(
        context,
        JobRequest(
            job_key="sync_a_stock_dataset",
            dry_run=request.dry_run,
            params={
                **params,
                "code": code,
                "codes": [code],
                "datasets": [DATASET],
                "dataset": DATASET,
                "fund_flow_provider": str(params.get("fund_flow_provider") or "easy_tdx"),
                "fund_flow_count": history_count,
                "fetch_live": not request.dry_run,
                "ignore_errors": False,
                "limit_codes": 1,
                "batch_limit": 1,
            },
        ),
    )
    result = payload.get("dataset_results", {}).get(DATASET, {})
    if int(result.get("history_complete_count") or 0) < 1 and not request.dry_run:
        raise RuntimeError(f"history incomplete: sources={result.get('sources')}")
    return payload


def _resolve_codes(context: PipelineContext, params: dict[str, Any]) -> list[str]:
    explicit = params.get("codes") or params.get("symbols") or params.get("code")
    if explicit:
        values = explicit if isinstance(explicit, list) else str(explicit).split(",")
        return _dedupe_codes([str(value).strip() for value in values if str(value).strip()])
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    scope = str(params.get("stock_scope") or params.get("scope") or params.get("checkpoint_scope") or "all-active")
    if scope in {"missing-120", "missing_120", "fund-flow-missing-120", "history-missing-120"}:
        minimum_rows = _int(params.get("minimum_history_rows") or params.get("history_threshold"), 120)
        return _missing_fund_flow_history_codes(context, minimum_rows=minimum_rows)
    active = _dedupe_codes(StockBasicRepository(pg).list_codes(active_only=True))
    if scope in {"limit-up-history", "historical-limit-up", "limit_up_history"}:
        return _dedupe_codes(MarketLimitRepository(pg).list_historical_limit_up_codes())
    if scope in {"remaining-active", "non-limit-up-active", "active-minus-limit-up"}:
        limit_codes = set(_dedupe_codes(MarketLimitRepository(pg).list_historical_limit_up_codes()))
        return [code for code in active if code not in limit_codes]
    return active


def _missing_fund_flow_history_codes(context: PipelineContext, *, minimum_rows: int = 120) -> list[str]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    from sqlalchemy import text

    pg = context.hooks.postgres.get_client()
    with pg.session() as db:
        rows = db.execute(
            text(
                """
                WITH active AS (
                    SELECT code
                      FROM cynexus_appl_market.sec_stock_basic
                     WHERE is_active IS TRUE
                       AND code IS NOT NULL
                       AND code <> ''
                ), coverage AS (
                    SELECT code, count(DISTINCT trade_date) AS history_rows
                      FROM cynexus_appl_market.a_stock_fund_flow_daily
                     WHERE source IN ('easy_tdx.tdx', 'eastmoney.push2his', 'eastmoney.push2delay')
                     GROUP BY code
                )
                SELECT active.code
                  FROM active
                  LEFT JOIN coverage ON coverage.code = active.code
                 WHERE COALESCE(coverage.history_rows, 0) < :minimum_rows
                 ORDER BY COALESCE(coverage.history_rows, 0), active.code
                """
            ),
            {"minimum_rows": minimum_rows},
        ).all()
    return _dedupe_codes([str(row[0]).strip() for row in rows if str(row[0]).strip()])


def _save_checkpoint(context: PipelineContext, key: str, completed: set[str], failed: dict[str, str], *, total: int, last_code: str) -> None:
    context.checkpoints.put(key, JOB_KEY, {"completed_codes": sorted(completed), "failed_codes": failed, "total_codes": total, "last_code": last_code})


def _progress(
    processed: int,
    shard_total: int,
    completed: int,
    failed: int,
    pending: int,
    total: int,
    rows_read: int,
    rows_written: int,
) -> dict[str, Any]:
    return {
        "step": "history_backfill",
        "unitLabel": "股票",
        "processedItems": processed,
        "totalItems": shard_total,
        "completedCodes": completed,
        "failedCodes": failed,
        "pendingCodes": pending,
        "totalCodes": total,
        "rowsRead": rows_read,
        "rowsWritten": rows_written,
    }


def _dedupe_codes(values: list[str]) -> list[str]:
    seen: set[str] = set()
    output: list[str] = []
    for value in values:
        code = str(value).strip()[-6:].zfill(6)
        if not code or code in seen:
            continue
        seen.add(code)
        output.append(code)
    return output


def _batches(values: list[str], size: int) -> list[list[str]]:
    chunk_size = max(1, size)
    return [values[index : index + chunk_size] for index in range(0, len(values), chunk_size)]


def _bool(value: Any, *, default: bool = False) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _int(value: Any, default: int, *, minimum: int = 1) -> int:
    try:
        return max(minimum, int(value))
    except (TypeError, ValueError):
        return default


def _float(value: Any, default: float, *, minimum: float = 0.0) -> float:
    try:
        return max(minimum, float(value))
    except (TypeError, ValueError):
        return default


def _bounded_int(value: Any, default: int, *, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = default
    return max(minimum, min(maximum, parsed))


def main() -> None:
    from cynexus_data.jobs._pipeline_job import main_for_job

    main_for_job(JOB_KEY, "Backfill stock fund-flow history in checkpointed 300-stock shards.")
