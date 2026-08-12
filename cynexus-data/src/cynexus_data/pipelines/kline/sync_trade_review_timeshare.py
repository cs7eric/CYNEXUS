from __future__ import annotations

from datetime import date, datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any
from zoneinfo import ZoneInfo

import pandas as pd
from sqlalchemy import text

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.tdx.easy_tdx import fetch_history_timeshare_rows
from cynexus_data.storage.clickhouse.client import ClickHouseClient
from cynexus_data.storage.clickhouse.writers import ClickHouseTradeReviewTimeshareSink
from cynexus_data.storage.redis.trade_review_timeshare import cache_key, publish
from cynexus_data.storage.postgres.ids import next_bigint_id


SHANGHAI_TZ = ZoneInfo("Asia/Shanghai")


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    params = request.params or {}
    trade_date = _parse_date(params.get("trade_date"))
    targets = _resolve_targets(context, trade_date, params)
    result = {
        "pipeline": "kline.sync_trade_review_timeshare",
        "trade_date": trade_date.isoformat(),
        "planned_targets": len(targets),
        "rows_read": 0,
        "rows_written": 0,
        "cache_published": 0,
        "failed": 0,
        "skipped_cached": 0,
        "errors": [],
        "dry_run": request.dry_run,
    }
    if request.dry_run:
        result["targets"] = [target for target in targets[:100]]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    sink = ClickHouseTradeReviewTimeshareSink(context.hooks.clickhouse.get_config())
    sink.create_table()
    ch_client = ClickHouseClient(context.hooks.clickhouse.get_config())
    redis_client = None
    if _truthy(params.get("redis_cache", True)):
        try:
            redis_client = context.hooks.redis.get_binary_client()
        except Exception as exc:  # noqa: BLE001
            result["errors"].append({"scope": "redis", "error": f"{type(exc).__name__}: {exc}"})

    if redis_client is not None:
        targets, skipped_cached = _exclude_cached_targets(redis_client, trade_date, targets)
        result["skipped_cached"] = skipped_cached
        result["planned_targets"] = len(targets)

    batch_id = _create_batch(context, trade_date, len(targets), params)
    if targets:
        sink.delete_targets(trade_date, targets)
    workers = max(1, min(int(params.get("workers") or 8), 32))
    result["workers"] = workers

    # Network-bound TDX history calls dominate this job. Fetch and enrich in
    # parallel, while keeping ClickHouse replacement and archive bookkeeping
    # serialized for deterministic writes and mutation ordering.
    def fetch_target(target):
        rows = fetch_history_timeshare_rows(
            target.get("provider_symbol") or target["symbol"],
            trade_date,
            timeout=float(params.get("timeout_seconds") or 15),
        )
        # The TDX history-timeshare payload is the source of truth for the
        # replay cache.  Do not issue a per-symbol ClickHouse 1m lookup here:
        # that turns the archive into an N+1 query workload and serializes the
        # hot path.  The payload already carries volume and, when available,
        # avg_price/turnover; detailed 1m enrichment remains an on-demand
        # chart concern.
        rows = _enrich_rows(rows)
        frame = _to_frame(rows, target, trade_date)
        if frame.empty:
            raise ValueError("provider returned no canonical session points")
        return frame

    with ThreadPoolExecutor(max_workers=workers, thread_name_prefix="timeshare") as executor:
        futures = {executor.submit(fetch_target, target): target for target in targets}
        for future in as_completed(futures):
            target = futures[future]
            try:
                frame = future.result()
                written = sink.replace(frame, delete_existing=False)
                result["rows_read"] += len(frame)
                result["rows_written"] += written
                if redis_client is not None:
                    publish(redis_client, trade_date, target["target_type"], target["market"], target["symbol"], frame.to_dict("records"), int(params.get("redis_ttl_seconds") or 604800))
                    result["cache_published"] += 1
                _update_target(context, batch_id, trade_date, target, status="READY", point_count=len(frame), error=None)
            except Exception as exc:  # noqa: BLE001
                result["failed"] += 1
                error = f"{type(exc).__name__}: {exc}"
                result["errors"].append({"symbol": target["symbol"], "error": error})
                _update_target(context, batch_id, trade_date, target, status="FAILED", point_count=0, error=error)
                if not _truthy(params.get("ignore_errors", True)):
                    for pending in futures:
                        pending.cancel()
                    raise
    _finish_batch(context, batch_id, result)
    result["errors"] = result["errors"][:50]
    return result


