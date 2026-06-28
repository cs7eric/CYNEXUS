from __future__ import annotations

from dataclasses import asdict
from datetime import date, datetime
from typing import Any, Protocol

import pandas as pd

from cynexus_data.sources.tdx.adjusted import stock_symbol_to_tdx_code


class IntradayDetailClient(Protocol):
    def get_quote(self, codes): ...
    def get_call_auction(self, code): ...
    def get_auction_0925(self, symbol, trade_date): ...
    def get_history_minute(self, *args, **kwargs): ...


def create_eltdx_intraday_client(timeout: float = 20.0):
    try:
        from eltdx import TdxClient  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for TDX intraday detail sync") from exc
    return TdxClient(timeout=timeout)


def fetch_stock_auction(client: IntradayDetailClient, symbol: str, trade_date: str | date | None = None, *, target_type: str | None = None) -> dict[str, Any]:
    code = _plain_code(symbol)
    tdx_code = stock_symbol_to_tdx_code(code, target_type=target_type)
    trading_date = _safe_date(trade_date)
    quote_rows = client.get_quote(tdx_code)
    quote = quote_rows[0] if isinstance(quote_rows, list) and quote_rows else quote_rows
    call_auction = client.get_call_auction(tdx_code)
    auction_0925 = client.get_auction_0925(code, trading_date.isoformat())

    auction_payload = _sanitize_jsonable(_object_to_dict(call_auction))
    points = _extract_rows(auction_payload.get("points") or [])
    auction_0925_payload = _sanitize_jsonable(_object_to_dict(auction_0925))
    point_rows = normalize_auction_points(
        symbol=code,
        tdx_code=tdx_code,
        trade_date=trading_date,
        points=points,
        auction_0925=auction_0925_payload,
    )
    return {
        "symbol": code,
        "tdx_code": tdx_code,
        "trade_date": trading_date.isoformat(),
        "quote": _sanitize_jsonable(_object_to_dict(quote)),
        "auction0925": auction_0925_payload,
        "points": point_rows,
        "raw": auction_payload,
    }


def fetch_stock_history_timeshare(
    client: IntradayDetailClient,
    symbol: str,
    trade_date: str | date,
    *,
    target_type: str | None = None,
) -> list[dict[str, Any]]:
    code = _plain_code(symbol)
    tdx_code = stock_symbol_to_tdx_code(code, target_type=target_type)
    trading_date = _safe_date(trade_date)
    rows = _call_client_variants(
        client,
        ["get_history_minute"],
        [
            ((tdx_code, trading_date), {}),
            ((tdx_code, trading_date.isoformat()), {}),
            ((tdx_code,), {"date": trading_date}),
            ((), {"code": tdx_code, "date": trading_date}),
        ],
    )
    normalized = normalize_timeshare_rows(symbol=code, tdx_code=tdx_code, rows=rows)
    if not normalized:
        raise ValueError("eltdx history minute returned no valid rows")
    return normalized


