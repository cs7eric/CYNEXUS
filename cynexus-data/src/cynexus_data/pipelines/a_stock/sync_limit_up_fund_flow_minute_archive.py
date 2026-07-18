from __future__ import annotations

from datetime import date, datetime, timezone
import random
import time
from typing import Any

from cynexus_data.domain.stock.a_stock_persistence import normalize_dataset
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.eastmoney import a_stock as em
from cynexus_data.sources.eastmoney.http import EastMoneyHttpClient
from cynexus_data.storage.clickhouse.a_stock import AStockClickHouseRepository
from cynexus_data.storage.postgres.a_stock import AStockPostgresRepository
from cynexus_data.storage.postgres.market_limit import MarketLimitRepository


MINUTE_SOURCE = "eastmoney.push2"
DAILY_SOURCE = "eastmoney.push2.minute_agg"


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    params = dict(request.params or {})
    trade_date = _date(params.get("trade_date")) or date.today()
    fetch_live = _bool(params.get("fetch_live"), not request.dry_run)
    create_table = _bool(params.get("create_table"), False)
    ignore_errors = _bool(params.get("ignore_errors"), True)
    request_sleep_seconds = _float(params.get("request_sleep_seconds"))
    if request_sleep_seconds is None:
        request_sleep_seconds = 0.5
    fetch_retries = _int(params.get("fetch_retries"))
    if fetch_retries is None:
        fetch_retries = 1
    retry_sleep_min_seconds = _float(params.get("retry_sleep_min_seconds"))
    if retry_sleep_min_seconds is None:
        retry_sleep_min_seconds = 2.0
    retry_sleep_max_seconds = _float(params.get("retry_sleep_max_seconds"))
    if retry_sleep_max_seconds is None:
        retry_sleep_max_seconds = 6.0
    retry_sleep_max_seconds = max(retry_sleep_min_seconds, retry_sleep_max_seconds)
    code_pool_limit = _int(params.get("code_pool_limit") or params.get("pool_limit"))
    checkpoint_key = str(params.get("checkpoint_key") or "a_stock.sync_limit_up_fund_flow_minute_archive.round_robin")
    checkpoint = context.checkpoints.get(checkpoint_key) or {}
    explicit_offset = _int(params.get("code_offset"))
    checkpoint_offset = _int(checkpoint.get("next_offset"))
    code_offset = max(0, explicit_offset if explicit_offset is not None else (checkpoint_offset or 0))
    shard_size = _int(params.get("shard_size") or params.get("limit_codes"))
    if shard_size is None:
        shard_size = 80
    shard_size = max(1, shard_size)
    batch_flush_successes = _int(params.get("batch_flush_successes") or params.get("flush_successes"))
    if batch_flush_successes is None:
        batch_flush_successes = 10
    batch_flush_successes = max(1, batch_flush_successes)
    max_runtime_seconds = _float(params.get("max_runtime_seconds"))
    if max_runtime_seconds is None:
        max_runtime_seconds = 20 * 60.0
    provider_timeout_seconds = _float(params.get("provider_timeout_seconds") or params.get("timeout_seconds"))
    if provider_timeout_seconds is None:
        provider_timeout_seconds = 8.0
    complete_after_time = str(params.get("complete_after_time") or "14:50:00")
    aggregate_source = str(params.get("aggregate_source") or DAILY_SOURCE)

    pg_client = context.hooks.postgres.get_client()
    ch_client = context.hooks.clickhouse.get_client()
    explicit_codes = _explicit_codes(params)
    code_pool = explicit_codes or _resolve_code_pool(pg_client, params, limit=code_pool_limit)
    code_count = len(code_pool)
    selected_codes, resolved_offset = _select_shard(code_pool, limit=shard_size, offset=code_offset)
    code_offset = resolved_offset
    next_offset = _advance_offset(code_count, code_offset, len(selected_codes))

    ch_repo = AStockClickHouseRepository(ch_client)
    pg_repo = AStockPostgresRepository(pg_client)
    if create_table and not request.dry_run:
        ch_repo.create_tables()
        pg_repo.create_tables()

    ch_batches: dict[str, list[dict[str, Any]]] = {}
    rows_read = 0
    rows_written_ch = 0
    errors: list[str] = []
    samples: list[Any] = []
    retry_count = 0
    attempted_codes = 0
    success_codes = 0
    empty_codes = 0
    stop_reason: str | None = None
    pending_successes = 0
    start_monotonic = time.monotonic()

    client = EastMoneyHttpClient(timeout=provider_timeout_seconds, min_interval=0.0, jitter=(0.0, 0.0))

    def flush_minute_batches() -> int:
        nonlocal ch_batches, pending_successes, rows_written_ch
        if not ch_batches:
            return 0
        written = 0
        if not request.dry_run:
            written = ch_repo.insert_batches(ch_batches)
            rows_written_ch += written
        ch_batches = {}
        pending_successes = 0
        return written

    if fetch_live:
        for index, code in enumerate(selected_codes, start=1):
            if max_runtime_seconds and time.monotonic() - start_monotonic >= max_runtime_seconds:
                stop_reason = "max_runtime_seconds"
                break
            attempts = 0
            attempted_codes += 1
            try:
                payload = None
                while attempts <= fetch_retries:
                    attempts += 1
                    try:
                        payload = em.fund_flow_minute(client, code)
                        if not payload:
                            raise RuntimeError("EastMoney minute fund-flow returned no rows")
                        break
                    except Exception:  # noqa: PERF203, BLE001 - retry transient provider disconnects.
                        if attempts > fetch_retries:
                            raise
                        retry_count += 1
                        time.sleep(random.uniform(retry_sleep_min_seconds, retry_sleep_max_seconds))
                if payload is None:
                    payload = []
                batch = normalize_dataset("eastmoney_fund_flow_minute", payload, {"code": code, "snapshot_date": trade_date})
                _merge(ch_batches, batch.clickhouse)
                rows_read += batch.rows_read
                success_codes += 1
                pending_successes += 1
                if len(samples) < 3:
                    samples.append({"code": code, "items": payload[:2], "count": len(payload), "attempts": attempts})
            except Exception as exc:  # noqa: BLE001 - batch jobs should keep partial coverage when requested.
                if "returned no rows" in str(exc):
                    empty_codes += 1
                errors.append(f"{code}:attempts={attempts}:{type(exc).__name__}: {exc}")
                if not ignore_errors:
                    raise
            if pending_successes >= batch_flush_successes:
                flush_minute_batches()
                _mark_running_progress(
                    context,
                    params,
                    request,
                    processed=attempted_codes,
                    total=len(selected_codes),
                    rows_read=rows_read,
                    rows_written=rows_written_ch,
                    code_offset=code_offset,
                    next_offset=_advance_offset(code_count, code_offset, attempted_codes),
                    errors=errors,
                )
            if request_sleep_seconds is not None and request_sleep_seconds > 0:
                time.sleep(request_sleep_seconds)
            if index >= (_int(params.get("batch_limit")) or 100_000):
                stop_reason = "batch_limit"
                break

    flush_minute_batches()
    if not fetch_live:
        attempted_codes = len(selected_codes)

    aggregate_rows = ch_repo.latest_fund_flow_minute_daily_rows(
        trade_date=trade_date.isoformat(),
        codes=selected_codes,
        source=MINUTE_SOURCE,
        min_trade_time=complete_after_time,
    )
    daily_rows = daily_rows_from_minute_aggregates(aggregate_rows, source=aggregate_source)
    rows_written_pg = 0
    if daily_rows and not request.dry_run:
        rows_written_pg = pg_repo.upsert_many("a_stock_fund_flow_daily", daily_rows)

    processed_codes = attempted_codes if fetch_live else len(selected_codes)
    next_offset = _advance_offset(code_count, code_offset, processed_codes)
    checkpoint_value = {
        "status": "partial" if stop_reason else "completed",
        "trade_date": trade_date.isoformat(),
        "code_count": code_count,
        "shard_size": shard_size,
        "last_offset": code_offset,
        "processed_codes": processed_codes,
        "next_offset": next_offset,
        "success_codes": success_codes,
        "empty_codes": empty_codes,
        "error_count": len(errors),
        "stop_reason": stop_reason,
        "updated_at": datetime.now(timezone.utc).isoformat(),
    }
    context.checkpoints.put(checkpoint_key, request.job_key, checkpoint_value)
    _mark_running_progress(
        context,
        params,
        request,
        processed=processed_codes,
        total=len(selected_codes),
        rows_read=rows_read + len(aggregate_rows),
        rows_written=rows_written_ch + rows_written_pg,
        code_offset=code_offset,
        next_offset=next_offset,
        errors=errors,
        force=True,
    )

    status = "partial" if errors and (rows_written_ch or rows_written_pg or daily_rows) else "success"
    if errors and not (rows_written_ch or rows_written_pg or daily_rows):
        status = "failed"
    if stop_reason and status == "success":
        status = "partial"

    return {
        "pipeline": "a_stock.sync_limit_up_fund_flow_minute_archive",
        "dry_run": request.dry_run,
        "trade_date": trade_date.isoformat(),
        "stock_pool": "market_limit_daily_stocks.limit_up",
        "pool_codes": code_count,
        "planned_codes": len(selected_codes),
        "attempted_codes": attempted_codes,
        "success_codes": success_codes,
        "empty_codes": empty_codes,
        "code_offset": code_offset,
        "next_offset": next_offset,
        "checkpoint_key": checkpoint_key,
        "checkpoint": checkpoint_value,
        "fetch_live": fetch_live,
        "minute_rows_read": rows_read,
        "rows_read": rows_read + len(aggregate_rows),
        "rows_written": rows_written_ch + rows_written_pg,
        "clickhouse_rows_written": rows_written_ch,
        "postgres_rows_written": rows_written_pg,
        "aggregate_rows": len(aggregate_rows),
        "daily_rows": len(daily_rows),
        "daily_source": aggregate_source,
        "complete_after_time": complete_after_time,
        "shard_size": shard_size,
        "batch_flush_successes": batch_flush_successes,
        "max_runtime_seconds": max_runtime_seconds,
        "stop_reason": stop_reason,
        "provider_timeout_seconds": provider_timeout_seconds,
        "fetch_retries": fetch_retries,
        "retry_sleep_min_seconds": retry_sleep_min_seconds,
        "retry_sleep_max_seconds": retry_sleep_max_seconds,
        "retry_count": retry_count,
        "samples": samples,
        "errors": errors[:20],
        "error_count": len(errors),
        "success": status != "failed",
        "status": status,
    }


