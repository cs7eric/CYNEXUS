from __future__ import annotations

import time
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError, as_completed
from datetime import date
from typing import Any

from cynexus_data.core.calendar import is_trading_day
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.stock_topics import create_eltdx_client, fetch_stock_topics, stock_topics_to_sector_mappings
from cynexus_data.storage.postgres.stock_basic import StockBasicRepository
from cynexus_data.storage.postgres.stock_sector import StockSectorRepository


DEFAULT_TDX_TOPIC_HOSTS = [
    "116.205.183.150:7709",
    "124.71.187.122:7709",
    "122.192.35.4:7709",
    "119.147.212.81:7709",
    "60.191.117.167:7709",
    "115.236.62.66:7709",
    "123.125.108.90:7709",
    "218.108.50.108:7709",
    "114.80.63.12:7709",
    "180.153.18.170:7709",
    "123.125.108.14:7709",
    "60.12.136.250:7709",
    "218.6.170.54:7709",
    "123.103.93.79:7709",
]


def run(context: PipelineContext, request: JobRequest) -> dict:
    codes = _normalize_codes(request.params.get("codes") or request.params.get("symbols"))
    limit_codes = request.params.get("limit_codes")
    create_table = _truthy(request.params.get("create_table"))
    trading_day_only = _truthy(request.params.get("trading_day_only"))
    target_date = _parse_date(
        request.params.get("target_date")
        or request.params.get("trade_date")
        or request.params.get("snapshot_date")
        or request.params.get("date")
    )
    pool_size = int(request.params.get("pool_size") or 8)
    timeout = float(request.params.get("timeout") or 8)
    workers = int(request.params.get("workers") or 64)
    shard_size = int(request.params.get("shard_size") or request.params.get("batch_size") or 800)
    sleep_first = float(request.params.get("sleep_first") or request.params.get("sleep_between_batches") or 0)
    sleep_retry = float(request.params.get("sleep_retry") or 5)
    max_retry_rounds = int(request.params.get("max_retry_rounds") or 5)
    batch_timeout_seconds = float(request.params.get("batch_timeout_seconds") or 0)
    replace_empty = _truthy(request.params.get("replace_empty"))
    hosts = _normalize_hosts(request.params.get("hosts"))
    if codes and limit_codes is not None:
        codes = codes[: int(limit_codes)]
    result = {
        "pipeline": "stock_basic.sync_stock_sectors",
        "dry_run": request.dry_run,
        "provider": "eltdx",
        "target_date": target_date.isoformat() if target_date else None,
        "trading_day_only": trading_day_only,
        "planned_codes": len(codes or []),
        "rows_read": 0,
        "rows_written": 0,
        "succeeded_codes": 0,
        "failed_codes": 0,
        "empty_codes": 0,
        "batch_count": 0,
        "retry_rounds": 0,
        "replace_empty": replace_empty,
        "hosts_enabled": bool(hosts),
        "code_results": [],
    }
    if trading_day_only and target_date is not None and not is_trading_day(target_date):
        result["skipped"] = True
        result["skip_reason"] = "non_trading_day"
        return result
    if request.dry_run:
        result["notes"] = ["dry_run skipped eltdx and Postgres access"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    if not codes:
        codes = StockBasicRepository(pg).list_codes(active_only=True)
        if limit_codes is not None:
            codes = codes[: int(limit_codes)]
        result["planned_codes"] = len(codes)
        result["code_source"] = "postgres.sec_stock_basic"
    if not codes:
        raise ValueError("codes/symbols are required or sec_stock_basic must contain active codes")

    repo = StockSectorRepository(pg)
    if create_table:
        repo.create_table()

    codes = list(dict.fromkeys(code.zfill(6) for code in codes))
    initial_batches = _chunk(codes, shard_size)
    pending: list[str] = []
    total_topics = 0
    total_written = 0
    empty_codes: set[str] = set()
    result["planned_codes"] = len(codes)
    result["batch_count"] = len(initial_batches)

    for batch_index, batch in enumerate(initial_batches):
        batch_results = _fetch_batch(
            batch,
            batch_index=batch_index,
            pool_size=pool_size,
            timeout=timeout,
            workers=workers,
            hosts=hosts,
            batch_timeout_seconds=batch_timeout_seconds,
        )
        written, topics_read, failed, empty, code_results = _persist_batch(repo, batch_results, replace_empty=replace_empty)
        total_written += written
        total_topics += topics_read
        pending.extend(failed)
        empty_codes.update(empty)
        result["code_results"].extend(code_results)
        if sleep_first > 0 and batch_index < len(initial_batches) - 1:
            time.sleep(sleep_first)

    retry_rounds = 0
    while pending and retry_rounds < max_retry_rounds:
        retry_rounds += 1
        if sleep_retry > 0 and retry_rounds > 1:
            time.sleep(sleep_retry)
        retry_results = _fetch_batch(
            pending,
            batch_index=len(initial_batches) + retry_rounds - 1,
            pool_size=pool_size,
            timeout=timeout,
            workers=workers,
            hosts=hosts,
            batch_timeout_seconds=batch_timeout_seconds,
        )
        written, topics_read, failed, empty, code_results = _persist_batch(repo, retry_results, replace_empty=replace_empty)
        total_written += written
        total_topics += topics_read
        empty_codes.update(empty)
        result["code_results"].extend(code_results)
        pending = failed

    result["rows_read"] = total_topics
    result["rows_written"] = total_written
    result["succeeded_codes"] = len(codes) - len(set(pending))
    result["failed_codes"] = len(set(pending))
    result["failed_code_samples"] = sorted(set(pending))[:50]
    result["empty_codes"] = len(empty_codes)
    result["empty_code_samples"] = sorted(empty_codes)[:50]
    result["retry_rounds"] = retry_rounds
    return {**result, "code_results": result["code_results"][:50], "code_results_truncated": len(result["code_results"]) > 50}


def _fetch_batch(
    codes: list[str],
    *,
    batch_index: int,
    pool_size: int,
    timeout: float,
    workers: int,
    hosts: list[str],
    batch_timeout_seconds: float = 0,
) -> dict[str, dict[str, Any]]:
    if not codes:
        return {}
    host = hosts[batch_index % len(hosts)] if hosts else None
    client = None
    timed_out = False
    try:
        client = create_eltdx_client(pool_size=pool_size, timeout=timeout, host=host)
        connect = getattr(client, "connect", None)
        if callable(connect):
            connect()
        max_workers = max(1, min(workers, len(codes)))
        out: dict[str, dict[str, Any]] = {}
        futures = {}
        executor = ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="stock-sector")
        batch_timeout = batch_timeout_seconds if batch_timeout_seconds > 0 else max(120.0, timeout * ((len(codes) / max_workers) + 3))
        try:
            futures = {executor.submit(_fetch_one, client, code): code for code in codes}
            pending = set(futures)
            for future in as_completed(futures, timeout=batch_timeout):
                code = futures[future]
                pending.discard(future)
                try:
                    out[code] = future.result()
                except Exception as exc:  # noqa: BLE001
                    out[code] = {"code": code, "ok": False, "topics": [], "error": str(exc)}
            for future in pending:
                code = futures[future]
                out[code] = {"code": code, "ok": False, "topics": [], "error": f"batch timeout after {batch_timeout:.1f}s"}
                future.cancel()
        except FuturesTimeoutError:
            timed_out = True
            for future, code in futures.items():
                if future.done():
                    try:
                        out[code] = future.result()
                    except Exception as exc:  # noqa: BLE001
                        out[code] = {"code": code, "ok": False, "topics": [], "error": str(exc)}
                else:
                    out[code] = {"code": code, "ok": False, "topics": [], "error": f"batch timeout after {batch_timeout:.1f}s"}
                    future.cancel()
        finally:
            executor.shutdown(wait=False, cancel_futures=True)
        return out
    except Exception as exc:  # noqa: BLE001
        return {code: {"code": code, "ok": False, "topics": [], "error": str(exc)} for code in codes}
    finally:
        close = getattr(client, "close", None) if client is not None else None
        if callable(close) and not timed_out:
            close()


