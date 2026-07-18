from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.market.market_overview import NUMERIC_FIELDS, merge_source, should_overwrite_field
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_overview_tables.sql"
OVERVIEW_TABLE = "market_overview_snapshots"
DETAIL_TABLE = "mkt_overview_detail_snapshot"


class MarketOverviewRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def get(self, trade_date) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(f"SELECT * FROM {self.client.schema}.{OVERVIEW_TABLE} WHERE trade_date = :trade_date AND deleted_at IS NULL"),
                {"trade_date": trade_date},
            ).mappings().first()
        return dict(row) if row else None

    def list_snapshots(self, *, end_date, limit: int = 260) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT *
                      FROM {self.client.schema}.{OVERVIEW_TABLE}
                     WHERE trade_date <= :end_date AND deleted_at IS NULL
                     ORDER BY trade_date DESC
                     LIMIT :limit
                    """
                ),
                {"end_date": end_date, "limit": int(limit)},
            ).mappings().all()
        return [dict(row) for row in reversed(rows)]

    def record_source_failure(self, *, source: str, error: str, attempted_at: str) -> str | None:
        """Record a failed source attempt without changing the latest numeric snapshot."""
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT trade_date, extra
                      FROM {self.client.schema}.{OVERVIEW_TABLE}
                     WHERE deleted_at IS NULL AND main_net_inflow IS NOT NULL
                     ORDER BY trade_date DESC
                     LIMIT 1
                    """
                )
            ).mappings().first()
            has_successful_value = row is not None
            if not row:
                row = db.execute(
                    text(
                        f"""
                        SELECT trade_date, extra
                          FROM {self.client.schema}.{OVERVIEW_TABLE}
                         WHERE deleted_at IS NULL
                         ORDER BY trade_date DESC
                         LIMIT 1
                        """
                    )
                ).mappings().first()
            if not row:
                return None
            extra = _json_object(row.get("extra"))
            status = _json_object(extra.get("market_fund_flow"))
            last_successful_trade_date = status.get("last_successful_trade_date")
            if not last_successful_trade_date and has_successful_value:
                last_successful_trade_date = _date_text(row.get("trade_date"))
            status.update(
                source=source,
                source_mode="failed",
                fallback_used=True,
                stale=True,
                error=error,
                attempted_at=attempted_at,
                last_successful_trade_date=last_successful_trade_date,
                last_successful_fetched_at=status.get("last_successful_fetched_at"),
            )
            extra["market_fund_flow"] = status
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.{OVERVIEW_TABLE}
                       SET extra = CAST(:extra AS jsonb), updated_at = now()
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": row["trade_date"], "extra": json.dumps(extra, ensure_ascii=False, default=str)},
            )
        return last_successful_trade_date

    def upsert_fields(
        self,
        fields: dict[str, Any],
        *,
        source: str,
        extra: dict[str, Any] | None = None,
        clear_manual_override: bool = False,
    ) -> dict[str, Any]:
        trade_date = fields["trade_date"]
        existing = self.get(trade_date)
        merged: dict[str, Any] = {"trade_date": trade_date}
        for field in NUMERIC_FIELDS:
            new_value = fields.get(field)
            current_value = existing.get(field) if existing else None
            merged[field] = new_value if new_value is not None and should_overwrite_field(source=source, field=field, current_value=current_value) else current_value
        merged["source"] = merge_source(existing.get("source") if existing else None, source)
        merged["is_manual_override"] = (
            False
            if clear_manual_override
            else bool((existing or {}).get("is_manual_override") or source == "manual")
        )
        is_manual_source = source == "manual"
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.{OVERVIEW_TABLE} (
                        id, trade_date, total_amount, total_volume, rising_count, falling_count, flat_count,
                        limit_up_count, limit_down_count, stock_count, main_net_inflow,
                        super_large_net_inflow, large_net_inflow, medium_net_inflow, small_net_inflow,
                        main_net_inflow_ratio, super_large_net_ratio, large_net_ratio, medium_net_ratio,
                        small_net_ratio, source, is_manual_override, manual_updated_at, extra, updated_at, deleted_at
                    ) VALUES (
                        :id, :trade_date, :total_amount, :total_volume, :rising_count, :falling_count, :flat_count,
                        :limit_up_count, :limit_down_count, :stock_count, :main_net_inflow,
                        :super_large_net_inflow, :large_net_inflow, :medium_net_inflow, :small_net_inflow,
                        :main_net_inflow_ratio, :super_large_net_ratio, :large_net_ratio, :medium_net_ratio,
                        :small_net_ratio, :source, :is_manual_override,
                        CASE WHEN :is_manual_source THEN now() ELSE NULL END,
                        CAST(:extra AS jsonb), now(), NULL
                    )
                    ON CONFLICT (trade_date) WHERE deleted_at IS NULL DO UPDATE SET
                        total_amount = EXCLUDED.total_amount,
                        total_volume = EXCLUDED.total_volume,
                        rising_count = EXCLUDED.rising_count,
                        falling_count = EXCLUDED.falling_count,
                        flat_count = EXCLUDED.flat_count,
                        limit_up_count = EXCLUDED.limit_up_count,
                        limit_down_count = EXCLUDED.limit_down_count,
                        stock_count = EXCLUDED.stock_count,
                        main_net_inflow = EXCLUDED.main_net_inflow,
                        super_large_net_inflow = EXCLUDED.super_large_net_inflow,
                        large_net_inflow = EXCLUDED.large_net_inflow,
                        medium_net_inflow = EXCLUDED.medium_net_inflow,
                        small_net_inflow = EXCLUDED.small_net_inflow,
                        main_net_inflow_ratio = EXCLUDED.main_net_inflow_ratio,
                        super_large_net_ratio = EXCLUDED.super_large_net_ratio,
                        large_net_ratio = EXCLUDED.large_net_ratio,
                        medium_net_ratio = EXCLUDED.medium_net_ratio,
                        small_net_ratio = EXCLUDED.small_net_ratio,
                        source = EXCLUDED.source,
                        is_manual_override = EXCLUDED.is_manual_override,
                        manual_updated_at = CASE
                            WHEN :clear_manual_override THEN NULL
                            ELSE COALESCE(EXCLUDED.manual_updated_at, {self.client.schema}.{OVERVIEW_TABLE}.manual_updated_at)
                        END,
                        extra = EXCLUDED.extra,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    **merged,
                    "id": next_bigint_id(),
                    "is_manual_source": is_manual_source,
                    "clear_manual_override": clear_manual_override,
                    "extra": json.dumps(
                        {**_json_object(existing.get("extra") if existing else None), **(extra or {})},
                        ensure_ascii=False,
                        default=str,
                    ),
                },
            )
        return self.get(trade_date) or merged

    def get_detail(self, trade_date, *, benchmark_code: str, source_table: str) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT *
                      FROM {self.client.schema}.{DETAIL_TABLE}
                     WHERE trade_date = :trade_date
                       AND benchmark_code = :benchmark_code
                       AND source_table = :source_table
                       AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date, "benchmark_code": benchmark_code, "source_table": source_table},
            ).mappings().first()
        return dict(row) if row else None

    def upsert_detail(
        self,
        *,
        trade_date,
        benchmark_code: str,
        source_table: str,
        payload: dict[str, Any],
        source: str = "python",
    ) -> dict[str, Any]:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.{DETAIL_TABLE} (
                        id, trade_date, benchmark_code, source_table, payload, source, generated_at,
                        updated_at, deleted_at
                    ) VALUES (
                        :id, :trade_date, :benchmark_code, :source_table, CAST(:payload AS jsonb), :source,
                        now(), now(), NULL
                    )
                    ON CONFLICT (trade_date, benchmark_code, source_table) WHERE deleted_at IS NULL DO UPDATE SET
                        payload = EXCLUDED.payload,
                        source = EXCLUDED.source,
                        generated_at = EXCLUDED.generated_at,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": trade_date,
                    "id": next_bigint_id(),
                    "benchmark_code": benchmark_code,
                    "source_table": source_table,
                    "payload": json.dumps(payload, ensure_ascii=False, default=str),
                    "source": source,
                },
            )
        return self.get_detail(trade_date, benchmark_code=benchmark_code, source_table=source_table) or {
            "trade_date": trade_date,
            "benchmark_code": benchmark_code,
            "source_table": source_table,
            "payload": payload,
            "source": source,
        }


def _json_object(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return dict(value)
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
        except (TypeError, ValueError):
            return {}
        return dict(parsed) if isinstance(parsed, dict) else {}
    return {}


def _date_text(value: Any) -> str | None:
    return value.isoformat() if hasattr(value, "isoformat") else (str(value) if value else None)