def daily_rows_from_minute_aggregates(rows: list[dict[str, Any]], *, source: str = DAILY_SOURCE) -> list[dict[str, Any]]:
    daily_rows: list[dict[str, Any]] = []
    for row in rows:
        code = str(row.get("code") or "").strip()[-6:].zfill(6)
        trade_date = _date(row.get("trade_date"))
        if not code or trade_date is None:
            continue
        daily_rows.append(
            {
                "source": source,
                "code": code,
                "trade_date": trade_date,
                "main_net": row.get("main_net"),
                "small_net": row.get("small_net"),
                "mid_net": row.get("mid_net"),
                "large_net": row.get("large_net"),
                "super_net": row.get("super_net"),
                "raw_payload": {
                    "source": MINUTE_SOURCE,
                    "aggregation": "latest_minute_of_day",
                    "latest_trade_time": str(row.get("latest_trade_time")) if row.get("latest_trade_time") is not None else None,
                    "minute_rows": row.get("minute_rows"),
                },
            }
        )
    return daily_rows


def _explicit_codes(params: dict[str, Any]) -> list[str]:
    explicit = _list_param(params.get("codes") or params.get("symbols") or params.get("code") or params.get("symbol"))
    return _normalize_codes(explicit)


def _resolve_code_pool(pg_client, params: dict[str, Any], *, limit: int | None) -> list[str]:
    return _normalize_codes(MarketLimitRepository(pg_client).list_historical_limit_up_codes(limit=limit, offset=0))


