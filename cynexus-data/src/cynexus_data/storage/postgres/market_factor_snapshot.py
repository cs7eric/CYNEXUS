from __future__ import annotations

import json
from datetime import datetime, time, timezone, timedelta
from typing import Any

from sqlalchemy import text

from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


class MarketFactorSnapshotRepository:
    SOURCE = "clickhouse.daily_raw"
    VERSION = "v1"

    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def replace_range(self, rows: list[dict[str, Any]], *, start_date: str, end_date: str, batch_size: int = 2_000) -> int:
        """Replace a bounded date range without building an unbounded PostgreSQL bind set."""
        if batch_size < 1:
            raise ValueError("batch_size must be positive")
        records = [_record(row) for row in rows]
        sql = text(f"""
            INSERT INTO {self.client.schema}.a_stock_daily_factor_snapshot
            (id, code, market, trade_date, close, amount, turnover_pct, feature_payload, feature_version, source, coverage, available_at, updated_at)
            VALUES (:id, :code, :market, :trade_date, :close, :amount, :turnover_pct, CAST(:feature_payload AS jsonb), :feature_version, CAST(:source AS jsonb), CAST(:coverage AS jsonb), :available_at, now())
            ON CONFLICT (code, market, trade_date, feature_version) DO UPDATE SET
              close = EXCLUDED.close, amount = EXCLUDED.amount, turnover_pct = EXCLUDED.turnover_pct,
              feature_payload = EXCLUDED.feature_payload, source = EXCLUDED.source, coverage = EXCLUDED.coverage,
              available_at = EXCLUDED.available_at, updated_at = now()
        """)
        with self.client.session() as db:
            db.execute(text(f"""DELETE FROM {self.client.schema}.a_stock_daily_factor_snapshot
                              WHERE feature_version = :version AND trade_date BETWEEN :start_date AND :end_date"""),
                       {"version": self.VERSION, "start_date": start_date, "end_date": end_date})
            for offset in range(0, len(records), batch_size):
                db.execute(sql, records[offset:offset + batch_size])
        return len(records)

    def count_rows(self, *, start_date: str, end_date: str) -> int:
        with self.client.engine.connect() as connection:
            row = connection.execute(text(f"""
                SELECT count(*)
                  FROM {self.client.schema}.a_stock_daily_factor_snapshot
                 WHERE feature_version = :version
                   AND trade_date BETWEEN :start_date AND :end_date
            """), {"version": self.VERSION, "start_date": start_date, "end_date": end_date}).scalar_one()
        return int(row)


def _record(row: dict[str, Any]) -> dict[str, Any]:
    code = str(row["code"]).strip()
    trade_date = str(row["trade_date"])[:10]
    market = str(row.get("market") or "UNKNOWN")[:8]
    if len(code) != 6 or not code.isdigit():
        raise ValueError(f"factor snapshot requires a six-digit stock code: {row}")
    payload = _primitive_payload(row)
    return {
        "id": stable_bigint_id("a_stock_daily_factor_snapshot", code, market, trade_date, MarketFactorSnapshotRepository.VERSION),
        "code": code, "market": market, "trade_date": trade_date, "close": row.get("close_price"), "amount": row.get("amount"),
        "turnover_pct": row.get("turnover_rate_pct"), "feature_payload": json.dumps(payload, default=str, separators=(",", ":")),
        "feature_version": MarketFactorSnapshotRepository.VERSION,
        "source": json.dumps({"name": MarketFactorSnapshotRepository.SOURCE, "source_trade_date": trade_date}, separators=(",", ":")),
        "coverage": json.dumps({"daily_raw": "AVAILABLE"}, separators=(",", ":")),
        # A daily bar cannot be used before the Shanghai close of its own
        # trade date. Persist that availability boundary instead of the job
        # execution time so historical Study leakage checks remain stable.
        "available_at": datetime.combine(datetime.fromisoformat(trade_date).date(), time(15), tzinfo=timezone(timedelta(hours=8))),
    }


def _primitive_payload(row: dict[str, Any]) -> dict[str, Any]:
    """Persist reusable observations, never a hand-authored factor conclusion."""
    open_price = _number(row.get("open_price"))
    high_price = _number(row.get("high_price"))
    low_price = _number(row.get("low_price"))
    close_price = _number(row.get("close_price"))
    previous_close = _number(row.get("prev_close"))
    body_pct = None
    upper_shadow_pct = None
    lower_shadow_pct = None
    if high_price is not None and low_price is not None and high_price > low_price and open_price is not None and close_price is not None:
        spread = high_price - low_price
        body_pct = abs(close_price - open_price) / spread
        upper_shadow_pct = (high_price - max(open_price, close_price)) / spread
        lower_shadow_pct = (min(open_price, close_price) - low_price) / spread
    return {
        "stock.open": open_price, "stock.high": high_price, "stock.low": low_price, "stock.close": close_price,
        "stock.volume": _number(row.get("volume")), "stock.amount": _number(row.get("amount")),
        "stock.turnover_pct": _number(row.get("turnover_rate_pct")), "stock.return_1d": _number(row.get("change_pct")),
        "stock.amplitude_pct": _number(row.get("amplitude_pct")), "stock.prev_close": previous_close,
        "stock.candlestick.body_pct": body_pct, "stock.candlestick.upper_shadow_pct": upper_shadow_pct,
        "stock.candlestick.lower_shadow_pct": lower_shadow_pct, "stock.total_market_cap": _number(row.get("total_market_cap")),
        "stock.float_market_cap": _number(row.get("float_market_cap")),
        "stock.return_3d": _number(row.get("return_3d")), "stock.return_5d": _number(row.get("return_5d")),
        "stock.return_10d": _number(row.get("return_10d")), "stock.return_20d": _number(row.get("return_20d")),
        "stock.return_60d": _number(row.get("return_60d")), "stock.return_120d": _number(row.get("return_120d")),
        "stock.return_252d": _number(row.get("return_252d")),
        "stock.ma5": _number(row.get("ma5")), "stock.ma10": _number(row.get("ma10")),
        "stock.ma20": _number(row.get("ma20")), "stock.ma60": _number(row.get("ma60")),
        "stock.close_vs_ma5": _ratio(close_price, row.get("ma5")), "stock.close_vs_ma10": _ratio(close_price, row.get("ma10")),
        "stock.close_vs_ma20": _ratio(close_price, row.get("ma20")), "stock.close_vs_ma60": _ratio(close_price, row.get("ma60")),
        "stock.volume_vs_ma5": _number(row.get("volume_vs_ma5")), "stock.volume_vs_ma10": _number(row.get("volume_vs_ma10")),
        "stock.volume_vs_ma20": _number(row.get("volume_vs_ma20")), "stock.volume_vs_ma60": _number(row.get("volume_vs_ma60")),
    }


def _number(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _ratio(value: Any, baseline: Any) -> float | None:
    numerator = _number(value)
    denominator = _number(baseline)
    if numerator is None or denominator in (None, 0):
        return None
    return numerator / denominator - 1
