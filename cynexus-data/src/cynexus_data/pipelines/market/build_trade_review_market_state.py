from __future__ import annotations

import base64
import gzip
import json
from datetime import date, datetime
from typing import Any

from sqlalchemy import text

from cynexus_data.domain.market.market_indicator import _round_price, _threshold_ratio
from cynexus_data.domain.market.trade_review_market_state import (
    build_daily_projection,
    build_limit_intervals,
    build_minute_projections,
    build_security_daily_facts,
    classify_phase,
    normalize_indicator_events,
)
from cynexus_data.domain.stock.stock_meta import effective_limit_up_threshold
from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.msi._params import parse_date, parse_int, truthy
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.storage.postgres.trade_review_market_state import TradeReviewMarketStateRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    """Build one day, or an ordered range of available trading days.

    A range is intentionally processed in chronological order: a day only
    reads earlier daily projections when producing its trend.  This is both
    the historical backfill path and the guard against future-data leakage.
    """
    start_date = parse_date(request.params.get("start_date"))
    end_date = parse_date(request.params.get("end_date"))
    if start_date or end_date:
        start = start_date or end_date
        end = end_date or start_date
        if start is None or end is None or start > end:
            raise ValueError("start_date must not be after end_date")
        dates = _available_trade_dates(context, start, end)
        results = [_run_with_failure_manifest(context, request, item) for item in dates]
        return {
            "pipeline": "market.build_trade_review_market_state",
            "start_date": start.isoformat(), "end_date": end.isoformat(),
            "trade_dates": [item.isoformat() for item in dates], "dry_run": request.dry_run,
            "day_results": results,
            "rows_read": sum(int(item.get("rows_read") or 0) for item in results),
            "rows_written": sum(int(item.get("rows_written") or 0) for item in results),
            "status": "READY" if results else "UNAVAILABLE",
        }
    trade_date = parse_date(request.params.get("trade_date")) or date.today()
    return _run_with_failure_manifest(context, request, trade_date)


def _run_with_failure_manifest(context: PipelineContext, request: JobRequest, trade_date: date) -> dict[str, Any]:
    """Run one day and leave an operator-visible failure manifest on errors."""
    try:
        return _run(context, request, trade_date)
    except Exception as exc:
        # A failed build must be observable by the Panel and by the operator.
        # This is deliberately best-effort: the original data exception is
        # never replaced if PostgreSQL itself is unavailable.
        if not request.dry_run and context.hooks is not None:
            try:
                TradeReviewMarketStateRepository(context.hooks.postgres.get_client()).update_manifest(
                    trade_date,
                    status="FAILED", minute_bar_count=0, minute_symbol_count=0, context_count=0,
                    indicator_event_count=0, market_minute_count=0, industry_minute_count=0,
                    daily_pool_count=0, cache_published=False,
                    quality={"status": "FAILED", "failureStage": "materialization"},
                    error_message=f"{type(exc).__name__}: {exc}"[:4000],
                )
            except Exception:
                pass
        raise


def _available_trade_dates(context: PipelineContext, start_date: date, end_date: date) -> list[date]:
    """Return only raw-1m dates, never inventing natural-calendar sessions."""
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    rows = context.hooks.clickhouse.get_client().query_rows("""
        SELECT DISTINCT trade_date
          FROM ch_dwd_kline_1m FINAL
         WHERE trade_date >= %(start_date)s AND trade_date <= %(end_date)s
           AND period = 1 AND source = 'tdx_local'
         ORDER BY trade_date
    """, {"start_date": start_date, "end_date": end_date})
    result: list[date] = []
    for (value,) in rows:
        if isinstance(value, datetime):
            value = value.date()
        if isinstance(value, date):
            result.append(value)
        else:
            result.append(date.fromisoformat(str(value)[:10]))
    return result


