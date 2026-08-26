from __future__ import annotations

from datetime import date, datetime, timezone
from time import monotonic, sleep
from typing import Any

from cynexus_data.domain.market.limit_board import build_limit_board_sample, last_price_limit_direction
from cynexus_data.domain.market.limit_board_session import (
    aggregate_limit_board_session,
    closed_trade_session,
    current_trade_session,
    session_bounds,
)
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.indicator import create_eltdx_indicator_client, fetch_indicator_snapshots
from cynexus_data.storage.postgres.market_indicator import MarketIndicatorRepository
from cynexus_data.storage.postgres.market_limit_board_session import MarketLimitBoardSessionRepository
from cynexus_data.storage.redis.market_limit_board_session import MarketLimitBoardSessionCache


DEFAULT_PREFIX = "cynexus:market-indicator:limit-board:v1"
SOURCE = "eltdx.quotes.best_quote"


def run_session(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    """Own a short sequence of captures so the scheduler does not overlap them."""
    target = max(1, parse_int(request.params.get("target_cadence_seconds"), 3))
    duration = max(1, parse_int(request.params.get("duration_seconds"), 300))
    max_cycles = max(1, parse_int(request.params.get("max_cycles"), duration // target + 1))
    if request.dry_run:
        return {
            "pipeline": "market.limit_board_monitor.session",
            "dry_run": True,
            "success": True,
            "target_cadence_seconds": target,
            "duration_seconds": duration,
            "max_cycles": max_cycles,
            "notes": ["one scheduler run owns sequential limit-board captures"],
        }

    started = monotonic()
    cycles: list[dict[str, Any]] = []
    failures = 0
    failure_backoff = max(0.0, float(request.params.get("failure_backoff_seconds") or 2.0))
    session_key: tuple[date, str] | None = None
    session_cache = None
    session_metadata: dict[str, dict[str, Any]] | None = None
    session_end: datetime | None = None
    if context.hooks is not None:
        session_at = _requested_datetime(request.params.get("captured_at")) or datetime.now(timezone.utc)
        session_name = current_trade_session(session_at)
        if session_name is not None:
            session_key = (_trade_date(session_at), session_name)
            session_end = session_bounds(session_key[0], session_name)[1].astimezone(timezone.utc)
            session_cache = MarketLimitBoardSessionCache(
                context.hooks.redis.get_client(),
                prefix=str(request.params.get("redis_prefix") or DEFAULT_PREFIX),
                ttl_seconds=parse_int(request.params.get("redis_ttl_seconds"), 43200),
            )
            session_cache.set_session_state(
                trade_date=session_key[0], trade_session=session_key[1], state="active", at=session_at
            )
            postgres = context.hooks.postgres.get_client()
            market_repo = MarketIndicatorRepository(postgres)
            if truthy(request.params.get("create_table")):
                market_repo.create_table()
            session_metadata = market_repo.list_stock_metadata()
    if session_key is None:
        return {
            "pipeline": "market.limit_board_monitor.session",
            "success": True,
            "status": "skipped",
            "reason": "outside_trade_session",
            "cycles": 0,
        }
    while len(cycles) < max_cycles and monotonic() - started < duration:
        if session_end is not None:
            remaining_session = (datetime.now(timezone.utc) - session_end).total_seconds()
            if remaining_session >= 0:
                break
        cycle_started = monotonic()
        heartbeat_at = _requested_datetime(request.params.get("captured_at")) or datetime.now(timezone.utc)
        if session_cache is not None and session_key is not None:
            session_cache.set_session_state(
                trade_date=session_key[0], trade_session=session_key[1], state="active", at=heartbeat_at
            )
        try:
            result = capture(context, request, metadata=session_metadata, cache=session_cache)
        except Exception as exc:  # Keep the session alive after a transient provider failure.
            failures += 1
            result = {
                "pipeline": "market.limit_board_monitor.capture",
                "success": False,
                "status": "failed",
                "error": f"{type(exc).__name__}: {exc}",
            }
        elapsed = monotonic() - cycle_started
        cycles.append({**result, "cycle": len(cycles) + 1, "elapsed_seconds": round(elapsed, 3)})
        remaining = target - elapsed
        if not result.get("success"):
            remaining = max(0.0, remaining) + failure_backoff
        if remaining > 0 and len(cycles) < max_cycles:
            sleep(min(remaining, max(0.0, duration - (monotonic() - started))))
    if session_cache is not None and session_key is not None:
        session_cache.set_session_state(
            trade_date=session_key[0], trade_session=session_key[1], state="completed",
            at=datetime.now(timezone.utc),
        )
    rows_read = sum(int(item.get("rows_read") or 0) for item in cycles)
    rows_written = sum(int(item.get("rows_written") or 0) for item in cycles)
    return {
        "pipeline": "market.limit_board_monitor.session",
        "success": all(bool(item.get("success")) for item in cycles) if cycles else False,
        "failed_cycles": failures,
        "cycles": len(cycles),
        "rows_read": rows_read,
        "rows_written": rows_written,
        "warnings": [f"{failures} capture cycle(s) failed; gaps are excluded from confirmed duration"] if failures else [],
        "target_cadence_seconds": target,
        "session_elapsed_seconds": round(monotonic() - started, 3),
        "cycle_results": cycles[-10:],
    }


def capture(
    context: PipelineContext,
    request: JobRequest,
    *,
    metadata: dict[str, dict[str, Any]] | None = None,
    cache: MarketLimitBoardSessionCache | None = None,
) -> dict[str, Any]:
    captured_at = _requested_datetime(request.params.get("captured_at")) or datetime.now(timezone.utc)
    session = current_trade_session(captured_at)
    if session is None:
        return {"pipeline": "market.limit_board_monitor.capture", "success": True, "status": "skipped", "reason": "outside_trade_session"}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    if request.dry_run:
        return {
            "pipeline": "market.limit_board_monitor.capture",
            "dry_run": True,
            "success": True,
            "trade_date": _trade_date(captured_at).isoformat(),
            "trade_session": session,
            "notes": ["dry_run skipped eltdx, Redis, and PostgreSQL access"],
        }

    if metadata is None:
        postgres = context.hooks.postgres.get_client()
        market_repo = MarketIndicatorRepository(postgres)
        if truthy(request.params.get("create_table")):
            market_repo.create_table()
        metadata = market_repo.list_stock_metadata()
    if not metadata:
        return {"pipeline": "market.limit_board_monitor.capture", "success": False, "error": "sec_stock_basic is empty"}

    trade_day = _trade_date(captured_at)
    if cache is None:
        cache = MarketLimitBoardSessionCache(
            context.hooks.redis.get_client(),
            prefix=str(request.params.get("redis_prefix") or DEFAULT_PREFIX),
            ttl_seconds=parse_int(request.params.get("redis_ttl_seconds"), 43200),
        )
    directions = cache.seen_directions(trade_date=trade_day, trade_session=session)
    seen_codes = set(directions) | cache.seen_codes(trade_date=trade_day, trade_session=session)
    # Whole-market discovery belongs to market_indicator_refresh. The
    # high-frequency sampler only follows its shared Redis notifications.
    if not seen_codes:
        return {
            "pipeline": "market.limit_board_monitor.capture",
            "success": True,
            "status": "waiting_for_discovery",
            "trade_date": trade_day.isoformat(),
            "trade_session": session,
            "rows_read": 0,
            "rows_written": 0,
            "seen_codes": 0,
        }
    requested_codes = seen_codes

    client = create_eltdx_indicator_client(
        pool_size=parse_int(request.params.get("pool_size"), 8),
        timeout=float(request.params.get("timeout") or 8.0),
        batch_size=parse_int(request.params.get("batch_size"), 80),
    )
    try:
        fetched = fetch_indicator_snapshots(
            client,
            metadata,
            requested_codes=requested_codes,
            batch_size=parse_int(request.params.get("batch_size"), 80),
            captured_at=captured_at,
            retry_attempts=parse_int(request.params.get("quote_retry_attempts"), 0),
            retry_backoff_seconds=float(request.params.get("quote_retry_backoff_seconds") or 0.25),
            request_spacing_seconds=float(request.params.get("quote_request_spacing_seconds") or 0.02),
            fallback_batch_size=parse_int(request.params.get("quote_fallback_batch_size"), 80),
            allow_partial_failures=True,
        )
    finally:
        close = getattr(client, "close", None)
        if callable(close):
            close()

    fetched_codes = {str(row.get("code") or "").strip()[-6:].zfill(6) for row in fetched}
    unavailable_codes = requested_codes - fetched_codes
    newly_seen: dict[str, str] = {}
    samples: list[dict[str, Any]] = []
    for row in fetched:
        code = str(row.get("code") or "").strip()[-6:].zfill(6)
        status = str(row.get("limit_status") or "")
        live_direction = last_price_limit_direction(row)
        direction = live_direction or _status_direction(status) or directions.get(code)
        if direction not in {"up", "down"}:
            continue
        if live_direction:
            newly_seen[code] = live_direction
        if code not in seen_codes and code not in newly_seen:
            continue
        sample = build_limit_board_sample(row, prior_direction=direction, source=SOURCE)
        if sample is not None:
            samples.append(sample)

    all_seen = {**directions, **newly_seen}
    cache.append_samples(
        trade_date=trade_day,
        trade_session=session,
        samples=samples,
        seen=all_seen,
        discovered_at=None,
        source=SOURCE,
    )
    return {
        "pipeline": "market.limit_board_monitor.capture",
        "success": True,
        "trade_date": trade_day.isoformat(),
        "trade_session": session,
        "discovery": False,
        "rows_read": len(fetched),
        "rows_written": len(samples),
        "seen_codes": len(all_seen),
        "unavailable_or_invalid_codes": len(unavailable_codes),
        "warnings": [f"{len(unavailable_codes)} tracked code(s) returned no usable quote"] if unavailable_codes else [],
    }


def finalize(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    now = _requested_datetime(request.params.get("finalized_at")) or datetime.now(timezone.utc)
    session = str(request.params.get("trade_session") or closed_trade_session(now) or "")
    if session not in {"morning", "afternoon"}:
        return {"pipeline": "market.limit_board_monitor.finalize", "success": True, "status": "skipped", "reason": "no_closed_trade_session"}
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    trade_day = _parse_date(request.params.get("trade_date")) or _trade_date(now)
    if request.dry_run:
        return {
            "pipeline": "market.limit_board_monitor.finalize",
            "dry_run": True,
            "success": True,
            "trade_date": trade_day.isoformat(),
            "trade_session": session,
            "notes": ["dry_run skipped Redis and PostgreSQL access"],
        }

    cache = MarketLimitBoardSessionCache(
        context.hooks.redis.get_client(),
        prefix=str(request.params.get("redis_prefix") or DEFAULT_PREFIX),
        ttl_seconds=parse_int(request.params.get("redis_ttl_seconds"), 43200),
    )
    is_active = getattr(cache, "is_session_active", None)
    if callable(is_active) and is_active(
        trade_date=trade_day,
        trade_session=session,
        now=now,
        stale_after_seconds=parse_int(request.params.get("session_stale_after_seconds"), 90),
    ):
        return {
            "pipeline": "market.limit_board_monitor.finalize",
            "success": True,
            "status": "skipped",
            "trade_date": trade_day.isoformat(),
            "trade_session": session,
            "reason": "capture_session_active",
        }
    rows = cache.read_samples(trade_date=trade_day, trade_session=session)
    if not rows:
        return {
            "pipeline": "market.limit_board_monitor.finalize",
            "success": True,
            "status": "skipped",
            "trade_date": trade_day.isoformat(),
            "trade_session": session,
            "reason": "no_cached_samples",
        }
    summaries = aggregate_limit_board_session(
        rows,
        trade_date=trade_day,
        trade_session=session,
        sample_interval_seconds=parse_int(request.params.get("sample_interval_seconds"), 5),
        source=str(request.params.get("source") or SOURCE),
    )
    if not summaries:
        return {
            "pipeline": "market.limit_board_monitor.finalize",
            "success": False,
            "error": "cached limit-board samples produced no summaries",
            "rows_read": len(rows),
        }
    repo = MarketLimitBoardSessionRepository(context.hooks.postgres.get_client())
    if truthy(request.params.get("create_table")):
        repo.create_table()
    written = repo.save_summaries(summaries, source=str(request.params.get("source") or SOURCE))
    cache.delete_session(trade_date=trade_day, trade_session=session)
    return {
        "pipeline": "market.limit_board_monitor.finalize",
        "success": True,
        "trade_date": trade_day.isoformat(),
        "trade_session": session,
        "rows_read": len(rows),
        "rows_written": written,
        "redis_deleted": True,
    }


def _needs_discovery(last: str | None, captured_at: datetime, interval_seconds: int) -> bool:
    if not last:
        return True
    try:
        previous = datetime.fromisoformat(last.replace("Z", "+00:00"))
    except ValueError:
        return True
    return (captured_at - previous).total_seconds() >= max(5, int(interval_seconds))


def _status_direction(status: str) -> str | None:
    if status.startswith("limit_up_"):
        return "up"
    if status.startswith("limit_down_"):
        return "down"
    return None


def _requested_datetime(value: Any) -> datetime | None:
    if value is None or value == "":
        return None
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError(f"invalid datetime: {value}") from exc
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def _parse_date(value: Any) -> date | None:
    if value is None or value == "":
        return None
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    return date.fromisoformat(str(value).replace("/", "-"))


def _trade_date(value: datetime) -> date:
    from zoneinfo import ZoneInfo

    return value.astimezone(ZoneInfo("Asia/Shanghai")).date()
