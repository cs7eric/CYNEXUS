from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.domain.stock.turnover import latest_turnover_entry
from cynexus_data.storage.postgres.client import PostgresClient


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260628_create_stock_turnover_tables.sql"


class StockTurnoverRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        self.client.apply_sql_file(Path(DDL_PATH))

    def upsert_series(self, series: dict[str, Any], *, source: str | None = None) -> dict[str, Any]:
        entries = series.get("entries") if isinstance(series.get("entries"), list) else []
        latest = latest_turnover_entry(series) or {}
        payload_source = source or str(series.get("source") or "clickhouse+eltdx")
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.stock_turnover_series_snapshot (
                        symbol, target_type, period, adjust, circulating_shares, total_shares,
                        latest_trade_date, latest_turnover_rate, entry_count, source, payload,
                        generated_at, updated_at, deleted_at
                    ) VALUES (
                        :symbol, :target_type, :period, :adjust, :circulating_shares, :total_shares,
                        :latest_trade_date, :latest_turnover_rate, :entry_count, :source, CAST(:payload AS jsonb),
                        now(), now(), NULL
                    )
                    ON CONFLICT (symbol, target_type, period, adjust, source) DO UPDATE SET
                        circulating_shares = EXCLUDED.circulating_shares,
                        total_shares = EXCLUDED.total_shares,
                        latest_trade_date = EXCLUDED.latest_trade_date,
                        latest_turnover_rate = EXCLUDED.latest_turnover_rate,
                        entry_count = EXCLUDED.entry_count,
                        payload = EXCLUDED.payload,
                        generated_at = EXCLUDED.generated_at,
                        updated_at = now(),
                        deleted_at = NULL
                    """
                ),
                {
                    "symbol": str(series.get("symbol") or ""),
                    "target_type": str(series.get("target_type") or "stock"),
                    "period": str(series.get("period") or "1d"),
                    "adjust": str(series.get("adjust") or "qfq"),
                    "circulating_shares": series.get("circulating_shares"),
                    "total_shares": series.get("total_shares"),
                    "latest_trade_date": latest.get("trade_date"),
                    "latest_turnover_rate": latest.get("turnover_rate"),
                    "entry_count": len(entries),
                    "source": payload_source,
                    "payload": json.dumps(series, ensure_ascii=False, default=str),
                },
            )
        return self.get_series(
            symbol=str(series.get("symbol") or ""),
            target_type=str(series.get("target_type") or "stock"),
            period=str(series.get("period") or "1d"),
            adjust=str(series.get("adjust") or "qfq"),
            source=payload_source,
        ) or {
            "symbol": series.get("symbol"),
            "target_type": series.get("target_type") or "stock",
            "period": series.get("period") or "1d",
            "adjust": series.get("adjust") or "qfq",
            "source": payload_source,
            "payload": series,
            "entry_count": len(entries),
        }

    def get_series(
        self,
        *,
        symbol: str,
        target_type: str = "stock",
        period: str = "1d",
        adjust: str = "qfq",
        source: str = "clickhouse+eltdx",
    ) -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT symbol, target_type, period, adjust, source, payload, entry_count,
                           latest_trade_date, latest_turnover_rate, generated_at
                      FROM {self.client.schema}.stock_turnover_series_snapshot
                     WHERE symbol = :symbol
                       AND target_type = :target_type
                       AND period = :period
                       AND adjust = :adjust
                       AND source = :source
                       AND deleted_at IS NULL
                    """
                ),
                {"symbol": symbol, "target_type": target_type, "period": period, "adjust": adjust, "source": source},
            ).mappings().first()
        return dict(row) if row else None