def _run(context: PipelineContext, request: JobRequest, trade_date: date) -> dict[str, Any]:
    window_days = max(5, min(240, parse_int(request.params.get("window_days"), 60)))
    result: dict[str, Any] = {
        "pipeline": "market.build_trade_review_market_state", "trade_date": trade_date.isoformat(),
        "dry_run": request.dry_run, "rows_read": 0, "rows_written": 0,
    }
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")
    pg = context.hooks.postgres.get_client()
    repo = TradeReviewMarketStateRepository(pg)
    if truthy(request.params.get("create_table")):
        repo.create_tables()

    metadata = _load_security_metadata(pg)
    # The raw 1m table contains stock, ETF and index records.  Six-digit
    # codes are not globally unique (notably SH000001 versus SZ000001), so
    # restrict the materialization universe by the persisted market+code
    # security catalogue *before* collapsing a code for the durable model.
    # Otherwise an index bar may overwrite the identically numbered stock bar.
    bars = _load_minute_bars(context, trade_date, metadata)
    previous_closes = _load_previous_closes(context, trade_date, metadata)
    limit_rows, previous_limit_rows = _load_limit_rows(pg, trade_date)
    memberships = _load_memberships(pg, trade_date)
    contexts = _build_contexts(metadata, bars, previous_closes, limit_rows, previous_limit_rows, memberships)
    raw_events = _load_indicator_events(pg, trade_date)
    events = normalize_indicator_events(raw_events, contexts)
    intervals = build_limit_intervals(events)
    projections = build_minute_projections(bars, contexts)
    # End-of-day facts are deliberately derived from raw bars, not copied
    # from the final minute market aggregate.  They are the daily replay
    # foundation for pools and for a later card/detail query.
    daily_facts = build_security_daily_facts(bars, contexts, events)
    daily_market, daily_pools = build_daily_projection(
        projections, contexts, events, current_limit_rows=limit_rows,
    )
    daily_industries = [item for item in projections[-1].industries] if projections else []
    _attach_daily_industry_factors(pg, trade_date, contexts, daily_industries)
    # These readers deliberately exclude ``trade_date``.  A rerun must not
    # include its previous projection and then append the newly computed day
    # a second time, which would make the phase non-idempotent.
    history = repo.recent_daily_market_states(trade_date, window_days - 1)
    phase = classify_phase([*history, daily_market] if daily_market else history, window_days=window_days)
    industry_history = repo.recent_daily_industry_states(trade_date, window_days - 1)
    industry_phases = _classify_industry_phases(industry_history, daily_industries, window_days=window_days)

    result.update({
        "rows_read": len(bars) + len(raw_events), "minute_bar_count": len(bars),
        "minute_symbol_count": len({_code(item.get("symbol")) for item in bars}),
        "context_count": len(contexts), "indicator_event_count": len(events),
        "market_minute_count": len(projections), "industry_minute_count": sum(len(item.industries) for item in projections),
    })
    if request.dry_run:
        result.update({
            "sample_context": list(contexts.values())[:3], "sample_market_minute": projections[0].market if projections else None,
            "daily_market": daily_market, "sample_daily_fact": daily_facts[:3], "daily_fact_count": len(daily_facts), "phase": phase,
            "notes": ["dry_run read raw data but did not write PostgreSQL or Redis"],
        })
        return result

    repo.replace_contexts(trade_date, contexts)
    daily_fact_count = repo.replace_security_daily_facts(trade_date, daily_facts)
    event_count, interval_count = repo.replace_events_and_intervals(trade_date, events, intervals)
    market_count, industry_count, pool_count = repo.replace_minute_states(trade_date, projections)
    daily_count, daily_industry_count, daily_pool_count = repo.replace_daily(
        trade_date, daily_market, daily_industries, daily_pools, phase, window_days, industry_phases,
    )
    minute_coverage = len(projections) / 240 if projections else 0.0
    coverage_complete = len(projections) >= 230
    quality = {
        "minuteBarCount": len(bars), "minuteSymbolCount": result["minute_symbol_count"],
        "contextCount": len(contexts), "indicatorEventCount": event_count,
        "marketMinuteCount": len(projections), "minuteCoverage": round(minute_coverage, 4),
        "coverageComplete": coverage_complete, "hasDailyState": bool(daily_market),
        "source": "clickhouse.ch_dwd_kline_1m+postgres.market_indicator",
    }
    status = "READY" if coverage_complete and contexts else "PARTIAL" if projections and contexts else "UNAVAILABLE"
    cache_published = _publish_cache(
        context, trade_date, daily_market, daily_industries, phase, projections, daily_pools, events,
        [*history, daily_market] if daily_market else history, industry_phases,
        {"status": status, "minuteBarCount": len(bars), "minuteSymbolCount": result["minute_symbol_count"],
         "contextCount": len(contexts), "indicatorEventCount": event_count, "quality": quality}, request,
    )
    repo.update_manifest(
        trade_date, status=status, minute_bar_count=len(bars), minute_symbol_count=result["minute_symbol_count"],
        context_count=len(contexts), indicator_event_count=event_count, market_minute_count=market_count,
        industry_minute_count=industry_count, daily_pool_count=daily_pool_count,
        cache_published=cache_published, quality=quality, error_message=None,
    )
    result.update({
        "rows_written": len(contexts) + daily_fact_count + event_count + interval_count + market_count + industry_count + pool_count + daily_count + daily_industry_count + daily_pool_count,
        "interval_count": interval_count, "minute_pool_count": pool_count, "daily_industry_count": daily_industry_count,
        "daily_fact_count": daily_fact_count, "daily_pool_count": daily_pool_count, "cache_published": cache_published, "status": status, "phase": phase,
    })
    return result


