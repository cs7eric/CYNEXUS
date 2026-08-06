from __future__ import annotations

import json
from datetime import date, datetime
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260727_create_market_indicator_tables.sql"
INDUSTRY_DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260728_create_market_indicator_industry_tables.sql"
TRANSMISSION_DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260805_create_market_indicator_transmission_tables.sql"


class MarketIndicatorRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))
        self.client.apply_sql_file(Path(INDUSTRY_DDL_PATH))
        self.client.apply_sql_file(Path(TRANSMISSION_DDL_PATH))

    def save_candidate_pool(self, rows: list[dict[str, Any]], *, captured_at: datetime, source: str) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            for row in rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.market_indicator_candidate_pool (
                        id, trade_date, captured_at, code, pool_tier, reason, priority_score,
                        source, payload, updated_at, deleted_at
                    ) VALUES (
                        :id, :trade_date, :captured_at, :code, :pool_tier, :reason, :priority_score,
                        :source, CAST(:payload AS jsonb), now(), NULL
                    )
                    ON CONFLICT (trade_date, captured_at, code) WHERE deleted_at IS NULL
                    DO UPDATE SET pool_tier = EXCLUDED.pool_tier, reason = EXCLUDED.reason,
                        priority_score = EXCLUDED.priority_score, source = EXCLUDED.source,
                        payload = EXCLUDED.payload, updated_at = now(), deleted_at = NULL
                """), {
                    "id": stable_bigint_id("market_indicator_candidate", captured_at.date(), row["code"], captured_at),
                    "trade_date": captured_at.date(),
                    "captured_at": captured_at,
                    "code": row["code"],
                    "pool_tier": row.get("pool_tier") or "bounded",
                    "reason": row.get("reason") or "candidate_pool",
                    "priority_score": row.get("priority_score"),
                    "source": source,
                    "payload": json.dumps(row, ensure_ascii=False, default=str),
                })
        return len(rows)

    def list_stock_metadata(self) -> dict[str, dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT code, name, market, is_st, limit_up_threshold, industry, float_shares
                  FROM {self.client.schema}.sec_stock_basic
                 WHERE is_active IS TRUE
                 ORDER BY code
            """)).mappings().all()
        return {str(row["code"]).zfill(6): dict(row) for row in rows}

    def list_industry_metadata(self) -> dict[str, dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(text(f"""
                WITH latest_snapshot AS (
                    SELECT max(snapshot_date) AS snapshot_date
                      FROM {self.client.schema}.ths_industry_constituent_snapshot
                     WHERE deleted_at IS NULL
                ),
                ths_stock_industry AS (
                    SELECT DISTINCT ON (stock_code)
                           stock_code AS code,
                           industry_code AS ths_code
                      FROM {self.client.schema}.ths_industry_constituent_snapshot s
                      JOIN latest_snapshot latest ON latest.snapshot_date = s.snapshot_date
                     WHERE s.deleted_at IS NULL
                       AND s.stock_code IS NOT NULL
                       AND s.stock_code <> ''
                       AND s.industry_code IS NOT NULL
                       AND s.industry_code <> ''
                     ORDER BY stock_code, rank NULLS LAST, industry_code
                ),
                ths_mapping AS (
                    SELECT DISTINCT ON (ths_code)
                           ths_code, ths_name, tdx_code6, tdx_name
                      FROM {self.client.schema}.tdx_ths_industry_mapping
                     WHERE deleted_at IS NULL
                     ORDER BY ths_code, is_primary DESC NULLS LAST,
                              priority ASC NULLS LAST, confidence DESC NULLS LAST,
                              tdx_code6
                )
                SELECT s.code,
                       s.ths_code AS industry_code,
                       COALESCE(m.ths_name, s.ths_code) AS industry_name,
                       s.ths_code,
                       m.ths_name,
                       m.tdx_code6 AS tdx_code,
                       m.tdx_name
                  FROM ths_stock_industry s
                  LEFT JOIN ths_mapping m ON m.ths_code = s.ths_code
            """)).mappings().all()
        return {
            str(row["code"]).strip()[-6:].zfill(6): {
                **dict(row),
                "industry_source": "ths90_tdx56_mapping",
                "category_label": "THS90",
            }
            for row in rows
            if str(row["code"]).strip()
        }

    def latest_market_context_inputs(self, trade_date: date) -> dict[str, Any]:
        """Compose the latest persisted Pulse/Index/MSI facts for event context.

        These tables are owned by adjacent market domains.  The indicator
        repository only reads them and returns a best-effort, provenance-rich
        dictionary; a missing optional table never prevents quote capture.
        """
        result: dict[str, Any] = {"provenance": {"source": "postgres.market_context", "tradeDate": trade_date.isoformat()}}
        with self.client.session() as db:
            try:
                pulse = db.execute(text(f"""
                    SELECT extra, fetched_at
                      FROM {self.client.schema}.market_pulse_capture_batches
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     ORDER BY captured_at DESC NULLS LAST
                     LIMIT 1
                """), {"trade_date": trade_date}).mappings().first()
                if pulse:
                    extra = pulse.get("extra") if isinstance(pulse.get("extra"), dict) else {}
                    result["liquidity"] = {
                        "totalAmount": extra.get("totalAmount") or extra.get("total_amount"),
                        "mainNetInflow": extra.get("mainNetInflow") or extra.get("main_net_inflow"),
                        "sourceAsOf": pulse.get("fetched_at"),
                    }
            except Exception:  # noqa: BLE001
                result.setdefault("warnings", []).append("market_pulse_unavailable")
            try:
                indices = db.execute(text(f"""
                    SELECT index_code, index_name, window_days, return_pct, current_trade_date
                      FROM {self.client.schema}.mkt_index_return_daily
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     ORDER BY index_code, window_days
                """), {"trade_date": trade_date}).mappings().all()
                result["indices"] = {
                    str(row["index_code"]): {
                        "name": row.get("index_name"),
                        "windowDays": row.get("window_days"),
                        "returnPct": row.get("return_pct"),
                        "currentTradeDate": row.get("current_trade_date"),
                    }
                    for row in indices
                }
            except Exception:  # noqa: BLE001
                result.setdefault("warnings", []).append("index_returns_unavailable")
            try:
                msi = db.execute(text(f"""
                    SELECT composite_score, level, updated_at
                      FROM {self.client.schema}.msi_index_daily
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                     ORDER BY updated_at DESC NULLS LAST
                     LIMIT 1
                """), {"trade_date": trade_date}).mappings().first()
                if msi:
                    result["sentiment"] = {
                        "msi": msi.get("composite_score"),
                        "level": msi.get("level"),
                        "sourceAsOf": msi.get("updated_at"),
                    }
            except Exception:  # noqa: BLE001
                result.setdefault("warnings", []).append("msi_unavailable")
        return result

    def latest_snapshots(self, trade_date: date | None = None) -> dict[str, dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT DISTINCT ON (code) code, name, market, last_price, prev_close, change_pct, limit_status, captured_at
                 FROM {self.client.schema}.market_indicator_snapshots
                 WHERE deleted_at IS NULL
                   AND last_price > 0
                   AND (:trade_date IS NULL OR trade_date = :trade_date)
                 ORDER BY code, captured_at DESC
            """), {"trade_date": trade_date}).mappings().all()
        return {str(row["code"]): dict(row) for row in rows}

    def list_historical_limit_codes(self, *, since_date: date) -> set[str]:
        """Return the active candidate universe from recent limit-up/down history."""
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT DISTINCT code
                  FROM {self.client.schema}.market_limit_daily_stocks
                 WHERE deleted_at IS NULL
                   AND trade_date >= :since_date
                   AND category IN ('limit_up', 'limit_down')
                   AND code IS NOT NULL
                   AND code <> ''
            """), {"since_date": since_date}).all()
        return {str(row[0]).strip()[-6:].zfill(6) for row in rows if str(row[0]).strip()}

    def list_capture_rows(
        self,
        trade_date: date,
        *,
        start_at: datetime | None = None,
        end_at: datetime | None = None,
    ) -> list[list[dict[str, Any]]]:
        """Load normalized quote captures grouped by capture timestamp."""
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT code, name, market, last_price, prev_close, change_pct, amount, volume,
                       turnover_rate, captured_at, raw_payload
                  FROM {self.client.schema}.market_indicator_snapshots
                 WHERE deleted_at IS NULL
                   AND trade_date = :trade_date
                   AND (:start_at IS NULL OR captured_at >= :start_at)
                   AND (:end_at IS NULL OR captured_at <= :end_at)
                 ORDER BY captured_at, code
            """), {"trade_date": trade_date, "start_at": start_at, "end_at": end_at}).mappings().all()
        grouped: dict[datetime, list[dict[str, Any]]] = {}
        for row in rows:
            captured_at = row.get("captured_at")
            if captured_at is None:
                continue
            payload = row.get("raw_payload") if isinstance(row.get("raw_payload"), dict) else {}
            item = dict(row)
            item.update(payload)
            item["captured_at"] = captured_at
            grouped.setdefault(captured_at, []).append(item)
        return [grouped[key] for key in sorted(grouped)]

    def list_transmission_episode_payloads(self, trade_date: date | None = None, *, limit: int = 500) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(text(f"""
                SELECT episode_id, payload
                  FROM {self.client.schema}.market_indicator_sector_episodes
                 WHERE deleted_at IS NULL
                   AND (:trade_date IS NULL OR trade_date = :trade_date)
                 ORDER BY last_evidence_at DESC
                 LIMIT :limit
            """), {"trade_date": trade_date, "limit": max(1, min(5000, int(limit)))}).mappings().all()
        result: list[dict[str, Any]] = []
        for row in rows:
            payload = row.get("payload")
            if isinstance(payload, dict):
                result.append(dict(payload))
            else:
                try:
                    parsed = json.loads(str(payload or "{}"))
                    if isinstance(parsed, dict):
                        result.append(parsed)
                except (TypeError, ValueError):
                    continue
        return result

    def save_outcomes(self, outcomes: list[dict[str, Any]], *, source: str = "market_indicator_outcome_finalize") -> int:
        if not outcomes:
            return 0
        with self.client.session() as db:
            for outcome in outcomes:
                episode_id = str(outcome.get("episode_id") or "")
                evaluated_at = outcome.get("evaluated_at")
                if not episode_id or evaluated_at is None:
                    continue
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.market_indicator_episode_outcomes (
                        id, episode_id, anchor_type, evaluated_at, outcome, algorithm_version, payload, updated_at, deleted_at
                    ) VALUES (:id, :episode_id, :anchor_type, :evaluated_at, :outcome, :algorithm_version,
                              CAST(:payload AS jsonb), now(), NULL)
                    ON CONFLICT (episode_id, evaluated_at) DO UPDATE SET
                        outcome = EXCLUDED.outcome, algorithm_version = EXCLUDED.algorithm_version,
                        payload = EXCLUDED.payload, updated_at = now(), deleted_at = NULL
                """), {
                    "id": stable_bigint_id("market_indicator_outcome", episode_id, evaluated_at),
                    "episode_id": episode_id,
                    "anchor_type": outcome.get("anchor_type") or "episode_start",
                    "evaluated_at": evaluated_at,
                    "outcome": outcome.get("outcome") or "insufficient_data",
                    "algorithm_version": outcome.get("algorithm_version") or "transmission-outcome-v1",
                    "payload": json.dumps({**outcome, "source": source}, ensure_ascii=False, default=str),
                })
        return len(outcomes)

    def save_benchmark_run(self, payload: dict[str, Any]) -> int:
        with self.client.session() as db:
            run_at = payload.get("run_at") or datetime.utcnow()
            row_id = stable_bigint_id("market_indicator_benchmark", run_at, payload.get("run_id") or "")
            db.execute(text(f"""
                INSERT INTO {self.client.schema}.market_indicator_benchmark_runs (
                    id, run_id, trade_date, provider, universe_size, rounds, target_cadence_seconds,
                    p50_seconds, p95_seconds, p99_seconds, coverage_ratio, fresh_ratio, payload, created_at
                ) VALUES (:id, :run_id, :trade_date, :provider, :universe_size, :rounds, :target_cadence_seconds,
                          :p50_seconds, :p95_seconds, :p99_seconds, :coverage_ratio, :fresh_ratio,
                          CAST(:payload AS jsonb), now())
                ON CONFLICT (run_id) DO UPDATE SET
                    p50_seconds = EXCLUDED.p50_seconds, p95_seconds = EXCLUDED.p95_seconds,
                    p99_seconds = EXCLUDED.p99_seconds, coverage_ratio = EXCLUDED.coverage_ratio,
                    fresh_ratio = EXCLUDED.fresh_ratio, payload = EXCLUDED.payload
            """), {
                "id": row_id, "run_id": payload.get("run_id") or str(row_id),
                "trade_date": payload.get("trade_date"), "provider": payload.get("provider") or "eltdx",
                "universe_size": payload.get("universe_size") or 0, "rounds": payload.get("rounds") or 0,
                "target_cadence_seconds": payload.get("target_cadence_seconds") or 5,
                "p50_seconds": payload.get("p50_seconds"), "p95_seconds": payload.get("p95_seconds"),
                "p99_seconds": payload.get("p99_seconds"), "coverage_ratio": payload.get("coverage_ratio"),
                "fresh_ratio": payload.get("fresh_ratio"), "payload": json.dumps(payload, ensure_ascii=False, default=str),
            })
        return 1

    def save_capture(
        self,
        rows: list[dict[str, Any]],
        events: list[dict[str, Any]],
        industry_rows: list[dict[str, Any]],
        *,
        source: str,
        candidate_rows: list[dict[str, Any]] | None = None,
        captured_at: datetime | None = None,
        transmission: dict[str, Any] | None = None,
    ) -> dict[str, int]:
        if not rows:
            return {"snapshots": 0, "events": 0, "industries": 0}
        with self.client.session() as db:
            if candidate_rows:
                candidate_capture = captured_at or rows[0]["captured_at"]
                for candidate in candidate_rows:
                    db.execute(text(f"""
                        INSERT INTO {self.client.schema}.market_indicator_candidate_pool (
                            id, trade_date, captured_at, code, pool_tier, reason, priority_score,
                            source, payload, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :captured_at, :code, :pool_tier, :reason, :priority_score,
                            :source, CAST(:payload AS jsonb), now(), NULL
                        )
                        ON CONFLICT (trade_date, captured_at, code) WHERE deleted_at IS NULL
                        DO UPDATE SET pool_tier = EXCLUDED.pool_tier, reason = EXCLUDED.reason,
                            priority_score = EXCLUDED.priority_score, source = EXCLUDED.source,
                            payload = EXCLUDED.payload, updated_at = now(), deleted_at = NULL
                    """), {
                        "id": stable_bigint_id("market_indicator_candidate", candidate_capture.date(), candidate["code"], candidate_capture),
                        "trade_date": candidate_capture.date(), "captured_at": candidate_capture,
                        "code": candidate["code"], "pool_tier": candidate.get("pool_tier") or "historical_limit",
                        "reason": candidate.get("reason") or "candidate_pool", "priority_score": candidate.get("priority_score"),
                        "source": source, "payload": json.dumps(candidate, ensure_ascii=False, default=str),
                    })
            for row in rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.market_indicator_snapshots (
                        id, trade_date, code, name, market, last_price, prev_close, open_price,
                        high_price, low_price, change_pct, amount, volume, turnover_rate,
                        limit_up_price, limit_down_price, limit_status, source, captured_at,
                        raw_payload, updated_at
                    ) VALUES (
                        :id, :trade_date, :code, :name, :market, :last_price, :prev_close, :open_price,
                        :high_price, :low_price, :change_pct, :amount, :volume, :turnover_rate,
                        :limit_up_price, :limit_down_price, :limit_status, :source, :captured_at,
                        CAST(:raw_payload AS jsonb), now()
                    )
                    ON CONFLICT (id) DO UPDATE SET
                        trade_date = EXCLUDED.trade_date,
                        code = EXCLUDED.code,
                        name = EXCLUDED.name,
                        market = EXCLUDED.market,
                        last_price = EXCLUDED.last_price,
                        prev_close = EXCLUDED.prev_close,
                        open_price = EXCLUDED.open_price,
                        high_price = EXCLUDED.high_price,
                        low_price = EXCLUDED.low_price,
                        change_pct = EXCLUDED.change_pct,
                        amount = EXCLUDED.amount,
                        volume = EXCLUDED.volume,
                        turnover_rate = EXCLUDED.turnover_rate,
                        limit_up_price = EXCLUDED.limit_up_price,
                        limit_down_price = EXCLUDED.limit_down_price,
                        limit_status = EXCLUDED.limit_status,
                        source = EXCLUDED.source,
                        captured_at = EXCLUDED.captured_at,
                        raw_payload = EXCLUDED.raw_payload,
                        updated_at = now(),
                        deleted_at = NULL
                """), _snapshot_params(row, source=source))
            for event in events:
                event_at = event["event_at"]
                event_minute = event_at.replace(second=0, microsecond=0)
                snapshot = event.get("snapshot") or {}
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.market_indicator_events (
                        id, trade_date, code, name, market, event_type, event_at, event_minute,
                        signal_value, last_price, change_pct, limit_status, source, payload, updated_at
                    ) VALUES (
                        :id, :trade_date, :code, :name, :market, :event_type, :event_at, :event_minute,
                        :signal_value, :last_price, :change_pct, :limit_status, :source,
                        CAST(:payload AS jsonb), now()
                    )
                    ON CONFLICT (trade_date, code, event_type, event_minute) WHERE deleted_at IS NULL
                    DO UPDATE SET name = EXCLUDED.name, signal_value = EXCLUDED.signal_value,
                        last_price = EXCLUDED.last_price, change_pct = EXCLUDED.change_pct,
                        limit_status = EXCLUDED.limit_status, payload = EXCLUDED.payload,
                        updated_at = now()
                """), _event_params(event, snapshot, source=source, event_minute=event_minute))
            for industry in industry_rows:
                db.execute(text(f"""
                    INSERT INTO {self.client.schema}.market_indicator_industry_snapshots (
                        id, trade_date, captured_at, industry_code, industry_name,
                        stock_count, advancing_count, declining_count, flat_count,
                        limit_up_count, limit_up_touch_count, limit_down_count, limit_down_touch_count,
                        rapid_rise_count, rapid_fall_count,
                        first_limit_up_at, first_limit_up_code, first_limit_up_name, first_limit_up_change_pct,
                        first_event_at, first_event_code, first_event_name, first_event_type,
                        first_event_signal_value, source, payload, updated_at
                    ) VALUES (
                        :id, :trade_date, :captured_at, :industry_code, :industry_name,
                        :stock_count, :advancing_count, :declining_count, :flat_count,
                        :limit_up_count, :limit_up_touch_count, :limit_down_count, :limit_down_touch_count,
                        :rapid_rise_count, :rapid_fall_count,
                        :first_limit_up_at, :first_limit_up_code, :first_limit_up_name, :first_limit_up_change_pct,
                        :first_event_at, :first_event_code, :first_event_name, :first_event_type,
                        :first_event_signal_value, :source, CAST(:payload AS jsonb), now()
                    )
                    ON CONFLICT (trade_date, captured_at, industry_name) WHERE deleted_at IS NULL
                    DO UPDATE SET
                        industry_code = EXCLUDED.industry_code,
                        stock_count = EXCLUDED.stock_count,
                        advancing_count = EXCLUDED.advancing_count,
                        declining_count = EXCLUDED.declining_count,
                        flat_count = EXCLUDED.flat_count,
                        limit_up_count = EXCLUDED.limit_up_count,
                        limit_up_touch_count = EXCLUDED.limit_up_touch_count,
                        limit_down_count = EXCLUDED.limit_down_count,
                        limit_down_touch_count = EXCLUDED.limit_down_touch_count,
                        rapid_rise_count = EXCLUDED.rapid_rise_count,
                        rapid_fall_count = EXCLUDED.rapid_fall_count,
                        first_limit_up_at = EXCLUDED.first_limit_up_at,
                        first_limit_up_code = EXCLUDED.first_limit_up_code,
                        first_limit_up_name = EXCLUDED.first_limit_up_name,
                        first_limit_up_change_pct = EXCLUDED.first_limit_up_change_pct,
                        first_event_at = EXCLUDED.first_event_at,
                        first_event_code = EXCLUDED.first_event_code,
                        first_event_name = EXCLUDED.first_event_name,
                        first_event_type = EXCLUDED.first_event_type,
                        first_event_signal_value = EXCLUDED.first_event_signal_value,
                        source = EXCLUDED.source,
                        payload = EXCLUDED.payload,
                        updated_at = now(), deleted_at = NULL
                """), _industry_params(industry, source=source))
            _save_transmission(db, transmission or {}, source=source, schema=self.client.schema)
        return {"snapshots": len(rows), "events": len(events), "industries": len(industry_rows)}


