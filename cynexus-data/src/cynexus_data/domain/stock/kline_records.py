from __future__ import annotations

from datetime import datetime
from typing import Any

import pandas as pd

from cynexus_data.core.market_symbol import normalize_market_symbol, split_market_symbol


def daily_provider_rows_to_frame(symbol: str, rows: list[dict[str, Any]], *, source: str, unit_scale: int = 1) -> pd.DataFrame:
    market, code = split_market_symbol(symbol)
    normalized = normalize_market_symbol(symbol)
    now = pd.Timestamp.now(tz="UTC")
    records: list[dict[str, Any]] = []
    for row in rows:
        trade_date = row.get("trade_date")
        if trade_date is None and row.get("trade_time") is not None:
            trade_time = row["trade_time"]
            trade_date = trade_time.date() if hasattr(trade_time, "date") else str(trade_time)[:10]
        if trade_date is None:
            continue
        records.append(
            {
                "market": market,
                "symbol": code,
                "code": code,
                "tdx_code": normalized,
                "trade_date": pd.to_datetime(trade_date).date(),
                "open": float(row.get("open") or 0),
                "high": float(row.get("high") or 0),
                "low": float(row.get("low") or 0),
                "close": float(row.get("close") or 0),
                "amount": float(row.get("amount") or 0),
                "volume": int(float(row.get("volume") or 0)),
                "unit_scale": int(unit_scale),
                "source": source,
                "source_file": "",
                "source_file_mtime": _epoch(),
                "ingested_at": now,
            }
        )
    return pd.DataFrame.from_records(records)


def minute_provider_rows_to_frame(symbol: str, rows: list[dict[str, Any]], *, source: str, period: int = 1) -> pd.DataFrame:
    market, code = split_market_symbol(symbol)
    normalized = normalize_market_symbol(symbol)
    now = pd.Timestamp.now(tz="UTC")
    records: list[dict[str, Any]] = []
    for row in rows:
        trade_time = row.get("trade_time")
        if trade_time is None:
            continue
        ts = pd.to_datetime(trade_time).to_pydatetime()
        records.append(
            {
                "market": market,
                "symbol": code,
                "tdx_code": normalized,
                "period": int(period),
                "trade_time": ts,
                "trade_date": ts.date(),
                "open": float(row.get("open") or 0),
                "high": float(row.get("high") or 0),
                "low": float(row.get("low") or 0),
                "close": float(row.get("close") or 0),
                "amount": float(row.get("amount") or 0),
                "volume": int(float(row.get("volume") or 0)),
                "adj_factor": None,
                "source": source,
                "source_file": "",
                "source_file_mtime": _epoch(),
                "ingested_at": now,
            }
        )
    return pd.DataFrame.from_records(records)


def _epoch() -> datetime:
    return datetime.fromtimestamp(0)
