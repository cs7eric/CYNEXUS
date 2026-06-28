from __future__ import annotations

from datetime import date, datetime, timezone
from typing import Any


def estimate_turnover_rate(volume_lots: float | int | None, circulating_shares: float | int | None) -> float:
    if not volume_lots or not circulating_shares:
        return 0.0
    return round(float(volume_lots) * 10000.0 / float(circulating_shares), 4)


def build_turnover_series(
    *,
    symbol: str,
    target_type: str = "stock",
    period: str = "1d",
    adjust: str = "qfq",
    bars: list[dict[str, Any]],
    circulating_shares: float | None,
    total_shares: float | None = None,
    source: str = "clickhouse+eltdx",
) -> dict[str, Any]:
    entries = [_entry_from_bar(bar, circulating_shares=circulating_shares) for bar in bars]
    return {
        "symbol": str(symbol),
        "target_type": str(target_type or "stock"),
        "period": str(period or "1d"),
        "adjust": str(adjust or "qfq"),
        "circulating_shares": circulating_shares,
        "total_shares": total_shares,
        "source": source,
        "entries": entries,
        "updated_at": datetime.now(timezone.utc).isoformat(),
    }


def latest_turnover_entry(series: dict[str, Any]) -> dict[str, Any] | None:
    entries = series.get("entries") if isinstance(series, dict) else None
    if not isinstance(entries, list) or not entries:
        return None
    latest = entries[-1]
    return dict(latest) if isinstance(latest, dict) else None


def _entry_from_bar(bar: dict[str, Any], *, circulating_shares: float | None) -> dict[str, Any]:
    trade_date = _trade_date(bar)
    volume = _float_or_zero(bar.get("volume"))
    amount = _float_or_zero(bar.get("amount") or bar.get("turnover"))
    timestamp = _int_or_zero(bar.get("timestamp"))
    return {
        "trade_date": trade_date,
        "timestamp": timestamp,
        "turnover_rate": estimate_turnover_rate(volume, circulating_shares),
        "volume": volume,
        "amount": amount,
        "circulating_shares": circulating_shares,
    }


def _trade_date(bar: dict[str, Any]) -> str | None:
    value = bar.get("trade_date") or bar.get("date")
    if value:
        return str(value)[:10]
    timestamp = _int_or_zero(bar.get("timestamp"))
    if timestamp > 0:
        return datetime.fromtimestamp(timestamp / 1000.0, tz=timezone.utc).date().isoformat()
    return None


def _float_or_zero(value: Any) -> float:
    if value in (None, ""):
        return 0.0
    return float(value)


def _int_or_zero(value: Any) -> int:
    if value in (None, ""):
        return 0
    return int(float(value))