def _resolve_targets(context: PipelineContext, trade_date: date, params: dict[str, Any]) -> list[dict[str, str]]:
    explicit = params.get("codes") or params.get("symbols")
    codes = _normalize_codes(explicit)
    targets: dict[tuple[str, str, str], dict[str, str]] = {}
    if codes:
        for code in codes:
            market = _market(code, "STOCK")
            targets[("STOCK", market, code)] = {"target_type": "STOCK", "market": market, "symbol": code, "name": code}
    for code in _normalize_codes(params.get("etf_codes")):
        market = _market(code, "ETF")
        targets[("ETF", market, code)] = {"target_type": "ETF", "market": market, "symbol": code, "name": code}
    if not codes and not _normalize_codes(params.get("etf_codes")):
        pg = context.hooks.postgres.get_client()
        with pg.session() as db:
            limit_rows = db.execute(text(f"""
                SELECT code, name, category FROM {pg.schema}.market_limit_daily_stocks
                 WHERE trade_date = :trade_date AND deleted_at IS NULL
                   AND category IN ('limit_up', 'limit_down', 'broken')
            """), {"trade_date": trade_date}).mappings().all()
            for row in limit_rows:
                code = _code(row["code"])
                targets[("STOCK", _market(code, "STOCK"), code)] = {"target_type": "STOCK", "market": _market(code, "STOCK"), "symbol": code, "name": row.get("name") or code}
            feedback_rows = db.execute(text(f"""
                SELECT DISTINCT ON (code) code, name
                  FROM {pg.schema}.market_limit_daily_stocks
                 WHERE trade_date < :trade_date AND deleted_at IS NULL
                   AND category IN ('limit_up', 'limit_down')
                 ORDER BY code, trade_date DESC
            """), {"trade_date": trade_date}).mappings().all()
            for row in feedback_rows:
                code = _code(row["code"])
                if code:
                    targets[("STOCK", _market(code, "STOCK"), code)] = {"target_type": "STOCK", "market": _market(code, "STOCK"), "symbol": code, "name": row.get("name") or code}
            # Stage 4's durable daily pools are the authoritative selection
            # for re-seals, broken boards and prior-day feedback.  Keep the
            # old limit snapshot query above as a compatibility fallback for
            # dates materialized before the market-state job existed.
            try:
                pool_rows = db.execute(text(f"""
                    SELECT pool_type, item->>'code' AS code, item->>'name' AS name
                      FROM {pg.schema}.trade_review_daily_pool,
                           LATERAL jsonb_array_elements(stocks) AS item
                     WHERE trade_date = :trade_date
                       AND pool_type IN ('limit_up', 'limit_down', 'broken', 'reseal',
                                         'yesterday_limit_up_feedback', 'yesterday_limit_down_feedback')
                """), {"trade_date": trade_date}).mappings().all()
                for row in pool_rows:
                    code = _code(row.get("code"))
                    if code:
                        targets[("STOCK", _market(code, "STOCK"), code)] = {
                            "target_type": "STOCK", "market": _market(code, "STOCK"),
                            "symbol": code, "name": row.get("name") or code,
                        }
            except Exception:
                # The archive must still run while a historical backfill is
                # waiting for the materialized read-model schema.
                pass
            trade_rows = db.execute(text(f"""
                SELECT DISTINCT symbol, max(security_name) AS name
                  FROM {pg.schema}.portfolio_trade
                 WHERE traded_at::date = :trade_date AND deleted_at IS NULL AND status = 'ACTIVE'
                 GROUP BY symbol
            """), {"trade_date": trade_date}).mappings().all()
            for row in trade_rows:
                code = _code(row["symbol"])
                if code:
                    targets[("STOCK", _market(code, "STOCK"), code)] = {"target_type": "STOCK", "market": _market(code, "STOCK"), "symbol": code, "name": row.get("name") or code}
            industry_rows = db.execute(text(f"""
                SELECT industry_code, stock_code, stock_name, change_pct
                  FROM {pg.schema}.ths_industry_constituent_snapshot
                 WHERE snapshot_date = :trade_date AND deleted_at IS NULL
            """), {"trade_date": trade_date}).mappings().all()
            limits = {str(row["code"]).strip()[-6:].zfill(6) for row in limit_rows}
            grouped: dict[str, list[dict[str, Any]]] = {}
            for row in industry_rows:
                grouped.setdefault(str(row["industry_code"]), []).append(dict(row))
            for rows in grouped.values():
                ordered = sorted(rows, key=lambda row: float(row.get("change_pct") or 0))
                selected = ordered[:10] + list(reversed(ordered[-15:]))
                for row in selected:
                    code = _code(row.get("stock_code"))
                    if not code or code in limits:
                        continue
                    targets[("STOCK", _market(code, "STOCK"), code)] = {"target_type": "STOCK", "market": _market(code, "STOCK"), "symbol": code, "name": row.get("stock_name") or code}
        for code in _normalize_codes(params.get("context_symbols") or "000001,399001,399006"):
            market = _market(code, "INDEX")
            targets[("INDEX", market, code)] = {"target_type": "INDEX", "market": market, "symbol": code, "name": code}
        # Index payloads are requested with their exchange prefix, but stored
        # and cached by code6 so the Panel's TDX industry relation can look
        # them up with the same stable key used by the materialized state.
        try:
            index_rows = db.execute(text(f"""
                SELECT DISTINCT ON (code6) code6, name, full_code, exchange
                  FROM {pg.schema}.tdx_industry_quote_snapshot
                 WHERE snapshot_date <= :trade_date AND deleted_at IS NULL
                 ORDER BY code6, snapshot_date DESC, fetched_at DESC
            """), {"trade_date": trade_date}).mappings().all()
            for row in index_rows:
                code = _code(row.get("code6"))
                provider_symbol = str(row.get("full_code") or "").strip().lower()
                exchange = str(row.get("exchange") or "").strip().upper()
                if not code or not provider_symbol or exchange not in {"SH", "SZ", "BJ"}:
                    continue
                targets[("INDEX", exchange, code)] = {
                    "target_type": "INDEX", "market": exchange, "symbol": code,
                    "provider_symbol": provider_symbol, "name": row.get("name") or code,
                }
        except Exception:
            # Industry index history is an enhancement; source availability
            # never blocks stock/market replay publication.
            pass
    return list(targets.values())