def normalize_auction_points(
    *,
    symbol: str,
    tdx_code: str,
    trade_date: date,
    points: list[dict[str, Any]],
    auction_0925: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for point in points:
        time_label = _time_label(point)
        if not time_label:
            continue
        phase = "opening" if time_label <= "09:25:00" else "closing" if time_label >= "14:57:00" else "continuous"
        rows.append(_auction_point_row(symbol, tdx_code, trade_date, time_label, phase, point))
    if auction_0925 and auction_0925.get("has_auction_0925") is not False:
        rows.append(_auction_point_row(symbol, tdx_code, trade_date, "09:25:00", "auction_0925", auction_0925))
    return rows


def normalize_timeshare_rows(*, symbol: str, tdx_code: str, rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    previous_volume = None
    volume_window: list[float] = []
    for row in rows:
        trade_time = _coerce_trade_time(row)
        if trade_time is None:
            continue
        price = _float(_get_value(row, "price", "close"))
        avg_price = _optional_float(_get_value(row, "avg_price", "average_price", "avg"))
        volume = _float(_get_value(row, "volume", "vol", "matched_volume", default=0))
        turnover = _float(_get_value(row, "turnover", "amount", "matched_amount", "matched_amount_estimated", default=0))
        volume_ratio = _volume_ratio(volume, previous_volume, volume_window)
        previous_volume = volume
        out.append(
            {
                "market": tdx_code[:2].upper(),
                "symbol": symbol,
                "code": symbol,
                "tdx_code": tdx_code,
                "trade_date": trade_time.date(),
                "trade_time": trade_time,
                "time_label": trade_time.strftime("%H:%M"),
                "price": round(price, 4),
                "avg_price": round(avg_price, 4) if avg_price is not None else None,
                "volume": volume,
                "turnover": turnover,
                "turnover_rate": _optional_float(_get_value(row, "turnover_rate")),
                "volume_ratio": volume_ratio,
                "source": "eltdx",
                "raw_payload": _sanitize_jsonable(row),
                "fetched_at": datetime.now(),
                "ingested_at": datetime.now(),
            }
        )
    return out


def auction_points_to_frame(rows: list[dict[str, Any]]) -> pd.DataFrame:
    return pd.DataFrame(rows, columns=_auction_columns())


def timeshare_to_frame(rows: list[dict[str, Any]]) -> pd.DataFrame:
    return pd.DataFrame(rows, columns=_timeshare_columns())


def _auction_point_row(symbol: str, tdx_code: str, trade_date: date, time_label: str, phase: str, point: dict[str, Any]) -> dict[str, Any]:
    trade_time = datetime.combine(trade_date, datetime.strptime(time_label, "%H:%M:%S").time())
    return {
        "market": tdx_code[:2].upper(),
        "symbol": symbol,
        "code": symbol,
        "tdx_code": tdx_code,
        "trade_date": trade_date,
        "trade_time": trade_time,
        "time_label": time_label,
        "phase": phase,
        "price": _optional_float(_get_value(point, "price", "match_price")),
        "matched_volume": _optional_float(_get_value(point, "matched_volume", "volume", "vol")),
        "matched_amount": _optional_float(_get_value(point, "matched_amount", "amount", "turnover", "matched_amount_estimated")),
        "unmatched_buy_volume": _optional_float(_get_value(point, "unmatched_buy_volume", "buy_volume", "buy")),
        "unmatched_sell_volume": _optional_float(_get_value(point, "unmatched_sell_volume", "sell_volume", "sell")),
        "source": "eltdx",
        "raw_payload": _sanitize_jsonable(point),
        "fetched_at": datetime.now(),
        "ingested_at": datetime.now(),
    }


def _call_client_variants(client: IntradayDetailClient, method_names: list[str], variants: list[tuple[tuple, dict]]) -> list[dict[str, Any]]:
    last_error = None
    for method_name in method_names:
        method = getattr(client, method_name, None)
        if not callable(method):
            continue
        for args, kwargs in variants:
            try:
                rows = _extract_rows(method(*args, **kwargs))
                if rows:
                    return rows
            except Exception as exc:  # noqa: BLE001
                last_error = exc
    if last_error is not None:
        raise last_error
    return []


def _extract_rows(response: Any) -> list[dict[str, Any]]:
    if response is None:
        return []
    rows = response
    for name in ["items", "rows", "points", "ticks"]:
        if hasattr(response, name):
            rows = getattr(response, name)
            break
    if callable(rows):
        rows = rows()
    if isinstance(rows, dict):
        rows = rows.get("items") or rows.get("rows") or rows.get("points") or [rows]
    if not isinstance(rows, (list, tuple)):
        return []
    return [_sanitize_jsonable(_object_to_dict(row)) for row in rows]


def _object_to_dict(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return value
    if hasattr(value, "__dataclass_fields__"):
        return asdict(value)
    raw = getattr(value, "__dict__", None)
    return raw if isinstance(raw, dict) else {}


def _coerce_trade_time(row: dict[str, Any]) -> datetime | None:
    for name in ["datetime", "trade_time", "trade_datetime", "time", "date", "day"]:
        value = row.get(name)
        if value is None:
            continue
        if isinstance(value, datetime):
            return value.replace(tzinfo=None)
        if isinstance(value, date):
            return datetime.combine(value, datetime.min.time())
        if isinstance(value, str) and value.strip():
            parsed = pd.to_datetime(value, errors="coerce")
            if pd.notna(parsed):
                return parsed.to_pydatetime().replace(tzinfo=None)
    return None


def _time_label(row: dict[str, Any]) -> str | None:
    value = _get_value(row, "time_label", "label", "time")
    if isinstance(value, datetime):
        return value.strftime("%H:%M:%S")
    if isinstance(value, str) and value.strip():
        text = value.strip()
        if text.count(":") == 2:
            parts = text.split(":")
            return f"{int(parts[0]):02d}:{int(parts[1]):02d}:{int(parts[2]):02d}"
        if len(text) == 5:
            hour, minute = text.split(":")
            return f"{int(hour):02d}:{int(minute):02d}:00"
        if len(text) >= 8:
            return text[:8]
    return None


def _sanitize_jsonable(value: Any):
    if isinstance(value, bytes):
        return value.hex()
    if isinstance(value, (date, datetime)):
        return value.isoformat()
    if isinstance(value, dict):
        return {key: _sanitize_jsonable(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_sanitize_jsonable(item) for item in value]
    return value


def _safe_date(value: str | date | None) -> date:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    if value:
        return datetime.strptime(str(value), "%Y-%m-%d").date()
    return datetime.now().date()


def _plain_code(symbol: str) -> str:
    text = str(symbol).strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        return text[2:]
    if "." in text:
        return text.split(".")[-1]
    return text.zfill(6)


def _get_value(obj: dict[str, Any], *names: str, default=None):
    for name in names:
        if name in obj and obj[name] is not None:
            return obj[name]
    return default


def _float(value: Any) -> float:
    out = _optional_float(value)
    return out if out is not None else 0.0


def _optional_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _volume_ratio(volume: float, previous_volume: float | None, window: list[float], *, window_size: int = 5) -> float | None:
    ratio = None
    if previous_volume and previous_volume > 0:
        ratio = round(volume / previous_volume, 4)
    window.append(volume)
    if len(window) > window_size:
        del window[0]
    return ratio


def _auction_columns() -> list[str]:
    return [
        "market",
        "symbol",
        "code",
        "tdx_code",
        "trade_date",
        "trade_time",
        "time_label",
        "phase",
        "price",
        "matched_volume",
        "matched_amount",
        "unmatched_buy_volume",
        "unmatched_sell_volume",
        "source",
        "raw_payload",
        "fetched_at",
        "ingested_at",
    ]


def _timeshare_columns() -> list[str]:
    return [
        "market",
        "symbol",
        "code",
        "tdx_code",
        "trade_date",
        "trade_time",
        "time_label",
        "price",
        "avg_price",
        "volume",
        "turnover",
        "turnover_rate",
        "volume_ratio",
        "source",
        "raw_payload",
        "fetched_at",
        "ingested_at",
    ]
