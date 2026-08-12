from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Any

from cynexus_data.domain.market.limit_board import build_limit_board_sample
from cynexus_data.domain.market.limit_board_session import (
    aggregate_limit_board_session,
    closed_trade_session,
    current_trade_session,
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


def capture(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
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

    postgres = context.hooks.postgres.get_client()
    market_repo = MarketIndicatorRepository(postgres)
    if truthy(request.params.get("create_table")):
        market_repo.create_table()
    metadata = market_repo.list_stock_metadata()
    if not metadata:
        return {"pipeline": "market.limit_board_monitor.capture", "success": False, "error": "sec_stock_basic is empty"}

    trade_day = _trade_date(captured_at)
    cache = MarketLimitBoardSessionCache(
        context.hooks.redis.get_client(),
        prefix=str(request.params.get("redis_prefix") or DEFAULT_PREFIX),
        ttl_seconds=parse_int(request.params.get("redis_ttl_seconds"), 43200),
    )
    directions = cache.seen_directions(trade_date=trade_day, trade_session=session)
    seen_codes = set(directions) | cache.seen_codes(trade_date=trade_day, trade_session=session)
    discovery = _needs_discovery(
        cache.metadata(trade_date=trade_day, trade_session=session).get("last_discovery_at"),
        captured_at,
        parse_int(request.params.get("discovery_interval_seconds"), 30),
    )
    requested_codes = None if discovery else seen_codes
    if requested_codes == set():
        discovery = True
        requested_codes = None

    client = create_eltdx_indicator_client(
        pool_size=parse_int(request.params.get("pool_size"), 8),
        timeout=float(request.params.get("timeout") or 8.0),
        batch_size=parse_int(request.params.get("batch_size"), 240),
    )
    try:
        fetched = fetch_indicator_snapshots(
            client,
            metadata,
            requested_codes=requested_codes,
            batch_size=parse_int(request.params.get("batch_size"), 240),
            captured_at=captured_at,
            retry_attempts=parse_int(request.params.get("quote_retry_attempts"), 2),
            retry_backoff_seconds=float(request.params.get("quote_retry_backoff_seconds") or 0.25),
            request_spacing_seconds=float(request.params.get("quote_request_spacing_seconds") or 0.02),
            fallback_batch_size=parse_int(request.params.get("quote_fallback_batch_size"), 80),
        )
    finally:
        close = getattr(client, "close", None)
        if callable(close):
            close()

    newly_seen: dict[str, str] = {}
    samples: list[dict[str, Any]] = []
    for row in fetched:
        code = str(row.get("code") or "").strip()[-6:].zfill(6)
        status = str(row.get("limit_status") or "")
        direction = _status_direction(status) or directions.get(code)
        if direction not in {"up", "down"}:
            continue
        if _status_direction(status):
            newly_seen[code] = direction
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
        discovered_at=captured_at if discovery else None,
        source=SOURCE,
    )
    return {
        "pipeline": "market.limit_board_monitor.capture",
        "success": True,
        "trade_date": trade_day.isoformat(),
        "trade_session": session,
        "discovery": discovery,
        "rows_read": len(fetched),
        "rows_written": len(samples),
        "seen_codes": len(all_seen),
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