def _enrich_rows(rows):
    normalized = []
    for row in rows:
        item = dict(row)
        item["slot"] = _slot(item.get("trade_time"))
        if item["slot"] is not None:
            normalized.append(item)
    return normalized


def _exclude_cached_targets(redis_client, trade_date: date, targets: list[dict[str, str]]) -> tuple[list[dict[str, str]], int]:
    """Skip targets whose complete replay series is already cached for the day."""
    remaining: list[dict[str, str]] = []
    skipped = 0
    chunk_size = 500
    for start in range(0, len(targets), chunk_size):
        chunk = targets[start : start + chunk_size]
        keys = [cache_key(trade_date, item["target_type"], item["market"], item["symbol"]) for item in chunk]
        cached = redis_client.mget(keys)
        for item, payload in zip(chunk, cached, strict=True):
            if payload:
                skipped += 1
            else:
                remaining.append(item)
    return remaining, skipped


def _to_frame(rows, target, trade_date: date) -> pd.DataFrame:
    # ClickHouse DateTime columns are explicitly Asia/Shanghai.  The executor
    # containers run in UTC, so naive Python datetimes would otherwise be
    # interpreted as UTC and shift every replay point by +8 hours on read.
    now = datetime.now(SHANGHAI_TZ)
    output = []
    for row in rows:
        output.append({
            "trade_date": trade_date,
            "target_type": target["target_type"],
            "market": target["market"],
            "symbol": target["symbol"],
            "tdx_code": row.get("tdx_code") or f"{target['market'].lower()}{target['symbol']}",
            "slot": int(row["slot"]),
            "trade_time": _as_shanghai(row["trade_time"]),
            "price": row["price"],
            "avg_price": row.get("avg_price"),
            "volume_lots": int(float(row.get("volume") or 0)),
            "turnover": row.get("turnover"),
            "price_source": "easy_tdx.history_timeshare",
            "amount_source": "tdx_local.1m" if row.get("turnover") else "unavailable",
            "source_version": "trade-review-timeshare-v1",
            "fetched_at": now,
            "ingested_at": now,
        })
    return pd.DataFrame(output)


