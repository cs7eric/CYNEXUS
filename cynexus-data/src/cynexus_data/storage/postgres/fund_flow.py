from __future__ import annotations

import json
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_sector_fund_flow_tables.sql"
STOCK_DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_stock_fund_flow_tables.sql"


class SectorFundFlowRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_snapshot(
        self,
        *,
        trade_date: date,
        records: list[dict[str, Any]],
        scope: str = "industry",
        source: str = "ths.10jqka.com.cn",
        source_type: str = "crawler",
        total_pages: int | None = None,
        page_row_counts: list[int] | None = None,
        fetched_at: datetime | None = None,
        extra: dict[str, Any] | None = None,
        status: str = "success",
    ) -> int:
        captured_at = fetched_at or datetime.now(timezone.utc)
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.sector_fund_flow_daily_snapshot
                       SET deleted_at = now(), updated_at = now()
                     WHERE scope = :scope AND trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"scope": scope, "trade_date": trade_date},
            )
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.sector_fund_flow_capture_batch
                       SET deleted_at = now(), updated_at = now()
                     WHERE scope = :scope AND trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"scope": scope, "trade_date": trade_date},
            )
            batch_id = db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.sector_fund_flow_capture_batch (
                        scope, trade_date, source_type, status, source, total_pages, row_count,
                        page_row_counts, fetched_at, extra, updated_at
                    ) VALUES (
                        :scope, :trade_date, :source_type, :status, :source, :total_pages, :row_count,
                        CAST(:page_row_counts AS jsonb), :fetched_at, CAST(:extra AS jsonb), now()
                    )
                    RETURNING batch_id
                    """
                ),
                {
                    "scope": scope,
                    "trade_date": trade_date,
                    "source_type": source_type,
                    "status": status,
                    "source": source,
                    "total_pages": total_pages,
                    "row_count": len(records),
                    "page_row_counts": json.dumps(page_row_counts or [], ensure_ascii=False),
                    "fetched_at": captured_at,
                    "extra": json.dumps(extra or {}, ensure_ascii=False, default=str),
                },
            ).scalar_one()
            for record in records:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.sector_fund_flow_daily_snapshot (
                            batch_id, scope, trade_date, sector_code, sector_name, rank, change_pct,
                            inflow, outflow, net, company_count, leader_stock, leader_change,
                            leader_price, source, source_type, captured_at, extra, updated_at
                        ) VALUES (
                            :batch_id, :scope, :trade_date, :sector_code, :sector_name, :rank, :change_pct,
                            :inflow, :outflow, :net, :company_count, :leader_stock, :leader_change,
                            :leader_price, :source, :source_type, :captured_at, CAST(:extra AS jsonb), now()
                        )
                        """
                    ),
                    _record_params(record, batch_id=batch_id, captured_at=captured_at),
                )
        return len(records)

    def get_daily_snapshot(self, trade_date: date, *, scope: str = "industry") -> list[dict[str, Any]]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT trade_date, sector_code, sector_name, rank, change_pct, inflow, outflow, net,
                           company_count, leader_stock, leader_change, leader_price, source, source_type, captured_at
                      FROM {self.client.schema}.sector_fund_flow_daily_snapshot
                     WHERE scope = :scope AND trade_date = :trade_date AND deleted_at IS NULL
                     ORDER BY net DESC NULLS LAST, rank ASC NULLS LAST
                    """
                ),
                {"scope": scope, "trade_date": trade_date},
            ).mappings().all()
        return [dict(row) for row in rows]


def _record_params(record: dict[str, Any], *, batch_id: Any, captured_at: datetime) -> dict[str, Any]:
    payload = dict(record)
    payload["batch_id"] = batch_id
    payload["captured_at"] = payload.get("captured_at") or captured_at
    payload["extra"] = json.dumps(payload.get("extra") or {}, ensure_ascii=False, default=str)
    return payload


class StockFundFlowRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(STOCK_DDL_PATH))

    def replace_snapshot(
        self,
        *,
        trade_date: date,
        records: list[dict[str, Any]],
        source: str = "tencent.qt",
        source_type: str = "crawler",
        requested_codes: list[str] | None = None,
        fetched_at: datetime | None = None,
        extra: dict[str, Any] | None = None,
        status: str = "success",
    ) -> int:
        captured_at = fetched_at or datetime.now(timezone.utc)
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.stock_fund_flow_snapshot
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date AND source = :source AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date, "source": source},
            )
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.stock_fund_flow_capture_batch
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date AND source = :source AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date, "source": source},
            )
            batch_id = db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.stock_fund_flow_capture_batch (
                        trade_date, source_type, status, source, row_count, requested_codes,
                        fetched_at, extra, updated_at
                    ) VALUES (
                        :trade_date, :source_type, :status, :source, :row_count, CAST(:requested_codes AS jsonb),
                        :fetched_at, CAST(:extra AS jsonb), now()
                    )
                    RETURNING batch_id
                    """
                ),
                {
                    "trade_date": trade_date,
                    "source_type": source_type,
                    "status": status,
                    "source": source,
                    "row_count": len(records),
                    "requested_codes": json.dumps(requested_codes or [], ensure_ascii=False),
                    "fetched_at": captured_at,
                    "extra": json.dumps(extra or {}, ensure_ascii=False, default=str),
                },
            ).scalar_one()
            for record in records:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.stock_fund_flow_snapshot (
                            batch_id, trade_date, code, symbol, name, last_price, pre_close, open, high, low,
                            change, change_pct, volume_lots, amount_wan, turnover_rate, outer_disc_lots,
                            inside_dish_lots, active_net_lots, active_net_amount_wan, active_buy_ratio,
                            active_sell_ratio, buy_big_ratio, buy_small_ratio, sell_big_ratio, sell_small_ratio,
                            pe, pb, amplitude, float_market_cap_yi, total_market_cap_yi, source, source_type,
                            captured_at, extra, updated_at
                        ) VALUES (
                            :batch_id, :trade_date, :code, :symbol, :name, :last_price, :pre_close, :open, :high, :low,
                            :change, :change_pct, :volume_lots, :amount_wan, :turnover_rate, :outer_disc_lots,
                            :inside_dish_lots, :active_net_lots, :active_net_amount_wan, :active_buy_ratio,
                            :active_sell_ratio, :buy_big_ratio, :buy_small_ratio, :sell_big_ratio, :sell_small_ratio,
                            :pe, :pb, :amplitude, :float_market_cap_yi, :total_market_cap_yi, :source, :source_type,
                            :captured_at, CAST(:extra AS jsonb), now()
                        )
                        """
                    ),
                    _stock_record_params(record, batch_id=batch_id, captured_at=captured_at),
                )
        return len(records)

    def get_daily_snapshot(self, trade_date: date, *, source: str = "tencent.qt", limit: int | None = None) -> list[dict[str, Any]]:
        limit_sql = "LIMIT :limit" if limit is not None else ""
        params: dict[str, Any] = {"trade_date": trade_date, "source": source}
        if limit is not None:
            params["limit"] = limit
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT trade_date, code, symbol, name, last_price, change_pct, volume_lots, amount_wan,
                           active_net_lots, active_net_amount_wan, active_buy_ratio, active_sell_ratio,
                           buy_big_ratio, buy_small_ratio, sell_big_ratio, sell_small_ratio, source, source_type, captured_at
                      FROM {self.client.schema}.stock_fund_flow_snapshot
                     WHERE trade_date = :trade_date AND source = :source AND deleted_at IS NULL
                     ORDER BY active_net_amount_wan DESC NULLS LAST
                     {limit_sql}
                    """
                ),
                params,
            ).mappings().all()
        return [dict(row) for row in rows]


def _stock_record_params(record: dict[str, Any], *, batch_id: Any, captured_at: datetime) -> dict[str, Any]:
    payload = dict(record)
    payload["batch_id"] = batch_id
    payload["captured_at"] = payload.get("captured_at") or captured_at
    payload["extra"] = json.dumps(payload.get("extra") or {}, ensure_ascii=False, default=str)
    return payload