def _fetch_one(client, code: str) -> dict[str, Any]:
    try:
        topics = fetch_stock_topics(client, code)
    except Exception as exc:  # noqa: BLE001
        return {"code": code, "ok": False, "topics": [], "error": str(exc)}
    return {"code": code, "ok": bool(topics), "topics": topics, "error": None}


def _persist_batch(
    repo: StockSectorRepository,
    batch_results: dict[str, dict[str, Any]],
    *,
    replace_empty: bool,
) -> tuple[int, int, list[str], list[str], list[dict[str, Any]]]:
    total_written = 0
    total_topics = 0
    failed: list[str] = []
    empty: list[str] = []
    code_results: list[dict[str, Any]] = []
    for code, item in batch_results.items():
        topics = item.get("topics") or []
        total_topics += len(topics)
        if not item.get("ok"):
            empty.append(code)
            if replace_empty:
                written = repo.replace_stock_mappings(code, [])
                code_results.append({"code": code, "topics": 0, "mappings_written": written, "status": "empty_replaced"})
                continue
            failed.append(code)
            code_results.append({"code": code, "topics": 0, "mappings_written": 0, "status": "empty", "error": item.get("error")})
            continue
        mappings = stock_topics_to_sector_mappings(code, topics)
        written = repo.replace_stock_mappings(code, mappings)
        total_written += written
        code_results.append({"code": code, "topics": len(topics), "mappings_written": written, "status": "success"})
    return total_written, total_topics, failed, empty, code_results


def _chunk(items: list[str], size: int) -> list[list[str]]:
    resolved_size = max(1, size)
    return [items[index:index + resolved_size] for index in range(0, len(items), resolved_size)]


def _normalize_codes(value) -> list[str] | None:
    if value in (None, "", []):
        return None
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _normalize_hosts(value) -> list[str]:
    if value in (None, "", []):
        return []
    if value is True:
        return DEFAULT_TDX_TOPIC_HOSTS
    if isinstance(value, str):
        if value.lower() in {"1", "true", "yes", "y", "on", "default"}:
            return DEFAULT_TDX_TOPIC_HOSTS
        if value.lower() in {"0", "false", "no", "n", "off", "none"}:
            return []
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(item).strip() for item in value if str(item).strip()]


def _parse_date(value) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
