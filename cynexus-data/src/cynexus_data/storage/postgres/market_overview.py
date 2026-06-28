from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.market.market_overview import NUMERIC_FIELDS, merge_source, should_overwrite_field
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_market_overview_tables.sql"


class MarketOverviewRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def get(self, trade_date) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(f"SELECT * FROM {self.client.schema}.market_overview_snapshot WHERE trade_date = :trade_date AND deleted_at IS NULL"),
                {"trade_date": trade_date},
            ).mappings().first()
        return dict(row) if row else None

    def list_snapshots(self, *, end_date, limit: int = 260) -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT *
                      FROM {self.client.schema}.market_overview_snapshot
                     WHERE trade_date <= :end_date AND deleted_at IS NULL
                     ORDER BY trade_date DESC
                     LIMIT :limit
                    """
                ),
                {"end_date": end_date, "limit": int(limit)},
            ).mappings().all()
        return [dict(row) for row in reversed(rows)]

    def upsert_fields(self, fields: dict[str, Any], *, source: str, extra: dict[str, Any] | None = None) -> dict[str, Any]:
        trade_date = fields["trade_date"]
        existing = self.get(trade_date)
        merged: dict[str, Any] = {"trade_date": trade_date}
        for field in NUMERIC_FIELDS:
            new_value = fields.get(field)
            current_value = existing.get(field) if existing else None
            merged[field] = new_value if new_value is not None and should_overwrite_field(source=source, field=field, current_value=current_value) else current_value
        merged["source"] = merge_source(existing.get("source") if existing else None, source)
        merged["is_manual_override"] = bool((existing or {}).get("is_manual_override") or source == "manual")
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.market_overview_snapshot (
                        trade_date, total_amount, total_volume, rising_count, falling_count, flat_count,
                        limit_up_count, limit_down_count, stock_count, main_net_inflow,
                        super_large_net_inflow, large_net_inflow, medium_net_inflow, small_net_inflow,
                        main_net_inflow_ratio, super_large_net_ratio, large_net_ratio, medium_net_ratio,
                        small_net_ratio, source, is_manual_override, manual_updated_at, extra, updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :total_amount, :total_volume, :rising_count, :falling_count, :flat_count,
                        :limit_up_count, :limit_down_count, :stock_count, :main_net_inflow,
                        :super_large_net_inflow, :large_net_inflow, :medium_net_inflow, :small_net_inflow,
                        :main_net_inflow_ratio, :super_large_net_ratio, :large_net_ratio, :medium_net_ratio,
                        :small_net_ratio, :source, :is_manual_override,
                        CASE WHEN :is_manual_override THEN now() ELSE NULL END,
                        CAST(:extra AS jsonb), now(), NULL
                    )
                    ON CONFLICT (trade_date) DO UPDATE SET
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
                        manual_updated_at = COALESCE(EXCLUDED.manual_updated_at, {self.client.schema}.market_overview_snapshot.manual_updated_at),
                        extra = EXCLUDED.extra,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {**merged, "extra": json.dumps(extra or {}, ensure_ascii=False, default=str)},
            )
        return self.get(trade_date) or merged

    def get_detail(self, trade_date, *, benchmark_code: str, source_table: str) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT *
                      FROM {self.client.schema}.market_overview_detail_snapshot
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
                    INSERT INTO {self.client.schema}.market_overview_detail_snapshot (
                        trade_date, benchmark_code, source_table, payload, source, generated_at,
                        updated_at, deleted_at
                    ) VALUES (
                        :trade_date, :benchmark_code, :source_table, CAST(:payload AS jsonb), :source,
                        now(), now(), NULL
                    )
                    ON CONFLICT (trade_date, benchmark_code, source_table) DO UPDATE SET
                        payload = EXCLUDED.payload,
                        source = EXCLUDED.source,
                        generated_at = EXCLUDED.generated_at,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "trade_date": trade_date,
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