def _snapshot_params(row: dict[str, Any], *, source: str) -> dict[str, Any]:
    captured = row["captured_at"]
    snapshot_id = stable_bigint_id("market_indicator_snapshot", captured, row["code"])
    return {**row, "id": snapshot_id, "trade_date": captured.date(), "source": source,
            "raw_payload": json.dumps(row.get("raw_payload") or {}, ensure_ascii=False, default=str)}


def _event_params(event: dict[str, Any], snapshot: dict[str, Any], *, source: str, event_minute: datetime) -> dict[str, Any]:
    event_at = event["event_at"]
    return {
        "id": stable_bigint_id("market_indicator_event", event_at.date(), event["code"], event["event_type"], event_minute),
        "trade_date": event_at.date(), "code": event["code"], "name": event.get("name"),
        "market": event.get("market"), "event_type": event["event_type"], "event_at": event_at,
        "event_minute": event_minute, "signal_value": event.get("signal_value"),
        "last_price": snapshot.get("last_price"), "change_pct": snapshot.get("change_pct"),
        "limit_status": snapshot.get("limit_status"), "source": source,
        "payload": json.dumps(snapshot.get("raw_payload") or {}, ensure_ascii=False, default=str),
    }


def _industry_params(row: dict[str, Any], *, source: str) -> dict[str, Any]:
    captured_at = row["captured_at"]
    return {
        **row,
        "id": stable_bigint_id("market_indicator_industry", row["trade_date"], row["industry_name"], captured_at),
        "source": source,
        "payload": json.dumps({"industry_name": row["industry_name"]}, ensure_ascii=False),
    }