def _load_security_metadata(pg) -> dict[str, dict[str, Any]]:
    with pg.session() as db:
        rows = db.execute(text(f"""
            SELECT code, name, market, is_st, limit_up_threshold, 'STOCK' AS security_type
              FROM {pg.schema}.sec_stock_basic
             WHERE is_active IS TRUE
            UNION ALL
            SELECT code, name,
                   CASE upper(exchange) WHEN 'SSE' THEN 'SH' WHEN 'SZSE' THEN 'SZ' ELSE upper(exchange) END AS market,
                   FALSE AS is_st, NULL::numeric AS limit_up_threshold, 'ETF' AS security_type
              FROM {pg.schema}.market_etf_catalog
             WHERE is_active IS TRUE AND deleted_at IS NULL
        """)).mappings().all()
    # A listed security code is unique inside the supported stock/ETF
    # catalogue.  The market is retained for matching raw TDX rows.
    return {_code(row["code"]): dict(row) for row in rows if _code(row.get("code"))}


def _load_minute_bars(
    context: PipelineContext, trade_date: date, metadata: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    client = context.hooks.clickhouse.get_client()
    rows = client.query_rows("""
        SELECT market, symbol, trade_time, open, high, low, close, volume, amount
          FROM ch_dwd_kline_1m FINAL
         WHERE trade_date = %(trade_date)s AND period = 1 AND source = 'tdx_local'
         ORDER BY trade_time, market, symbol
    """, {"trade_date": trade_date})
    selected: list[dict[str, Any]] = []
    for row in rows:
        item = dict(zip(("market", "symbol", "trade_time", "open", "high", "low", "close", "volume", "amount"), row, strict=True))
        code = _code(item["symbol"])
        meta = metadata.get(code)
        if meta and str(meta.get("market") or "").upper() == str(item["market"] or "").upper():
            selected.append(item)
    return selected


def _load_previous_closes(
    context: PipelineContext, trade_date: date, metadata: dict[str, dict[str, Any]],
) -> dict[str, float]:
    client = context.hooks.clickhouse.get_client()
    rows = client.query_rows("""
        SELECT market, symbol, argMax(close, trade_date) AS pre_close
          FROM daily_raw FINAL
         WHERE trade_date < %(trade_date)s
         GROUP BY market, symbol
    """, {"trade_date": trade_date})
    result: dict[str, float] = {}
    for market, symbol, close in rows:
        code = _code(symbol)
        meta = metadata.get(code)
        if not code or close is None or not meta:
            continue
        if str(meta.get("market") or "").upper() == str(market or "").upper():
            result[code] = float(close)
    return result


def _load_limit_rows(pg, trade_date: date) -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    with pg.session() as db:
        today = db.execute(text(f"""
            SELECT code, name, category, streak, change_pct, limit_up_price, limit_down_price
              FROM {pg.schema}.market_limit_daily_stocks
             WHERE trade_date = :trade_date AND deleted_at IS NULL
        """), {"trade_date": trade_date}).mappings().all()
        # "昨日" must be the immediately preceding *trading* date, not the
        # latest observation for each individual security.  The latter leaks
        # stale limit-up statuses across suspensions and holidays.
        previous = db.execute(text(f"""
            WITH previous_trade_date AS (
                SELECT max(trade_date) AS trade_date
                  FROM {pg.schema}.market_limit_daily_stocks
                 WHERE trade_date < :trade_date AND deleted_at IS NULL
            ) SELECT code, category, streak, change_pct
                FROM {pg.schema}.market_limit_daily_stocks
               WHERE trade_date = (SELECT trade_date FROM previous_trade_date)
                 AND deleted_at IS NULL
        """), {"trade_date": trade_date}).mappings().all()
    return ({_code(row["code"]): dict(row) for row in today}, {_code(row["code"]): dict(row) for row in previous})


def _load_memberships(pg, trade_date: date) -> dict[str, list[dict[str, str]]]:
    result: dict[str, list[dict[str, str]]] = {}
    with pg.session() as db:
        ths_rows = db.execute(text(f"""
            SELECT DISTINCT ON (stock_code, industry_code) stock_code, industry_code
              FROM {pg.schema}.ths_industry_constituent_snapshot
             WHERE snapshot_date <= :trade_date AND deleted_at IS NULL
             ORDER BY stock_code, industry_code, snapshot_date DESC
        """), {"trade_date": trade_date}).mappings().all()
        names = db.execute(text(f"""
            SELECT ths_code, max(ths_name) AS ths_name, max(tdx_code6) AS tdx_code, max(tdx_name) AS tdx_name
              FROM {pg.schema}.tdx_ths_industry_mapping WHERE deleted_at IS NULL GROUP BY ths_code
        """)).mappings().all()
        # eltdx theme_market snapshots are the authoritative TDX constituent
        # source.  Keep the historical snapshot bounded by the review date;
        # THS->TDX mapping remains a fallback only.
        tdx_rows = db.execute(text(f"""
            SELECT DISTINCT ON (code6) code6, name, payload
              FROM {pg.schema}.tdx_sector_quote_detail_snapshot
             WHERE snapshot_date <= :trade_date AND deleted_at IS NULL
             ORDER BY code6, snapshot_date DESC, generated_at DESC
        """), {"trade_date": trade_date}).mappings().all()
    mapping = {str(row["ths_code"]): dict(row) for row in names}
    direct_tdx: dict[str, list[dict[str, str]]] = {}
    for row in tdx_rows:
        industry_code = str(row.get("code6") or "").strip()
        industry_name = str(row.get("name") or industry_code)
        payload = row.get("payload")
        if isinstance(payload, str):
            try:
                payload = json.loads(payload)
            except ValueError:
                payload = {}
        sections = payload.get("sections") if isinstance(payload, dict) else {}
        constituents = sections.get("constituents") if isinstance(sections, dict) else []
        for constituent in constituents if isinstance(constituents, list) else []:
            if not isinstance(constituent, dict):
                continue
            code = _code(constituent.get("code") or constituent.get("stock_code") or constituent.get("stockCode"))
            if code:
                direct_tdx.setdefault(code, []).append({"source": "tdx", "code": industry_code, "name": industry_name})
    for row in ths_rows:
        code = _code(row["stock_code"])
        industry = str(row["industry_code"])
        item = mapping.get(industry, {})
        members = result.setdefault(code, [])
        members.append({"source": "ths", "code": industry, "name": str(item.get("ths_name") or industry)})
        tdx_code = str(item.get("tdx_code") or "").strip()
        # Direct constituents win.  Mapping fills any missing historical TDX
        # relation without overwriting the source snapshot.
        if tdx_code and not any(member["source"] == "tdx" and member["code"] == tdx_code for member in direct_tdx.get(code, [])):
            members.append({"source": "tdx", "code": tdx_code, "name": str(item.get("tdx_name") or tdx_code)})
    for code, tdx_members in direct_tdx.items():
        result.setdefault(code, []).extend(tdx_members)
    for members in result.values():
        # The storage key is (membership, industry_code).  A THS mapping and
        # a TDX snapshot can occasionally carry different display names for
        # the same code; retaining both makes one minute projection violate
        # that durable key. Keep the first source relation deterministically.
        by_source_code: dict[tuple[str, str], dict[str, str]] = {}
        for item in members:
            key = (item["source"], item["code"])
            by_source_code.setdefault(key, item)
        members[:] = [by_source_code[key] for key in sorted(by_source_code)]
        union_by_code: dict[str, str] = {}
        for item in members:
            union_by_code.setdefault(item["code"], item["name"])
        members.extend({"source": "union", "code": code, "name": name} for code, name in sorted(union_by_code.items()))
    return result


def _build_contexts(metadata: dict[str, dict[str, Any]], bars: list[dict[str, Any]], previous_closes: dict[str, float], limit_rows: dict[str, dict[str, Any]], previous_limit_rows: dict[str, dict[str, Any]], memberships: dict[str, list[dict[str, str]]]) -> dict[str, dict[str, Any]]:
    codes = {_code(item.get("symbol")) for item in bars}
    contexts: dict[str, dict[str, Any]] = {}
    for code in codes:
        if not code:
            continue
        meta = metadata.get(code, {})
        pre_close = previous_closes.get(code)
        limit = limit_rows.get(code, {})
        previous = previous_limit_rows.get(code, {})
        if str(meta.get("security_type") or "STOCK").upper() == "STOCK":
            threshold = effective_limit_up_threshold(code, meta)
        else:
            threshold = float(meta.get("limit_up_threshold") or 10.0)
        up = _number(limit.get("limit_up_price")) or (_round_price(pre_close * (1 + _threshold_ratio(threshold))) if pre_close else None)
        down = _number(limit.get("limit_down_price")) or (_round_price(pre_close * (1 - _threshold_ratio(threshold))) if pre_close else None)
        security_type = str(meta.get("security_type") or ("ETF" if _is_etf(code) else "STOCK" if meta else "OTHER"))
        contexts[code] = {
            "code": code, "name": meta.get("name") or limit.get("name") or code, "market": meta.get("market"),
            "security_type": security_type, "pre_close": pre_close,
            "limit_up_price": up, "limit_down_price": down,
            "previous_streak": int(previous.get("streak") or 0) if previous.get("category") == "limit_up" else 0,
            "previous_limit_status": previous.get("category"), "memberships": memberships.get(code, []),
            "source": {"daily": "clickhouse.daily_raw", "membership": "ths+tdx"},
        }
    return contexts


def _attach_daily_industry_factors(pg, trade_date: date, contexts: dict[str, dict[str, Any]], industries: list[dict[str, Any]]) -> None:
    """Attach only as-of-date daily factors; failure is an explicit degradation.

    Factors intentionally live in the daily projection rather than being
    fetched by the replay API, preventing either future-data leakage or a
    provider/API wait during cursor movement.
    """
    if not industries:
        return
    try:
        with pg.session() as db:
            tdx_rows = db.execute(text(f"""
                SELECT DISTINCT ON (code6) code6, flow_trade_date, main_net, large_order_net, medium_order_net, small_order_net
                  FROM {pg.schema}.tdx_industry_fund_flow_snapshot
                 WHERE trade_date <= :trade_date AND deleted_at IS NULL AND snapshot_status = 'ok'
                 ORDER BY code6, trade_date DESC, generated_at DESC
            """), {"trade_date": trade_date}).mappings().all()
            ths_rows = db.execute(text(f"""
                SELECT DISTINCT ON (COALESCE(industry_code, industry)) industry_code, industry, trade_date, net, change_pct
                  FROM {pg.schema}.mkt_ths_industry_fund_flow_daily
                 WHERE trade_date <= :trade_date AND deleted_at IS NULL
                 ORDER BY COALESCE(industry_code, industry), trade_date DESC, ingested_at DESC
            """), {"trade_date": trade_date}).mappings().all()
            margin_rows = db.execute(text(f"""
                SELECT DISTINCT ON (stock_code) stock_code, trade_date, financing_balance, financing_buy_amount, margin_balance
                  FROM {pg.schema}.margin_stock_detail
                 WHERE trade_date <= :trade_date AND deleted_at IS NULL
                 ORDER BY stock_code, trade_date DESC
            """), {"trade_date": trade_date}).mappings().all()
    except Exception:  # An optional daily source must not prevent raw 1m replay materialization.
        for industry in industries:
            industry["factor"] = {
                "status": "partial",
                "fundFlow": _factor_value(None, None, "industry_fund_flow", False),
                "margin": _factor_value(None, None, "margin_stock_detail", False),
                "financialSummary": _factor_value(None, None, "industry_financial_summary", False),
                "valuationSummary": _factor_value(None, None, "industry_valuation_summary", False),
                "missing": ["fund_flow_or_margin"],
            }
        return

    tdx_by_code = {str(row.get("code6") or ""): dict(row) for row in tdx_rows}
    ths_by_code = {str(row.get("industry_code") or row.get("industry") or ""): dict(row) for row in ths_rows}
    ths_by_name = {str(row.get("industry") or ""): dict(row) for row in ths_rows}
    margin_by_code = {_code(row.get("stock_code")): dict(row) for row in margin_rows}
    grouped_margin: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for context in contexts.values():
        margin = margin_by_code.get(context["code"])
        if not margin:
            continue
        for membership in context.get("memberships") or []:
            if membership.get("source") in {"ths", "tdx", "union"}:
                grouped_margin.setdefault((str(membership["source"]), str(membership["code"])), []).append(margin)
    for industry in industries:
        membership = str(industry.get("membership") or "union")
        code = str(industry.get("industry_code") or "")
        name = str(industry.get("industry_name") or "")
        flow = tdx_by_code.get(code) if membership == "tdx" else ths_by_code.get(code) or ths_by_name.get(name)
        margins = grouped_margin.get((membership, code), [])
        margin_dates = [row.get("trade_date") for row in margins if row.get("trade_date")]
        factor: dict[str, Any] = {
            "status": "ready" if flow or margins else "partial",
            "fundFlow": _factor_value(None, None, "tdx_industry_fund_flow_snapshot" if membership == "tdx" else "mkt_ths_industry_fund_flow_daily", False),
            "margin": _factor_value(None, None, "margin_stock_detail", False),
            # These are deliberately explicit until an as-of industry
            # aggregation is materialized.  The replay endpoint must never
            # call F10/valuation providers while a user drags a K-line.
            "financialSummary": _factor_value(None, None, "industry_financial_summary", False),
            "valuationSummary": _factor_value(None, None, "industry_valuation_summary", False),
        }
        if flow:
            factor["fundFlow"] = _factor_value({
                "net": _number(flow.get("main_net") if membership == "tdx" else flow.get("net")),
                "largeOrderNet": _number(flow.get("large_order_net")),
                "changePct": _number(flow.get("change_pct")),
            }, flow.get("flow_trade_date") or flow.get("trade_date"),
            "tdx_industry_fund_flow_snapshot" if membership == "tdx" else "mkt_ths_industry_fund_flow_daily", True)
        if margins:
            factor["margin"] = _factor_value({
                "stockCount": len(margins),
                "financingBalance": sum(_number(row.get("financing_balance")) or 0.0 for row in margins),
                "financingBuyAmount": sum(_number(row.get("financing_buy_amount")) or 0.0 for row in margins),
                "marginBalance": sum(_number(row.get("margin_balance")) or 0.0 for row in margins),
            }, max(margin_dates) if margin_dates else None, "margin_stock_detail", True)
        industry["factor"] = factor


def _factor_value(value: Any, source_date: Any, source_type: str, available: bool) -> dict[str, Any]:
    """Uniform as-of factor envelope consumed by daily replay clients."""
    return {
        "value": value,
        "sourceDate": str(source_date) if source_date else None,
        "sourceType": source_type,
        "available": bool(available),
    }


def _load_indicator_events(pg, trade_date: date) -> list[dict[str, Any]]:
    with pg.session() as db:
        rows = db.execute(text(f"""
            SELECT code, name, event_type, event_at, last_price, change_pct, source, payload
              FROM {pg.schema}.market_indicator_events
             WHERE trade_date = :trade_date AND deleted_at IS NULL
             ORDER BY event_at, code
        """), {"trade_date": trade_date}).mappings().all()
    return [dict(row) for row in rows]


def _publish_cache(
    context: PipelineContext, trade_date: date, daily_market: dict[str, Any], daily_industries: list[dict[str, Any]],
    phase: dict[str, Any], projections: list[Any], daily_pools: list[dict[str, Any]], events: list[dict[str, Any]],
    history: list[dict[str, Any]], industry_phases: list[dict[str, Any]], manifest: dict[str, Any], request: JobRequest,
) -> bool:
    if not truthy(request.params.get("redis_cache", True)):
        return False
    try:
        client = context.hooks.redis.get_binary_client()
        prefix = str(request.params.get("redis_prefix") or "cynexus:trade-review:market-state")
        ttl = parse_int(request.params.get("redis_ttl_seconds"), 604800)
        day_key = f"{prefix}:day:{trade_date.isoformat()}"
        # Do not publish a monolithic all-industry/all-minute JSON document.
        # It was 128 MiB for a single complete trading day, forcing the Java
        # read path to deserialize a full day for every replay cursor move.
        # The immutable cache is instead a tiny day index plus a compressed
        # cursor-minute document.  The base/index is written last, making a
        # partially written publish a safe cache miss rather than a partial
        # replay response.
        minute_keys = [item.minute_at.isoformat() for item in projections]
        event_index: dict[str, list[dict[str, Any]]] = {}
        for event in events:
            event_at = event.get("event_at")
            if not isinstance(event_at, datetime):
                continue
            minute_key = event_at.replace(second=0, microsecond=0).isoformat()
            event_index.setdefault(minute_key, []).append(_compact_cache_event(event))
        for item in projections:
            minute_at = item.minute_at.isoformat()
            client.setex(
                f"{day_key}:minute:{_cache_minute_token(item.minute_at)}", ttl,
                _cache_json({
                    "minuteAt": minute_at, "market": item.market,
                    "industries": item.industries, "pools": item.pools,
                }),
            )
        client.setex(f"{day_key}:events", ttl, _cache_json({"events": event_index}))
        payload = {
            "cacheVersion": 2,
            "tradeDate": trade_date.isoformat(), "manifest": manifest, "daily": daily_market,
            "dailyIndustries": daily_industries, "phase": phase, "history": history,
            "industryPhases": industry_phases,
            "minuteKeys": minute_keys,
            "dailyPools": daily_pools,
        }
        client.setex(day_key, ttl, _cache_json(payload))
        return True
    except Exception:  # noqa: BLE001 - durable PostgreSQL projections remain serving truth.
        return False


def _compact_cache_event(event: dict[str, Any]) -> dict[str, Any]:
    """Keep cursor-event UI fields, not repeated raw payload/membership blobs."""
    return {
        "code": event.get("code"), "name": event.get("name"), "event_type": event.get("event_type"),
        "event_at": event.get("event_at"), "effective_minute": event.get("effective_minute"),
        "price": event.get("price"), "change_pct": event.get("change_pct"),
        "limit_up_price": event.get("limit_up_price"), "limit_down_price": event.get("limit_down_price"),
        "source": event.get("source"),
    }


def _cache_minute_token(value: datetime) -> str:
    return value.strftime("%H%M")


def _cache_json(value: Any) -> bytes:
    raw = json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":")).encode("utf-8")
    return b"gzip:" + base64.b64encode(gzip.compress(raw, compresslevel=6))


def _code(value: Any) -> str:
    digits = "".join(char for char in str(value or "") if char.isdigit())
    return digits[-6:].zfill(6) if digits else ""


def _number(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _is_etf(code: str) -> bool:
    return code.startswith(("15", "16", "18", "50", "51", "52", "56", "58"))


def _classify_industry_phases(history: list[dict[str, Any]], current: list[dict[str, Any]], *, window_days: int = 60) -> list[dict[str, Any]]:
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in history:
        grouped.setdefault((str(row.get("membership") or "union"), str(row.get("industry_code") or "")), []).append(row)
    output: list[dict[str, Any]] = []
    for item in current:
        membership = str(item.get("membership") or "union")
        code = str(item.get("industry_code") or "")
        series = [*grouped.get((membership, code), []), item]
        phase = classify_phase(series, window_days=window_days)
        factor = dict(phase.get("factor") or {})
        factor["relativeChangePct"] = _number(item.get("relative_change_pct"))
        factor["dailyFactor"] = item.get("factor") or {}
        output.append({"membership": membership, "industry_code": code, **phase, "factor": factor})
    return output