def _normalize_codes(values: list[str]) -> list[str]:
    seen: set[str] = set()
    codes: list[str] = []
    for value in values:
        code = str(value).strip()[-6:].zfill(6)
        if not code or code == "000000" or code in seen:
            continue
        seen.add(code)
        codes.append(code)
    return codes


def _select_shard(codes: list[str], *, limit: int, offset: int) -> tuple[list[str], int]:
    if not codes:
        return [], 0
    limit = min(max(1, limit), len(codes))
    resolved_offset = max(0, offset) % len(codes)
    end = resolved_offset + limit
    if end <= len(codes):
        return codes[resolved_offset:end], resolved_offset
    return codes[resolved_offset:] + codes[: end % len(codes)], resolved_offset


def _advance_offset(total: int, offset: int, processed: int) -> int:
    if total <= 0:
        return 0
    return (max(0, offset) + max(0, processed)) % total


def _mark_running_progress(
    context: PipelineContext,
    params: dict[str, Any],
    request: JobRequest,
    *,
    processed: int,
    total: int,
    rows_read: int,
    rows_written: int,
    code_offset: int,
    next_offset: int,
    errors: list[str],
    force: bool = False,
) -> None:
    run_id = params.get("progress_run_id")
    if not run_id:
        return
    every = _int(params.get("progress_update_every_items")) or 10
    if not force and processed > 0 and processed % every != 0:
        return
    metadata = dict(params.get("progress_base_metadata") or {})
    metadata.update(
        {
            "lastStep": request.job_key,
            "lastMessage": f"running {request.job_key}: stocks {processed}/{total}, next_offset={next_offset}, errors={len(errors)}",
            "currentStepProgress": {
                "step": request.job_key,
                "unitLabel": "stocks",
                "processedItems": processed,
                "totalItems": total,
                "rowsRead": rows_read,
                "rowsWritten": rows_written,
                "codeOffset": code_offset,
                "nextOffset": next_offset,
                "errorCount": len(errors),
            },
        }
    )
    context.job_store.mark_progress(
        str(run_id),
        {
            "status": "running",
            "success": False,
            "rows_read": rows_read,
            "rows_written": rows_written,
            "metadata": metadata,
        },
    )


def _merge(target: dict[str, list[dict[str, Any]]], source: dict[str, list[dict[str, Any]]]) -> None:
    for table, rows in source.items():
        target.setdefault(table, []).extend(rows)


def _list_param(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, (list, tuple, set)):
        return [str(item).strip() for item in value if str(item).strip()]
    text = str(value).strip()
    if not text:
        return []
    return [item.strip() for item in text.split(",") if item.strip()]


def _int(value: Any) -> int | None:
    if value in (None, ""):
        return None
    return int(value)


def _float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    return float(value)


def _bool(value: Any, default: bool) -> bool:
    if value in (None, ""):
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "y", "on"}


def _date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    text = str(value).strip()[:10]
    if len(text) == 8 and text.isdigit():
        text = f"{text[:4]}-{text[4:6]}-{text[6:8]}"
    return date.fromisoformat(text)
