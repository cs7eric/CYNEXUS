from __future__ import annotations

from datetime import datetime, timedelta, timezone
from math import ceil
import json
from time import monotonic
from typing import Any

from cynexus_data.domain.market.market_indicator import (
    build_indicator_industry_snapshots,
    detect_indicator_events,
)
from cynexus_data.domain.market.transmission import (
    MarketContextSnapshot,
    TransmissionConfig,
    build_transmission_snapshot,
    to_jsonable,
)
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.indicator import create_eltdx_indicator_client, fetch_indicator_snapshots
from cynexus_data.storage.postgres.market_indicator import MarketIndicatorRepository
from cynexus_data.storage.redis.market_indicator import MarketIndicatorCache


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    repo = MarketIndicatorRepository(context.hooks.postgres.get_client())
    create_table = truthy(request.params.get("create_table"))
    if create_table:
        repo.create_table()
    if request.dry_run:
        return {"pipeline": "market.market_indicator_refresh", "dry_run": True, "rows_read": 0, "rows_written": 0,
                "notes": ["dry_run skipped eltdx and PostgreSQL access"]}
    all_metadata = repo.list_stock_metadata()
    metadata = dict(all_metadata)
    if not metadata:
        return {"pipeline": "market.market_indicator_refresh", "success": False, "error": "sec_stock_basic is empty"}
    workflow_started = monotonic()
    captured_at = datetime.now(timezone.utc)
    limit_history_days = max(1, parse_int(request.params.get("limit_history_days"), 365))
    historical_limit_codes = repo.list_historical_limit_codes(
        since_date=captured_at.date() - timedelta(days=limit_history_days),
    )
    universe_mode = str(request.params.get("universe_mode") or "full_market").strip().lower()
    if universe_mode in {"historical_limit", "pool", "bounded"}:
        metadata = {code: metadata[code] for code in metadata if code in historical_limit_codes}
    if not metadata:
        return {"pipeline": "market.market_indicator_refresh", "success": False,
                "error": "selected market indicator universe is empty",
                "universe_mode": universe_mode, "historical_limit_universe_size": len(historical_limit_codes)}
    limit_codes = parse_int(request.params.get("limit_codes"), 0)
    if limit_codes > 0:
        metadata = dict(list(metadata.items())[:limit_codes])
    candidates = [
        {
            "code": code,
            "pool_tier": "historical_limit" if code in historical_limit_codes else "market_all",
            "reason": f"limit_history_{limit_history_days}d" if code in historical_limit_codes else "full_market_refresh",
            "priority_score": None,
        }
        for code in metadata
    ]
    timeout = float(request.params.get("timeout") or 8.0)
    batch_size = parse_int(request.params.get("batch_size"), 240)
    client = create_eltdx_indicator_client(
        pool_size=parse_int(request.params.get("pool_size"), 8),
        timeout=timeout,
        batch_size=batch_size,
    )
    fetch_started = monotonic()
    try:
        rows = fetch_indicator_snapshots(
            client,
            metadata,
            batch_size=batch_size,
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
    min_snapshot_ratio = float(request.params.get("min_snapshot_ratio") or 0.8)
    coverage = len(rows) / len(metadata) if metadata else 0.0
    if not rows:
        return {"pipeline": "market.market_indicator_refresh", "success": False,
                "error": "historical limit-up/down pool returned no valid quotes",
                "candidate_pool_size": len(candidates), "rows_read": 0,
                "historical_limit_universe_size": len(historical_limit_codes)}
    if coverage < min_snapshot_ratio:
        return {"pipeline": "market.market_indicator_refresh", "success": False,
                "error": f"snapshot coverage {coverage:.1%} is below {min_snapshot_ratio:.1%}",
                "candidate_pool_size": len(candidates), "rows_read": len(rows),
                "snapshot_coverage": coverage, "historical_limit_universe_size": len(historical_limit_codes)}
    events: list[dict[str, Any]] = []
    industry_metadata = repo.list_industry_metadata()
    eligible_count_by_sector: dict[str, int] = {}
    for code, info in all_metadata.items():
        mapping = industry_metadata.get(code, {})
        sector_code = str(mapping.get("industry_code") or info.get("industry_code") or "").strip() or None
        sector_name = str(mapping.get("industry_name") or info.get("industry") or "未分类").strip() or "未分类"
        sector_key = sector_code or sector_name
        eligible_count_by_sector[sector_key] = eligible_count_by_sector.get(sector_key, 0) + 1
    for code, info in metadata.items():
        mapping = industry_metadata.setdefault(code, {
            "industry_name": "未分类",
            "industry_code": None,
        })
        info["industry_name"] = mapping.get("industry_name") or info.get("industry") or "未分类"
        info["industry_code"] = mapping.get("industry_code")
    for row in rows:
        mapping = industry_metadata.get(row["code"], {})
        row["industry_name"] = mapping.get("industry_name") or row.get("industry") or "未分类"
        row["industry"] = row["industry_name"]
        row["industry_code"] = mapping.get("industry_code")
        row["membership_snapshot_date"] = mapping.get("snapshot_date")
    capture_date = rows[0]["captured_at"].date() if rows else captured_at.date()
    cache: MarketIndicatorCache | None = None
    cache_warnings: list[str] = []
    if truthy(request.params.get("redis_cache", True)):
        try:
            cache = MarketIndicatorCache(
                context.hooks.redis.get_client(),
                prefix=str(request.params.get("redis_prefix") or "cynexus:market-indicator"),
                ttl_seconds=parse_int(request.params.get("redis_ttl_seconds"), 86400),
            )
            cache.client.ping()
        except Exception as exc:  # noqa: BLE001
            cache = None
            cache_warnings.append(f"redis_cache_unavailable:{type(exc).__name__}")

    previous_by_code = {}
    if cache:
        try:
            previous_by_code = cache.latest_snapshots(capture_date)
        except Exception as exc:  # noqa: BLE001
            cache_warnings.append(f"redis_latest_unavailable:{type(exc).__name__}")
    if not previous_by_code and truthy(request.params.get("persist_postgres", True)):
        previous_by_code = repo.latest_snapshots(capture_date)
    for row in rows:
        previous = previous_by_code.get(row["code"])
        events.extend(detect_indicator_events(
            row,
            previous,
            rapid_change_pct=float(request.params.get("rapid_change_pct") or 1.0),
            max_gap_minutes=parse_int(request.params.get("max_gap_minutes"), 10),
        ))
    industry_rows = build_indicator_industry_snapshots(rows, events, industry_metadata)
    stored_context = {}
    if not request.params.get("market_context") and not request.params.get("market_context_json"):
        try:
            stored_context = repo.latest_market_context_inputs(captured_at.date())
        except Exception as exc:  # noqa: BLE001
            cache_warnings.append(f"market_context_unavailable:{type(exc).__name__}")
    market_context = _market_context_from_request(request, rows, captured_at, stored_context=stored_context)
    full_confirmation_required = truthy(
        request.params.get("full_confirmation_required", universe_mode in {"historical_limit", "pool", "bounded"})
    )
    transmission = build_transmission_snapshot(
        rows,
        previous_by_code,
        market_context=market_context,
        config=TransmissionConfig(
            signal_window_seconds=parse_int(request.params.get("signal_window_seconds"), 5),
            rapid_return_pct=float(request.params.get("transmission_rapid_return_pct") or 0.8),
            follower_return_pct=float(request.params.get("transmission_follower_return_pct") or 0.3),
            eligible_count_by_sector=eligible_count_by_sector,
            full_confirmation_required=full_confirmation_required,
        ),
        detected_at=captured_at,
    )
    transmission_payload = to_jsonable(transmission)
    target_cadence = max(1, parse_int(request.params.get("target_cadence_seconds"), 5))
    pre_persist_elapsed = max(0.001, monotonic() - workflow_started)
    transmission_payload["runtime"] = {
        "workflowElapsedSeconds": round(pre_persist_elapsed, 3),
        "targetCadenceSeconds": target_cadence,
        "effectiveCadenceSeconds": max(target_cadence, int(ceil(pre_persist_elapsed * 1.25))),
        "benchmarkGate": "pass" if pre_persist_elapsed <= target_cadence else "degrade_cadence",
        "scope": str(request.params.get("scope") or ("priority" if universe_mode != "full_market" else "full")),
        "universeMode": universe_mode,
        "fullConfirmationRequired": full_confirmation_required,
        "persistPostgres": truthy(request.params.get("persist_postgres", True)),
    }
    source = str(request.params.get("source") or "eltdx.quotes")
    written = {"snapshots": 0, "events": 0, "industries": 0}
    staged = None
    cache_mode = "postgres_direct"
    persist_postgres = truthy(request.params.get("persist_postgres", True))
    if cache:
        try:
            staged = cache.stage_capture(
                captured_at=captured_at,
                rows=rows,
                events=events,
                industry_rows=industry_rows,
                candidate_rows=candidates,
                source=source,
                transmission=transmission_payload,
                retain_pending=persist_postgres,
            )
            cache_mode = "redis_staged"
            if persist_postgres:
                written = cache.flush_due(
                    repo,
                    min_age_seconds=parse_int(request.params.get("persistence_interval_seconds"), 180),
                    max_captures=parse_int(request.params.get("max_persist_captures"), 1),
                )
            else:
                written = {
                    "captures": 0,
                    "snapshots": 0,
                    "events": 0,
                    "industries": 0,
                    "pending_captures": staged.get("pending_captures", 0),
                }
        except Exception as exc:  # noqa: BLE001
            cache_warnings.append(f"redis_stage_or_flush_failed:{type(exc).__name__}")
            cache_mode = "postgres_fallback"
            written = repo.save_capture(
                rows,
                events,
                industry_rows,
                source=source,
                candidate_rows=candidates,
                captured_at=captured_at,
                transmission=transmission_payload,
            ) if persist_postgres else {"captures": 0, "snapshots": 0, "events": 0, "industries": 0}
    elif persist_postgres:
        written = repo.save_capture(
            rows,
            events,
            industry_rows,
            source=source,
            candidate_rows=candidates,
            captured_at=captured_at,
            transmission=transmission_payload,
        )
    elapsed_seconds = max(0.001, monotonic() - workflow_started)
    effective_cadence = max(target_cadence, int(ceil(elapsed_seconds * 1.25)))
    return {"pipeline": "market.market_indicator_refresh", "success": True, "rows_read": len(rows),
            "rows_written": written["snapshots"], "events_written": written["events"],
            "industry_rows_written": written["industries"], "candidate_pool_written": len(candidates),
            "candidate_pool_size": len(candidates), "historical_limit_universe_size": len(historical_limit_codes),
            "snapshot_coverage": coverage, "source": source, "cache_mode": cache_mode,
            "universe_mode": universe_mode,
            "scope": transmission_payload["runtime"]["scope"],
            "full_confirmation_required": full_confirmation_required,
            "persist_postgres": persist_postgres,
            "workflow_elapsed_seconds": round(elapsed_seconds, 3),
            "fetch_elapsed_seconds": round(max(0.0, monotonic() - fetch_started), 3),
            "target_cadence_seconds": target_cadence, "effective_cadence_seconds": effective_cadence,
            "benchmark_gate": "pass" if elapsed_seconds <= target_cadence else "degrade_cadence",
            "transmission_signal_count": len(transmission.get("signal_events") or []),
            "transmission_episode_count": len(transmission.get("episodes") or []),
            "transmission_advice_count": len(transmission.get("advice") or []),
            "transmission_quality_grade": transmission["quality"].grade.value,
            "rows_staged": len(rows) if staged else 0, "events_staged": len(events) if staged else 0,
            "persisted_captures": written.get("captures", 1 if cache_mode != "redis_staged" else 0),
            "pending_captures": written.get("pending_captures", staged.get("pending_captures", 0) if staged else 0),
            "warnings": cache_warnings}


def _market_context_from_request(
    request: JobRequest,
    rows: list[dict[str, Any]],
    captured_at: datetime,
    *,
    stored_context: dict[str, Any] | None = None,
) -> MarketContextSnapshot:
    """Build event-time market context from the current pulse payload when supplied.

    The collector stays provider-agnostic: schedulers may pass a JSON context
    assembled from market pulse/index/sentiment snapshots, while the fallback
    derives breadth from the same full-market quote capture.
    """
    raw = request.params.get("market_context") or request.params.get("market_context_json")
    supplied: dict[str, Any] = dict(stored_context or {})
    if isinstance(raw, dict):
        supplied = dict(raw)
    elif raw:
        try:
            parsed = json.loads(str(raw))
            if isinstance(parsed, dict):
                supplied = parsed
        except (TypeError, ValueError):
            supplied = {}
    changes = [float(row["change_pct"]) for row in rows if row.get("change_pct") is not None]
    advancing = sum(1 for value in changes if value > 0)
    declining = sum(1 for value in changes if value < 0)
    flat = len(changes) - advancing - declining
    breadth = {"observedCount": len(rows), "advancingCount": advancing, "decliningCount": declining, "flatCount": flat}
    breadth.update(dict(supplied.get("breadth") or {}))
    indices = dict(supplied.get("indices") or {})
    if request.params.get("benchmark_return_pct") is not None:
        indices.setdefault("benchmarkReturnPct", float(request.params["benchmark_return_pct"]))
    now = captured_at
    # Never mix a daily/weekly benchmark return into a seconds-level signal.
    # Only an explicitly intraday (<=5 minute) benchmark may be used as the
    # market-excess baseline.  Otherwise the signal remains unbenchmarked and
    # the UI can surface that missing evidence honestly.
    stored_indices = dict(supplied.get("indices") or {})
    benchmark_return = None
    for key in ("000300", "000001", "SH000300", "399001"):
        candidate = stored_indices.get(key)
        if not isinstance(candidate, dict):
            continue
        window_minutes = candidate.get("windowMinutes")
        window_days = candidate.get("windowDays")
        intraday = window_minutes is not None and float(window_minutes) <= 5
        zero_day = window_days is not None and float(window_days) == 0
        if (intraday or zero_day) and candidate.get("returnPct") is not None:
            benchmark_return = candidate.get("returnPct")
            break
    if benchmark_return is not None:
        indices.setdefault("benchmarkReturnPct", float(benchmark_return))
    sentiment_payload = dict(supplied.get("sentiment") or {})
    msi_value = sentiment_payload.get("msi")
    if msi_value is not None:
        sentiment_payload.setdefault("score", msi_value)
    return MarketContextSnapshot(
        context_id=str(supplied.get("contextId") or supplied.get("context_id") or f"market:{now.isoformat()}"),
        trade_date=now.date().isoformat(), bucket_at=now, as_of=now,
        trading_phase=str(supplied.get("tradingPhase") or supplied.get("trading_phase") or "intraday"),
        regime=str(supplied.get("regime") or request.params.get("market_regime") or "unknown"),
        regime_strength=float(supplied.get("regimeStrength") or supplied.get("regime_strength") or 0.0),
        indices=indices, breadth=breadth, liquidity=dict(supplied.get("liquidity") or {}),
        sentiment=sentiment_payload, style=dict(supplied.get("style") or {}),
        data_quality=transmission_quality(rows),
        provenance={"source": "market_indicator_capture", "supplied": bool(supplied)},
    )


def transmission_quality(rows: list[dict[str, Any]]):
    from cynexus_data.domain.market.transmission import DataQuality

    valid = sum(1 for row in rows if row.get("last_price") is not None)
    mapped = sum(1 for row in rows if row.get("industry_code") or row.get("industry_name") not in (None, "", "未分类"))
    return DataQuality.from_counts(eligible_count=len(rows), observed_count=valid, fresh_count=valid, mapped_count=mapped)
