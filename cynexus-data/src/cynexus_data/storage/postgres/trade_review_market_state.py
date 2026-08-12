from __future__ import annotations

import json
from datetime import date, datetime
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260811_create_trade_review_market_state.sql"


class TradeReviewMarketStateRepository:
    """Durable projections used by the Trade Review read API.

    Replacement is intentionally scoped to a single trading day.  This keeps
    post-close repair/backfill idempotent and leaves raw market data untouched.
    """

    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_tables(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_contexts(self, trade_date: date, contexts: dict[str, dict[str, Any]]) -> int:
        rows = []
        for code, value in contexts.items():
            rows.append({
                "trade_date": trade_date, "code": code, "name": value.get("name"), "market": value.get("market"),
                "security_type": value.get("security_type") or "STOCK", "pre_close": value.get("pre_close"),
                "limit_up_price": value.get("limit_up_price"), "limit_down_price": value.get("limit_down_price"),
                "previous_streak": int(value.get("previous_streak") or 0), "previous_limit_status": value.get("previous_limit_status"),
                "memberships": _json(value.get("memberships") or []), "source": _json(value.get("source") or {}),
            })
        with self.client.session() as db:
            db.execute(text(f"DELETE FROM {self.client.schema}.trade_review_security_day_context WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            if rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_security_day_context
                    (trade_date, code, name, market, security_type, pre_close, limit_up_price, limit_down_price,
                     previous_streak, previous_limit_status, memberships, source, updated_at)
                    VALUES (:trade_date, :code, :name, :market, :security_type, :pre_close, :limit_up_price, :limit_down_price,
                            :previous_streak, :previous_limit_status, CAST(:memberships AS jsonb), CAST(:source AS jsonb), now())
                """), rows)
        return len(rows)

    def replace_security_daily_facts(self, trade_date: date, facts: list[dict[str, Any]]) -> int:
        """Atomically replace the immutable post-close security fact set."""
        rows = [{
            "trade_date": trade_date, **fact,
            "memberships": _json(fact.get("memberships") or []),
        } for fact in facts]
        with self.client.session() as db:
            db.execute(text(f"DELETE FROM {self.client.schema}.trade_review_security_daily_fact WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            if rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_security_daily_fact
                    (trade_date, code, name, security_type, open, high, low, close, change_pct, amplitude, volume, amount,
                     touch_limit_up, touch_limit_down, final_limit_up, final_limit_down, first_limit_up_at, first_limit_down_at,
                     open_count, reseal_count, previous_streak, current_streak, is_advanced, is_broken_streak,
                     previous_limit_status, intraday_available, is_one_word_limit, event_evidence_quality, memberships, updated_at)
                    VALUES (:trade_date, :code, :name, :security_type, :open, :high, :low, :close, :change_pct, :amplitude, :volume, :amount,
                            :touch_limit_up, :touch_limit_down, :final_limit_up, :final_limit_down, :first_limit_up_at, :first_limit_down_at,
                            :open_count, :reseal_count, :previous_streak, :current_streak, :is_advanced, :is_broken_streak,
                            :previous_limit_status, :intraday_available, :is_one_word_limit, :event_evidence_quality,
                            CAST(:memberships AS jsonb), now())
                """), rows)
        return len(rows)

    def replace_events_and_intervals(self, trade_date: date, events: list[dict[str, Any]], intervals: list[dict[str, Any]]) -> tuple[int, int]:
        event_rows = [{
            "id": stable_bigint_id("trade_review_indicator_event", trade_date, item["code"], item["event_type"], item["event_at"]),
            "trade_date": trade_date, **item, "payload": _json(item.get("payload") or {}),
        } for item in events]
        interval_rows = [{
            "id": stable_bigint_id("trade_review_limit_interval", trade_date, item["code"], item["state"], item["started_at"]),
            "trade_date": trade_date, **item, "source": "market_indicator", "payload": "{}",
        } for item in intervals]
        event_index: dict[Any, list[dict[str, Any]]] = {}
        for item in events:
            minute = item.get("effective_minute")
            if minute is None:
                continue
            event_index.setdefault(minute, []).append({
                "code": item.get("code"), "name": item.get("name"), "eventType": item.get("event_type"),
                "eventAt": item.get("event_at"), "price": item.get("price"), "changePct": item.get("change_pct"),
                "limitUpPrice": item.get("limit_up_price"), "limitDownPrice": item.get("limit_down_price"),
                "memberships": item.get("memberships") or [], "source": item.get("source"),
            })
        index_rows = [{"trade_date": trade_date, "minute_at": minute, "events": _json(rows)} for minute, rows in event_index.items()]
        with self.client.session() as db:
            db.execute(text(f"DELETE FROM {self.client.schema}.trade_review_indicator_event WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            db.execute(text(f"DELETE FROM {self.client.schema}.trade_review_limit_state_interval WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            db.execute(text(f"DELETE FROM {self.client.schema}.trade_review_minute_event_index WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            if event_rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_indicator_event
                    (id, trade_date, code, name, event_type, event_at, effective_minute, price, change_pct,
                     limit_up_price, limit_down_price, source, payload, updated_at)
                    VALUES (:id, :trade_date, :code, :name, :event_type, :event_at, :effective_minute, :price, :change_pct,
                            :limit_up_price, :limit_down_price, :source, CAST(:payload AS jsonb), now())
                """), event_rows)
            if interval_rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_limit_state_interval
                    (id, trade_date, code, state, started_at, ended_at, event_count, source, payload, updated_at)
                    VALUES (:id, :trade_date, :code, :state, :started_at, :ended_at, :event_count, :source, CAST(:payload AS jsonb), now())
                """), interval_rows)
            if index_rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_minute_event_index
                    (trade_date, minute_at, events, updated_at)
                    VALUES (:trade_date, :minute_at, CAST(:events AS jsonb), now())
                """), index_rows)
        return len(event_rows), len(interval_rows)

    def replace_minute_states(self, trade_date: date, projections: list[Any]) -> tuple[int, int, int]:
        markets = []
        industries = []
        pools = []
        for item in projections:
            market = dict(item.market)
            markets.append({"trade_date": trade_date, **market, "ladder": _json(market.get("ladder") or [])})
            for industry in item.industries:
                industries.append({"trade_date": trade_date, **industry, "ladder": _json(industry.get("ladder") or []), "core_pool": _json(industry.get("core_pool") or [])})
            for pool in item.pools:
                pools.append({"trade_date": trade_date, **pool, "stocks": _json(pool.get("stocks") or [])})
        with self.client.session() as db:
            for table in ("trade_review_market_minute_state", "trade_review_industry_minute_state", "trade_review_minute_pool_index"):
                db.execute(text(f"DELETE FROM {self.client.schema}.{table} WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            if markets:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_market_minute_state
                    (trade_date, minute_at, advancing_count, declining_count, flat_count, above_5_count, below_minus_5_count,
                     touched_limit_up_count, sealed_limit_up_count, broken_limit_up_count, sealed_limit_down_count, max_height,
                     seal_rate, break_rate, ladder, source_confidence, updated_at)
                    VALUES (:trade_date, :minute_at, :advancing_count, :declining_count, :flat_count, :above_5_count, :below_minus_5_count,
                            :touched_limit_up_count, :sealed_limit_up_count, :broken_limit_up_count, :sealed_limit_down_count, :max_height,
                            :seal_rate, :break_rate, CAST(:ladder AS jsonb), :source_confidence, now())
                """), markets)
            if industries:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_industry_minute_state
                    (trade_date, minute_at, membership, industry_code, industry_name, stock_count, advancing_count, declining_count,
                     flat_count, touched_limit_up_count, sealed_limit_up_count, broken_limit_up_count, sealed_limit_down_count,
                     max_height, seal_rate, break_rate, relative_change_pct, ladder, core_pool, updated_at)
                    VALUES (:trade_date, :minute_at, :membership, :industry_code, :industry_name, :stock_count, :advancing_count, :declining_count,
                            :flat_count, :touched_limit_up_count, :sealed_limit_up_count, :broken_limit_up_count, :sealed_limit_down_count,
                            :max_height, :seal_rate, :break_rate, :relative_change_pct, CAST(:ladder AS jsonb), CAST(:core_pool AS jsonb), now())
                """), industries)
            if pools:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_minute_pool_index
                    (trade_date, minute_at, pool_type, membership, industry_code, stocks, updated_at)
                    VALUES (:trade_date, :minute_at, :pool_type, :membership, :industry_code, CAST(:stocks AS jsonb), now())
                """), pools)
        return len(markets), len(industries), len(pools)

    def replace_daily(self, trade_date: date, market: dict[str, Any], industries: list[dict[str, Any]], pools: list[dict[str, Any]], phase: dict[str, Any], window_days: int,
                      industry_phases: list[dict[str, Any]] | None = None) -> tuple[int, int, int]:
        with self.client.session() as db:
            for table in ("trade_review_market_daily_state", "trade_review_industry_daily_state", "trade_review_daily_pool", "trade_review_trend_phase"):
                db.execute(text(f"DELETE FROM {self.client.schema}.{table} WHERE trade_date=:trade_date"), {"trade_date": trade_date})
            if market:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_market_daily_state
                    (trade_date, advancing_count, declining_count, flat_count, above_5_count, below_minus_5_count,
                     touched_limit_up_count, sealed_limit_up_count, broken_limit_up_count, sealed_limit_down_count, max_height,
                     seal_rate, break_rate, promotion_rate, break_streak_rate, ladder, source, updated_at)
                    VALUES (:trade_date, :advancing_count, :declining_count, :flat_count, :above_5_count, :below_minus_5_count,
                            :touched_limit_up_count, :sealed_limit_up_count, :broken_limit_up_count, :sealed_limit_down_count, :max_height,
                            :seal_rate, :break_rate, :promotion_rate, :break_streak_rate, CAST(:ladder AS jsonb), CAST(:source AS jsonb), now())
                """), {"trade_date": trade_date, **market, "ladder": _json(market.get("ladder") or []), "source": _json(market.get("source") or {})})
            if industries:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_industry_daily_state
                    (trade_date, membership, industry_code, industry_name, stock_count, advancing_count, declining_count,
                     touched_limit_up_count, sealed_limit_up_count, broken_limit_up_count, sealed_limit_down_count, max_height,
                     seal_rate, break_rate, relative_change_pct, factor, updated_at)
                    VALUES (:trade_date, :membership, :industry_code, :industry_name, :stock_count, :advancing_count, :declining_count,
                            :touched_limit_up_count, :sealed_limit_up_count, :broken_limit_up_count, :sealed_limit_down_count, :max_height,
                            :seal_rate, :break_rate, :relative_change_pct, CAST(:factor AS jsonb), now())
                """), [{"trade_date": trade_date, **row, "factor": _json(row.get("factor") or {})} for row in industries])
            if pools:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_daily_pool
                    (trade_date, pool_type, membership, industry_code, stocks, updated_at)
                    VALUES (:trade_date, :pool_type, :membership, :industry_code, CAST(:stocks AS jsonb), now())
                """), [{"trade_date": trade_date, **row, "stocks": _json(row.get("stocks") or [])} for row in pools])
            db.execute(text(f"""
                INSERT INTO {self.client.schema}.trade_review_trend_phase
                (trade_date, scope, membership, industry_code, window_days, phase, phase_score, factor, policy_version, updated_at)
                VALUES (:trade_date, 'market', 'union', '', :window_days, :phase, :phase_score, CAST(:factor AS jsonb), :policy_version, now())
            """), {"trade_date": trade_date, "window_days": window_days, "phase": phase["phase"], "phase_score": phase["phase_score"],
                  "factor": _json(phase.get("factor") or {}), "policy_version": phase.get("policyVersion") or "daily-v2"})
            if industry_phases:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.trade_review_trend_phase
                    (trade_date, scope, membership, industry_code, window_days, phase, phase_score, factor, policy_version, updated_at)
                    VALUES (:trade_date, 'industry', :membership, :industry_code, :window_days, :phase, :phase_score,
                            CAST(:factor AS jsonb), :policy_version, now())
                """), [{"trade_date": trade_date, "window_days": window_days, "membership": item["membership"],
                            "industry_code": item["industry_code"], "phase": item["phase"], "phase_score": item["phase_score"],
                            "factor": _json(item.get("factor") or {}),
                            "policy_version": item.get("policyVersion") or "daily-v2"} for item in industry_phases])
        return int(bool(market)), len(industries), len(pools)

    def update_manifest(self, trade_date: date, **values: Any) -> None:
        # `values` can contain a native dict from the pipeline.  Build it
        # first and serialize last so the JSON string is not overwritten by
        # the original mapping during dictionary expansion.
        payload = {"trade_date": trade_date, **values}
        payload["quality"] = _json(values.get("quality") or {})
        with self.client.session() as db:
            db.execute(text(f"""
                INSERT INTO {self.client.schema}.trade_review_day_manifest
                (trade_date, status, minute_bar_count, minute_symbol_count, context_count, indicator_event_count,
                 market_minute_count, industry_minute_count, daily_pool_count, cache_published, quality, error_message, built_at, updated_at)
                VALUES (:trade_date, :status, :minute_bar_count, :minute_symbol_count, :context_count, :indicator_event_count,
                        :market_minute_count, :industry_minute_count, :daily_pool_count, :cache_published,
                        CAST(:quality AS jsonb), :error_message, now(), now())
                ON CONFLICT (trade_date) DO UPDATE SET
                    status=EXCLUDED.status, minute_bar_count=EXCLUDED.minute_bar_count,
                    minute_symbol_count=EXCLUDED.minute_symbol_count, context_count=EXCLUDED.context_count,
                    indicator_event_count=EXCLUDED.indicator_event_count, market_minute_count=EXCLUDED.market_minute_count,
                    industry_minute_count=EXCLUDED.industry_minute_count, daily_pool_count=EXCLUDED.daily_pool_count,
                    cache_published=EXCLUDED.cache_published, quality=EXCLUDED.quality, error_message=EXCLUDED.error_message,
                    built_at=now(), updated_at=now()
            """), payload)

    def recent_daily_market_states(self, trade_date: date, window_days: int) -> list[dict[str, Any]]:
        """Return prior trading days only; the caller appends its new daily fact."""
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT * FROM {self.client.schema}.trade_review_market_daily_state
                 WHERE trade_date < :trade_date ORDER BY trade_date DESC LIMIT :limit
            """), {"trade_date": trade_date, "limit": max(1, min(240, window_days))}).mappings().all()
        return [dict(row) for row in reversed(rows)]

    def recent_daily_industry_states(self, trade_date: date, window_days: int) -> list[dict[str, Any]]:
        """Return prior trading days only; current-day reruns must be stable."""
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT trade_date, membership, industry_code, industry_name, advancing_count, declining_count,
                       touched_limit_up_count, sealed_limit_up_count, broken_limit_up_count, sealed_limit_down_count,
                       max_height, seal_rate, break_rate, relative_change_pct, factor
                  FROM {self.client.schema}.trade_review_industry_daily_state
                 WHERE trade_date < :trade_date
                   AND trade_date >= (
                       SELECT min(trade_date) FROM (
                           SELECT DISTINCT trade_date FROM {self.client.schema}.trade_review_industry_daily_state
                            WHERE trade_date < :trade_date ORDER BY trade_date DESC LIMIT :limit
                       ) window_dates
                   )
                 ORDER BY membership, industry_code, trade_date
            """), {"trade_date": trade_date, "limit": max(1, min(240, window_days))}).mappings().all()
        return [dict(row) for row in rows]


def _json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, default=str)