def _as_shanghai(value: Any) -> datetime | None:
    timestamp = pd.to_datetime(value, errors="coerce")
    if pd.isna(timestamp):
        return None
    resolved = timestamp.to_pydatetime() if hasattr(timestamp, "to_pydatetime") else timestamp
    if resolved.tzinfo is None:
        return resolved.replace(tzinfo=SHANGHAI_TZ)
    return resolved.astimezone(SHANGHAI_TZ)


def _slot(value, minute_end: bool = False) -> int | None:
    if value is None:
        return None
    timestamp = pd.to_datetime(value, errors="coerce")
    if pd.isna(timestamp):
        return None
    timestamp = timestamp.to_pydatetime().replace(second=0, microsecond=0)
    if minute_end:
        timestamp -= timedelta(minutes=1)
    minutes = timestamp.hour * 60 + timestamp.minute
    if 9 * 60 + 30 <= minutes < 11 * 60 + 30:
        return minutes - (9 * 60 + 30)
    if 13 * 60 <= minutes < 15 * 60:
        return 120 + minutes - 13 * 60
    return None


def _create_batch(context, trade_date: date, count: int, params: dict[str, Any]) -> int:
    pg = context.hooks.postgres.get_client()
    with pg.session() as db:
        row = db.execute(text(f"""
            INSERT INTO {pg.schema}.timeshare_archive_batch
                (id, trade_date, status, selection_version, config_snapshot, planned_target_count, created_at, updated_at)
            VALUES (:id, :trade_date, 'RUNNING', 'v1', CAST(:config AS jsonb), :count, now(), now())
            ON CONFLICT (trade_date, selection_version) DO UPDATE SET
              status = 'RUNNING', planned_target_count = EXCLUDED.planned_target_count,
              started_at = now(), completed_at = NULL, updated_at = now()
            RETURNING id
        """), {"id": next_bigint_id(), "trade_date": trade_date, "config": "{}", "count": count}).scalar_one()
    return int(row)


def _update_target(context, batch_id: int, trade_date: date, target: dict[str, str], *, status: str, point_count: int, error: str | None) -> None:
    pg = context.hooks.postgres.get_client()
    with pg.session() as db:
        db.execute(text(f"""
            INSERT INTO {pg.schema}.timeshare_archive_target
              (id, batch_id, trade_date, target_type, market, symbol, canonical_symbol, name, status, point_count, expected_point_count, last_error_message, updated_at)
            VALUES (:id, :batch_id, :trade_date, :target_type, :market, :symbol, :symbol, :name, :status, :point_count, 240, :error, now())
            ON CONFLICT (batch_id, target_type, market, canonical_symbol) DO UPDATE SET
              status = EXCLUDED.status, point_count = EXCLUDED.point_count, last_error_message = EXCLUDED.last_error_message, updated_at = now()
        """), {"id": next_bigint_id(), "batch_id": batch_id, "trade_date": trade_date, **target, "status": status, "point_count": point_count, "error": error})


def _finish_batch(context, batch_id: int, result: dict[str, Any]) -> None:
    pg = context.hooks.postgres.get_client()
    status = "FAILED" if result["failed"] and not result["rows_written"] else "PARTIAL" if result["failed"] else "SUCCESS"
    with pg.session() as db:
        db.execute(text(f"""
            UPDATE {pg.schema}.timeshare_archive_batch
               SET status = :status, ready_target_count = :ready, failed_target_count = :failed,
                   cache_published_count = :cached, completed_at = now(), updated_at = now()
             WHERE id = :id
        """), {"id": batch_id, "status": status, "ready": result["planned_targets"] - result["failed"], "failed": result["failed"], "cached": result["cache_published"]})


def _normalize_codes(value) -> list[str]:
    if value in (None, "", []):
        return []
    values = value.split(",") if isinstance(value, str) else value
    return [_code(item) for item in values if _code(item)]


def _code(value) -> str:
    digits = "".join(ch for ch in str(value or "") if ch.isdigit())
    return digits[-6:].zfill(6) if digits else ""


def _market(code: str, target_type: str = "STOCK") -> str:
    if target_type == "INDEX" and code == "000001":
        return "SH"
    if code.startswith(("4", "8", "920")):
        return "BJ"
    return "SH" if code.startswith(("5", "6", "9")) else "SZ"


def _parse_date(value) -> date:
    if value:
        return date.fromisoformat(str(value)[:10].replace("/", "-"))
    return date.today()


def _truthy(value) -> bool:
    if isinstance(value, bool):
        return value
    return str(value or "").lower() in {"1", "true", "yes", "y", "on"}