def _save_transmission(db: Any, transmission: dict[str, Any], *, source: str, schema: str) -> None:
    """Persist the canonical transmission facts without coupling SQL to Python dataclasses."""
    if not transmission:
        return
    for episode in transmission.get("episodes") or []:
        episode_id = str(episode.get("episode_id") or "")
        if not episode_id:
            continue
        confidence = episode.get("confidence") or {}
        quality = episode.get("data_quality") or {}
        db.execute(text(f"""
            INSERT INTO {schema}.market_indicator_sector_episodes (
                id, episode_id, trade_date, sector_type, sector_code, sector_name, direction, stage,
                started_at, confirmed_at, last_evidence_at, ended_at, eligible_count, observed_count,
                responded_count, coverage_ratio, coverage_peak, median_lag_seconds, p75_lag_seconds,
                sector_equal_return_pct, sector_median_return_pct, ex_leader_return_pct, weighted_return_pct,
                benchmark_excess_pct, breadth_delta_pct_point, amount_acceleration, confidence_score,
                confidence_level, data_quality_grade, source, algorithm_version, payload, updated_at, deleted_at
            ) VALUES (
                :id, :episode_id, :trade_date, :sector_type, :sector_code, :sector_name, :direction, :stage,
                :started_at, :confirmed_at, :last_evidence_at, :ended_at, :eligible_count, :observed_count,
                :responded_count, :coverage_ratio, :coverage_peak, :median_lag_seconds, :p75_lag_seconds,
                :sector_equal_return_pct, :sector_median_return_pct, :ex_leader_return_pct, :weighted_return_pct,
                :benchmark_excess_pct, :breadth_delta_pct_point, :amount_acceleration, :confidence_score,
                :confidence_level, :data_quality_grade, :source, :algorithm_version, CAST(:payload AS jsonb), now(), NULL
            )
            ON CONFLICT (episode_id) DO UPDATE SET
                stage = EXCLUDED.stage, confirmed_at = EXCLUDED.confirmed_at,
                last_evidence_at = EXCLUDED.last_evidence_at, ended_at = EXCLUDED.ended_at,
                observed_count = EXCLUDED.observed_count, responded_count = EXCLUDED.responded_count,
                coverage_ratio = EXCLUDED.coverage_ratio, coverage_peak = EXCLUDED.coverage_peak,
                confidence_score = EXCLUDED.confidence_score, confidence_level = EXCLUDED.confidence_level,
                data_quality_grade = EXCLUDED.data_quality_grade, payload = EXCLUDED.payload,
                updated_at = now(), deleted_at = NULL
        """), {
            "id": stable_bigint_id("market_indicator_episode", episode_id), "episode_id": episode_id,
            "trade_date": episode.get("trade_date"), "sector_type": episode.get("sector_type"),
            "sector_code": episode.get("sector_code"), "sector_name": episode.get("sector_name"),
            "direction": episode.get("direction"), "stage": episode.get("stage"),
            "started_at": episode.get("started_at"), "confirmed_at": episode.get("confirmed_at"),
            "last_evidence_at": episode.get("last_evidence_at"), "ended_at": episode.get("ended_at"),
            "eligible_count": episode.get("eligible_count") or 0, "observed_count": episode.get("observed_count") or 0,
            "responded_count": episode.get("responded_count") or 0, "coverage_ratio": episode.get("coverage_ratio"),
            "coverage_peak": episode.get("coverage_peak"), "median_lag_seconds": episode.get("median_lag_seconds"),
            "p75_lag_seconds": episode.get("p75_lag_seconds"), "sector_equal_return_pct": episode.get("sector_equal_return_pct"),
            "sector_median_return_pct": episode.get("sector_median_return_pct"), "ex_leader_return_pct": episode.get("ex_leader_return_pct"),
            "weighted_return_pct": episode.get("weighted_return_pct"), "benchmark_excess_pct": episode.get("benchmark_excess_pct"),
            "breadth_delta_pct_point": episode.get("breadth_delta_pct_point"), "amount_acceleration": episode.get("amount_acceleration"),
            "confidence_score": confidence.get("score"), "confidence_level": confidence.get("level"),
            "data_quality_grade": quality.get("grade"), "source": source,
            "algorithm_version": episode.get("provenance", {}).get("algorithmVersion") or transmission.get("algorithm_version"),
            "payload": json.dumps(episode, ensure_ascii=False, default=str),
        })
    for outcome in transmission.get("outcomes") or []:
        outcome_id = str(outcome.get("episode_id") or "")
        if not outcome_id:
            continue
        db.execute(text(f"""
            INSERT INTO {schema}.market_indicator_episode_outcomes (
                id, episode_id, anchor_type, evaluated_at, outcome, algorithm_version, payload, updated_at, deleted_at
            ) VALUES (:id, :episode_id, :anchor_type, :evaluated_at, :outcome, :algorithm_version, CAST(:payload AS jsonb), now(), NULL)
            ON CONFLICT (episode_id, evaluated_at) DO UPDATE SET outcome = EXCLUDED.outcome,
                algorithm_version = EXCLUDED.algorithm_version, payload = EXCLUDED.payload, updated_at = now(), deleted_at = NULL
        """), {
            "id": stable_bigint_id("market_indicator_outcome", outcome_id, outcome.get("evaluated_at")),
            "episode_id": outcome_id, "anchor_type": outcome.get("anchor_type"),
            "evaluated_at": outcome.get("evaluated_at"), "outcome": outcome.get("outcome"),
            "algorithm_version": outcome.get("algorithm_version") or transmission.get("algorithm_version"),
            "payload": json.dumps(outcome, ensure_ascii=False, default=str),
        })
    for advice in transmission.get("advice") or []:
        advice_id = str(advice.get("advice_id") or "")
        if not advice_id:
            continue
        db.execute(text(f"""
            INSERT INTO {schema}.market_indicator_trade_reference_advice (
                id, advice_id, episode_id, target_type, target_code, target_name, position_state, action_class,
                generated_at, as_of, valid_until, horizon, confidence, disclaimer_code, policy_version,
                source, payload, updated_at, deleted_at
            ) VALUES (:id, :advice_id, :episode_id, :target_type, :target_code, :target_name, :position_state, :action_class,
                :generated_at, :as_of, :valid_until, :horizon, :confidence, :disclaimer_code, :policy_version,
                :source, CAST(:payload AS jsonb), now(), NULL)
            ON CONFLICT (advice_id) DO UPDATE SET action_class = EXCLUDED.action_class,
                confidence = EXCLUDED.confidence, as_of = EXCLUDED.as_of, valid_until = EXCLUDED.valid_until,
                payload = EXCLUDED.payload, updated_at = now(), deleted_at = NULL
        """), {
            "id": stable_bigint_id("market_indicator_advice", advice_id), "advice_id": advice_id,
            "episode_id": advice.get("episode_id"), "target_type": advice.get("target_type"),
            "target_code": advice.get("target_code"), "target_name": advice.get("target_name"),
            "position_state": advice.get("position_state"), "action_class": advice.get("action_class"),
            "generated_at": advice.get("generated_at"), "as_of": advice.get("as_of"),
            "valid_until": advice.get("valid_until"), "horizon": advice.get("horizon"),
            "confidence": advice.get("confidence"), "disclaimer_code": advice.get("disclaimer_code"),
            "policy_version": advice.get("policy_version"), "source": source,
            "payload": json.dumps(advice, ensure_ascii=False, default=str),
        })
