from __future__ import annotations

import json
from datetime import date
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260704_create_margin_tables.sql"


class MarginRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def replace_market_summary(self, *, trade_date: date, rows: list[dict[str, Any]]) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.margin_market_summary
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.margin_market_summary (
                            id, trade_date, exchange, source, financing_balance, financing_buy_amount,
                            financing_repay_amount, securities_lending_balance, securities_lending_balance_amount,
                            securities_lending_sell_volume, securities_lending_repay_volume, margin_balance,
                            extra, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :exchange, :source, :financing_balance, :financing_buy_amount,
                            :financing_repay_amount, :securities_lending_balance, :securities_lending_balance_amount,
                            :securities_lending_sell_volume, :securities_lending_repay_volume, :margin_balance,
                            CAST(:extra AS jsonb), now(), NULL
                        )
                        ON CONFLICT (trade_date, exchange, source) WHERE deleted_at IS NULL DO UPDATE SET
                            financing_balance = EXCLUDED.financing_balance,
                            financing_buy_amount = EXCLUDED.financing_buy_amount,
                            financing_repay_amount = EXCLUDED.financing_repay_amount,
                            securities_lending_balance = EXCLUDED.securities_lending_balance,
                            securities_lending_balance_amount = EXCLUDED.securities_lending_balance_amount,
                            securities_lending_sell_volume = EXCLUDED.securities_lending_sell_volume,
                            securities_lending_repay_volume = EXCLUDED.securities_lending_repay_volume,
                            margin_balance = EXCLUDED.margin_balance,
                            extra = EXCLUDED.extra,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    _market_summary_params(row, trade_date=trade_date),
                )
        return len(rows)

    def replace_stock_detail(self, *, trade_date: date, rows: list[dict[str, Any]]) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.margin_stock_detail
                       SET deleted_at = now(), updated_at = now()
                     WHERE trade_date = :trade_date AND deleted_at IS NULL
                    """
                ),
                {"trade_date": trade_date},
            )
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.margin_stock_detail (
                            id, trade_date, exchange, stock_code, stock_name, source, financing_balance,
                            financing_buy_amount, financing_repay_amount, securities_lending_balance,
                            securities_lending_balance_amount, securities_lending_sell_volume,
                            securities_lending_repay_volume, margin_balance, extra, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :exchange, :stock_code, :stock_name, :source, :financing_balance,
                            :financing_buy_amount, :financing_repay_amount, :securities_lending_balance,
                            :securities_lending_balance_amount, :securities_lending_sell_volume,
                            :securities_lending_repay_volume, :margin_balance, CAST(:extra AS jsonb), now(), NULL
                        )
                        ON CONFLICT (trade_date, exchange, stock_code, source) WHERE deleted_at IS NULL DO UPDATE SET
                            stock_name = EXCLUDED.stock_name,
                            financing_balance = EXCLUDED.financing_balance,
                            financing_buy_amount = EXCLUDED.financing_buy_amount,
                            financing_repay_amount = EXCLUDED.financing_repay_amount,
                            securities_lending_balance = EXCLUDED.securities_lending_balance,
                            securities_lending_balance_amount = EXCLUDED.securities_lending_balance_amount,
                            securities_lending_sell_volume = EXCLUDED.securities_lending_sell_volume,
                            securities_lending_repay_volume = EXCLUDED.securities_lending_repay_volume,
                            margin_balance = EXCLUDED.margin_balance,
                            extra = EXCLUDED.extra,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    _stock_detail_params(row, trade_date=trade_date),
                )
        return len(rows)

    def replace_account_stats(self, *, rows: list[dict[str, Any]]) -> int:
        if not rows:
            return 0
        with self.client.session() as db:
            for row in rows:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.margin_account_stats (
                            id, trade_date, source, financing_balance, securities_lending_balance,
                            financing_buy_amount, securities_lending_sell_amount, broker_count,
                            business_department_count, personal_investor_count, institution_investor_count,
                            active_investor_count, liability_investor_count, total_guarantee,
                            average_guarantee_ratio, extra, updated_at, deleted_at
                        ) VALUES (
                            :id, :trade_date, :source, :financing_balance, :securities_lending_balance,
                            :financing_buy_amount, :securities_lending_sell_amount, :broker_count,
                            :business_department_count, :personal_investor_count, :institution_investor_count,
                            :active_investor_count, :liability_investor_count, :total_guarantee,
                            :average_guarantee_ratio, CAST(:extra AS jsonb), now(), NULL
                        )
                        ON CONFLICT (trade_date, source) WHERE deleted_at IS NULL DO UPDATE SET
                            financing_balance = EXCLUDED.financing_balance,
                            securities_lending_balance = EXCLUDED.securities_lending_balance,
                            financing_buy_amount = EXCLUDED.financing_buy_amount,
                            securities_lending_sell_amount = EXCLUDED.securities_lending_sell_amount,
                            broker_count = EXCLUDED.broker_count,
                            business_department_count = EXCLUDED.business_department_count,
                            personal_investor_count = EXCLUDED.personal_investor_count,
                            institution_investor_count = EXCLUDED.institution_investor_count,
                            active_investor_count = EXCLUDED.active_investor_count,
                            liability_investor_count = EXCLUDED.liability_investor_count,
                            total_guarantee = EXCLUDED.total_guarantee,
                            average_guarantee_ratio = EXCLUDED.average_guarantee_ratio,
                            extra = EXCLUDED.extra,
                            updated_at = now(),
                            deleted_at = NULL
                        """
                    ),
                    _account_stats_params(row),
                )
        return len(rows)


def _market_summary_params(row: dict[str, Any], *, trade_date: date) -> dict[str, Any]:
    return _base_params(row, trade_date=trade_date)


def _stock_detail_params(row: dict[str, Any], *, trade_date: date) -> dict[str, Any]:
    payload = _base_params(row, trade_date=trade_date)
    payload["stock_code"] = row.get("stock_code")
    payload["stock_name"] = row.get("stock_name")
    return payload


def _account_stats_params(row: dict[str, Any]) -> dict[str, Any]:
    payload = dict(row)
    payload["id"] = next_bigint_id()
    payload["extra"] = json.dumps(row.get("extra") or {}, ensure_ascii=False, default=str)
    for key in (
        "financing_balance",
        "securities_lending_balance",
        "financing_buy_amount",
        "securities_lending_sell_amount",
        "broker_count",
        "business_department_count",
        "personal_investor_count",
        "institution_investor_count",
        "active_investor_count",
        "liability_investor_count",
        "total_guarantee",
        "average_guarantee_ratio",
    ):
        payload.setdefault(key, None)
    return payload


def _base_params(row: dict[str, Any], *, trade_date: date) -> dict[str, Any]:
    payload = dict(row)
    payload["id"] = next_bigint_id()
    payload["trade_date"] = row.get("trade_date") or trade_date
    payload["extra"] = json.dumps(row.get("extra") or {}, ensure_ascii=False, default=str)
    for key in (
        "financing_balance",
        "financing_buy_amount",
        "financing_repay_amount",
        "securities_lending_balance",
        "securities_lending_balance_amount",
        "securities_lending_sell_volume",
        "securities_lending_repay_volume",
        "margin_balance",
    ):
        payload.setdefault(key, None)
    return payload
