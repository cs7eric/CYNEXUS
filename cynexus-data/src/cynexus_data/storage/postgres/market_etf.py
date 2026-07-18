from __future__ import annotations

import json
import math
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260718_create_market_etf_tables.sql"


class MarketEtfRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def upsert_snapshot(self, rows: list[dict[str, Any]], *, trade_date: date, source: str) -> int:
        if not rows:
            return 0
        captured_at = datetime.now(timezone.utc)
        snapshot_minute = captured_at.replace(second=0, microsecond=0)
        with self.client.session() as db:
            for row in rows:
                params = self._snapshot_params(row, trade_date, source, captured_at, snapshot_minute)
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.market_etf_spot_snapshot (
                            id, trade_date, snapshot_minute, captured_at, code, exchange, name, category, theme, classification_source,
                            price, prev_close, change_pct, volume, amount, iopv, premium_pct, turnover_pct,
                            inside_volume, outside_volume, latest_shares, main_net_inflow, main_net_ratio,
                            super_large_net_inflow, super_large_net_ratio, large_net_inflow, large_net_ratio,
                            medium_net_inflow, medium_net_ratio, small_net_inflow, small_net_ratio,
                            source, raw_payload, created_at, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :snapshot_minute, :captured_at, :code, :exchange, :name, :category, :theme, :classification_source,
                            :price, :prev_close, :change_pct, :volume, :amount, :iopv, :premium_pct, :turnover_pct,
                            :inside_volume, :outside_volume, :latest_shares, :main_net_inflow, :main_net_ratio,
                            :super_large_net_inflow, :super_large_net_ratio, :large_net_inflow, :large_net_ratio,
                            :medium_net_inflow, :medium_net_ratio, :small_net_inflow, :small_net_ratio,
                            :source, CAST(:raw_payload AS jsonb), now(), now(), NULL
                        )
                        ON CONFLICT (trade_date, snapshot_minute, code, source) WHERE deleted_at IS NULL DO UPDATE SET
                            captured_at = EXCLUDED.captured_at, exchange = EXCLUDED.exchange, name = EXCLUDED.name,
                            category = EXCLUDED.category, theme = EXCLUDED.theme, classification_source = EXCLUDED.classification_source, price = EXCLUDED.price,
                            prev_close = EXCLUDED.prev_close, change_pct = EXCLUDED.change_pct, volume = EXCLUDED.volume,
                            amount = EXCLUDED.amount, iopv = EXCLUDED.iopv, premium_pct = EXCLUDED.premium_pct,
                            turnover_pct = EXCLUDED.turnover_pct, inside_volume = EXCLUDED.inside_volume,
                            outside_volume = EXCLUDED.outside_volume, latest_shares = EXCLUDED.latest_shares,
                            main_net_inflow = EXCLUDED.main_net_inflow, main_net_ratio = EXCLUDED.main_net_ratio,
                            super_large_net_inflow = EXCLUDED.super_large_net_inflow,
                            super_large_net_ratio = EXCLUDED.super_large_net_ratio,
                            large_net_inflow = EXCLUDED.large_net_inflow, large_net_ratio = EXCLUDED.large_net_ratio,
                            medium_net_inflow = EXCLUDED.medium_net_inflow, medium_net_ratio = EXCLUDED.medium_net_ratio,
                            small_net_inflow = EXCLUDED.small_net_inflow, small_net_ratio = EXCLUDED.small_net_ratio,
                            raw_payload = EXCLUDED.raw_payload, updated_at = now(), deleted_at = NULL
                        """
                    ),
                    params,
                )
        return len(rows)

    def upsert_daily(self, rows: list[dict[str, Any]], *, trade_date: date, source: str) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            for row in rows:
                previous = db.execute(
                    text(
                        f"""
                        SELECT total_shares
                          FROM {self.client.schema}.market_etf_daily
                         WHERE code = :code AND trade_date < :trade_date AND deleted_at IS NULL
                         ORDER BY trade_date DESC LIMIT 1
                        """
                    ),
                    {"code": row["code"], "trade_date": trade_date},
                ).scalar_one_or_none()
                total_shares = row.get("total_shares", row.get("latest_shares"))
                share_delta = total_shares - previous if total_shares is not None and previous is not None else None
                proxy = share_delta * row["iopv"] if share_delta is not None and row.get("iopv") is not None else None
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.market_etf_daily (
                            id, trade_date, code, exchange, name, category, theme, classification_source, close_price, iopv, premium_pct,
                            volume, amount, total_shares, previous_shares, share_delta, net_subscription_proxy,
                            cumulative_share_delta, cumulative_net_subscription_proxy, turnover_pct,
                            inside_volume, outside_volume, super_large_net_inflow, super_large_net_ratio,
                            main_net_inflow, main_net_ratio, large_net_inflow, large_net_ratio,
                            medium_net_inflow, medium_net_ratio, small_net_inflow, small_net_ratio,
                            source, raw_payload, created_at, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :code, :exchange, :name, :category, :theme, :classification_source, :close_price, :iopv,
                            :premium_pct, :volume, :amount, :total_shares, :previous_shares, :share_delta,
                            :net_subscription_proxy, :cumulative_share_delta, :cumulative_net_subscription_proxy,
                            :turnover_pct, :inside_volume, :outside_volume, :super_large_net_inflow, :super_large_net_ratio,
                            :main_net_inflow, :main_net_ratio, :large_net_inflow, :large_net_ratio,
                            :medium_net_inflow, :medium_net_ratio, :small_net_inflow, :small_net_ratio,
                            :source, CAST(:raw_payload AS jsonb), now(), now(), NULL
                        )
                        ON CONFLICT (trade_date, code, source) WHERE deleted_at IS NULL DO UPDATE SET
                            exchange = EXCLUDED.exchange, name = EXCLUDED.name, category = EXCLUDED.category,
                            theme = EXCLUDED.theme, classification_source = EXCLUDED.classification_source, close_price = EXCLUDED.close_price, iopv = EXCLUDED.iopv,
                            premium_pct = EXCLUDED.premium_pct, volume = EXCLUDED.volume, amount = EXCLUDED.amount,
                            total_shares = EXCLUDED.total_shares, previous_shares = EXCLUDED.previous_shares,
                            share_delta = EXCLUDED.share_delta, net_subscription_proxy = EXCLUDED.net_subscription_proxy,
                            cumulative_share_delta = EXCLUDED.cumulative_share_delta,
                            cumulative_net_subscription_proxy = EXCLUDED.cumulative_net_subscription_proxy,
                            turnover_pct = EXCLUDED.turnover_pct, inside_volume = EXCLUDED.inside_volume,
                            outside_volume = EXCLUDED.outside_volume,
                            super_large_net_inflow = EXCLUDED.super_large_net_inflow,
                            super_large_net_ratio = EXCLUDED.super_large_net_ratio,
                            main_net_inflow = EXCLUDED.main_net_inflow, main_net_ratio = EXCLUDED.main_net_ratio,
                            large_net_inflow = EXCLUDED.large_net_inflow, large_net_ratio = EXCLUDED.large_net_ratio,
                            medium_net_inflow = EXCLUDED.medium_net_inflow, medium_net_ratio = EXCLUDED.medium_net_ratio,
                            small_net_inflow = EXCLUDED.small_net_inflow, small_net_ratio = EXCLUDED.small_net_ratio,
                            raw_payload = EXCLUDED.raw_payload,
                            updated_at = now(), deleted_at = NULL
                        """
                    ),
                    {
                        "id": stable_bigint_id("market_etf_daily", trade_date, row["code"], source),
                        "trade_date": trade_date,
                        "code": row["code"],
                        "exchange": row["exchange"],
                        "name": row["name"],
                        "category": row["category"],
                        "theme": row.get("theme"),
                        "classification_source": row.get("classification_source", "fallback.name_keyword"),
                        "close_price": row.get("price"),
                        "iopv": row.get("iopv"),
                        "premium_pct": row.get("premium_pct"),
                        "volume": row.get("volume"),
                        "amount": row.get("amount"),
                        "total_shares": total_shares,
                        "previous_shares": previous,
                        "share_delta": share_delta,
                        "net_subscription_proxy": proxy,
                        "cumulative_share_delta": None,
                        "cumulative_net_subscription_proxy": None,
                        "turnover_pct": row.get("turnover_pct"),
                        "inside_volume": row.get("inside_volume"),
                        "outside_volume": row.get("outside_volume"),
                        "super_large_net_inflow": row.get("super_large_net_inflow"),
                        "super_large_net_ratio": row.get("super_large_net_ratio"),
                        "main_net_inflow": row.get("main_net_inflow"),
                        "main_net_ratio": row.get("main_net_ratio"),
                        "large_net_inflow": row.get("large_net_inflow"),
                        "large_net_ratio": row.get("large_net_ratio"),
                        "medium_net_inflow": row.get("medium_net_inflow"),
                        "medium_net_ratio": row.get("medium_net_ratio"),
                        "small_net_inflow": row.get("small_net_inflow"),
                        "small_net_ratio": row.get("small_net_ratio"),
                        "source": source,
                        "raw_payload": _json_payload(row.get("raw_payload") or {}),
                    },
                )
            self._recompute_share_cumulative(db, source)
        return len(rows)

    def _recompute_share_cumulative(self, db, source: str) -> None:
        db.execute(
            text(
                f"""
                WITH lagged AS (
                    SELECT id, code, source, trade_date, total_shares, iopv,
                           lag(total_shares) OVER (PARTITION BY code, source ORDER BY trade_date) AS previous_value
                      FROM {self.client.schema}.market_etf_daily
                     WHERE source = :source AND deleted_at IS NULL
                ), calculated AS (
                    SELECT id, previous_value,
                           CASE WHEN total_shares IS NOT NULL AND previous_value IS NOT NULL
                                THEN total_shares - previous_value END AS delta_value,
                           CASE WHEN total_shares IS NOT NULL AND previous_value IS NOT NULL AND iopv IS NOT NULL
                                THEN (total_shares - previous_value) * iopv END AS proxy_value
                      FROM lagged
                ), cumulative AS (
                    SELECT id, previous_value, delta_value, proxy_value,
                           sum(delta_value) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumulative_delta,
                           sum(proxy_value) OVER (PARTITION BY code ORDER BY trade_date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumulative_proxy
                      FROM (
                          SELECT c.*, l.code, l.trade_date
                            FROM calculated c
                            JOIN lagged l ON l.id = c.id
                      ) ordered
                )
                UPDATE {self.client.schema}.market_etf_daily target
                   SET previous_shares = cumulative.previous_value,
                       share_delta = cumulative.delta_value,
                       net_subscription_proxy = cumulative.proxy_value,
                       cumulative_share_delta = cumulative.cumulative_delta,
                       cumulative_net_subscription_proxy = cumulative.cumulative_proxy,
                       updated_at = now()
                  FROM cumulative
                 WHERE target.id = cumulative.id
                """
            ),
            {"source": source},
        )

    def upsert_catalog(self, rows: list[dict[str, Any]], *, source: str) -> int:
        with self.client.session() as db:
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.market_etf_catalog (
                            id, code, exchange, name, category, theme, classification_source, source, is_active, raw_payload,
                            created_at, updated_at, deleted_at
                        ) VALUES (:id, :code, :exchange, :name, :category, :theme, :classification_source, :source, true,
                                  CAST(:raw_payload AS jsonb), now(), now(), NULL)
                        ON CONFLICT (code) WHERE deleted_at IS NULL DO UPDATE SET
                            exchange = EXCLUDED.exchange, name = EXCLUDED.name, category = EXCLUDED.category,
                            theme = EXCLUDED.theme, classification_source = EXCLUDED.classification_source, source = EXCLUDED.source, is_active = true,
                            raw_payload = EXCLUDED.raw_payload, updated_at = now(), deleted_at = NULL
                        """
                    ),
                    {
                        "id": stable_bigint_id("market_etf_catalog", row["code"]),
                        "code": row["code"],
                        "exchange": row["exchange"],
                        "name": row["name"],
                        "category": row["category"],
                        "theme": row.get("theme"),
                        "classification_source": row.get("classification_source", "fallback.name_keyword"),
                        "source": source,
                        "raw_payload": _json_payload(row.get("raw_payload") or {}),
                    },
                )
        return len(rows)

    def upsert_mappings(self, mappings: list[dict[str, Any]]) -> int:
        if not mappings:
            return 0
        with self.client.session() as db:
            for mapping in mappings:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.market_etf_mapping (
                            id, etf_code, mapping_group, mapping_code, mapping_name,
                            is_primary, confidence, source, effective_from, raw_payload,
                            created_at, updated_at, deleted_at
                        ) VALUES (
                            :id, :etf_code, :mapping_group, :mapping_code, :mapping_name,
                            :is_primary, :confidence, :source, CURRENT_DATE, CAST(:raw_payload AS jsonb),
                            now(), now(), NULL
                        )
                        ON CONFLICT (etf_code, mapping_group, mapping_code, source) WHERE deleted_at IS NULL DO UPDATE SET
                            mapping_name = EXCLUDED.mapping_name, is_primary = EXCLUDED.is_primary,
                            confidence = EXCLUDED.confidence, effective_from = EXCLUDED.effective_from,
                            raw_payload = EXCLUDED.raw_payload, updated_at = now(), deleted_at = NULL
                        """
                    ),
                    {
                        "id": stable_bigint_id(
                            "market_etf_mapping",
                            mapping["etf_code"],
                            mapping["mapping_group"],
                            mapping["mapping_code"],
                            mapping["source"],
                        ),
                        "etf_code": mapping["etf_code"],
                        "mapping_group": mapping["mapping_group"],
                        "mapping_code": mapping["mapping_code"],
                        "mapping_name": mapping["mapping_name"],
                        "is_primary": mapping.get("is_primary", False),
                        "confidence": mapping.get("confidence"),
                        "source": mapping["source"],
                        "raw_payload": _json_payload(mapping.get("raw_payload") or {}),
                    },
                )
        return len(mappings)

    def _snapshot_params(self, row: dict[str, Any], trade_date: date, source: str, captured_at, snapshot_minute) -> dict[str, Any]:
        return {
            "id": stable_bigint_id("market_etf_spot_snapshot", trade_date, snapshot_minute, row["code"], source),
            "trade_date": trade_date,
            "snapshot_minute": snapshot_minute,
            "captured_at": captured_at,
            "code": row["code"],
            "exchange": row["exchange"],
            "name": row["name"],
            "category": row["category"],
            "theme": row.get("theme"),
            "classification_source": row.get("classification_source", "fallback.name_keyword"),
            **{field: row.get(field) for field in (
                "price", "prev_close", "change_pct", "volume", "amount", "iopv", "premium_pct", "turnover_pct",
                "inside_volume", "outside_volume", "latest_shares", "main_net_inflow", "main_net_ratio",
                "super_large_net_inflow", "super_large_net_ratio", "large_net_inflow", "large_net_ratio",
                "medium_net_inflow", "medium_net_ratio", "small_net_inflow", "small_net_ratio",
            )},
            "source": source,
            "raw_payload": _json_payload(row.get("raw_payload") or {}),
        }


def _json_payload(value: Any) -> str:
    """PostgreSQL JSON rejects NaN/Infinity values that market APIs may emit."""
    return json.dumps(_json_safe(value), ensure_ascii=False, allow_nan=False, default=str)


def _json_safe(value: Any) -> Any:
    if isinstance(value, float):
        return value if math.isfinite(value) else None
    if isinstance(value, dict):
        return {str(key): _json_safe(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_json_safe(item) for item in value]
    return value
