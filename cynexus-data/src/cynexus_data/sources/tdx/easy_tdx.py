"""easy_tdx-backed online TDX and MAC source adapters.

The adapter keeps the optional dependency and protocol-specific objects at the
source boundary. Callers receive plain dictionaries or DataFrames that can be
normalized by the existing Cynexus domain and storage layers.
"""

from __future__ import annotations

from datetime import date, datetime
from threading import Lock
from typing import Any

import pandas as pd

from cynexus_data.core.market_symbol import normalize_market_symbol


_CLIENT_FACTORY_LOCK = Lock()
_PREFERRED_STANDARD_TDX_HOSTS = (
    "180.153.18.170",
    "115.238.56.198",
    "115.238.90.165",
    "218.75.126.9",
)


def _from_best_host(client_type: Any, *, timeout: float) -> Any:
    # `from_best_host()` pings every known TDX endpoint on every symbol.  The
    # archive can fetch thousands of symbols, so repeating that probe becomes
    # the dominant cost and the factory lock effectively serializes workers.
    # Reuse easy_tdx's persisted best host; the client itself still retains its
    # normal reconnect/failover behavior for command-level failures.
    from easy_tdx.config import get_best_host, get_port

    host = get_best_host()
    if host:
        return client_type(host, get_port(), timeout=timeout)
    with _CLIENT_FACTORY_LOCK:
        return client_type.from_best_host(timeout=timeout)


def fetch_kline_rows(
    symbol: str,
    *,
    period: int | str = 1,
    adjust: str = "none",
    limit: int = 500,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    """Fetch online K-line rows through easy_tdx MAC.

    MAC supports 1m and adjusted daily bars directly. The returned shape is
    the same provider-row shape consumed by the existing K-line adapters.
    """
    from easy_tdx import Adjust, MacClient, Market, Period

    market, code = _market_code(symbol, Market)
    period_value = _period(period, Period)
    adjust_value = _adjust(adjust, Adjust)
    with _from_best_host(MacClient, timeout=timeout) as client:
        frame = client.get_stock_kline(
            market,
            code,
            period_value,
            count=max(1, int(limit)),
            adjust=adjust_value,
        )
    return _kline_records(frame, source="easy_tdx.mac")


def fetch_history_timeshare_rows(
    symbol: str,
    trade_date: str | date,
    *,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    """Fetch one historical trading day's 1-minute timeshare data."""
    from easy_tdx import Market, TdxClient

    market, code = _market_code(symbol, Market)
    query_date = _date_int(trade_date)
    with _from_best_host(TdxClient, timeout=timeout) as client:
        frame = client.get_history_minute_time_data(market, code, query_date)

    from cynexus_data.sources.tdx.intraday import normalize_timeshare_rows

    raw_rows = _frame_records(frame)
    return normalize_timeshare_rows(
        symbol=code,
        tdx_code=f"{_market_prefix(market)}{code}",
        rows=raw_rows,
    )


def fetch_current_stock_fund_flow(
    symbol: str,
    *,
    trade_date: str | date | None = None,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    """Fetch current stock fund flow from standard TDX transaction data."""
    from easy_tdx import Market, TdxClient

    market, code = _market_code(symbol, Market)
    with _from_best_host(TdxClient, timeout=timeout) as client:
        flow = client.get_fund_flow(market, code)
        quote = client.get_security_quotes([(market, code)])
    rows = _flow_records(flow, code=code, trade_date=trade_date)
    if rows and quote is not None and not quote.empty:
        quote_row = _frame_records(quote)[0]
        rows[0].update(_quote_fields(quote_row))
    return rows


def fetch_current_stock_fund_flow_batch(
    symbols: list[str],
    *,
    trade_date: str | date | None = None,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    """Fetch current fund flow for many symbols on one standard connection."""
    from easy_tdx import Market, TdxClient

    resolved = [_market_code(symbol, Market) for symbol in symbols]
    output: list[dict[str, Any]] = []
    with _from_best_host(TdxClient, timeout=timeout) as client:
        for market, code in resolved:
            flow = client.get_fund_flow(market, code)
            rows = _flow_records(flow, code=code, trade_date=trade_date)
            if rows:
                quote = client.get_security_quotes([(market, code)])
                if quote is not None and not quote.empty:
                    rows[0].update(_quote_fields(_frame_records(quote)[0]))
            output.extend(rows)
    return output


def fetch_history_stock_fund_flow(
    symbol: str,
    *,
    count: int = 120,
    start: int = 0,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    """Fetch historical daily stock fund flow from standard TDX."""
    import asyncio

    from easy_tdx import AsyncTdxClient, Market
    from easy_tdx.commands.security_bars import GetSecurityBarsCmd
    from easy_tdx.config import get_best_host, get_known_hosts, save_best_host
    from easy_tdx.models.enums import KlineCategory

    market, code = _market_code(symbol, Market)
    requested = max(1, int(count))
    offset = int(start)

    async def _fetch() -> list[dict[str, Any]]:
        hosts = _standard_tdx_candidate_hosts(get_best_host(), get_known_hosts())
        for host in hosts:
            try:
                client = AsyncTdxClient(
                    host,
                    timeout=timeout,
                    auto_reconnect=False,
                    heartbeat_interval=0,
                )
                async with client:
                    bars = await client._execute(
                        GetSecurityBarsCmd(market, code, KlineCategory.DAY, offset, 1)
                    )
                    if not bars:
                        continue
                    frame = await client.get_history_fund_flow(market, code, offset, requested)
            except Exception:
                continue
            rows = _flow_records(frame, code=code)
            if rows:
                save_best_host(host)
                return rows
        return []

    return asyncio.run(_fetch())


def fetch_history_stock_fund_flow_payload(
    symbol: str,
    *,
    count: int = 120,
    timeout: float = 15.0,
) -> dict[str, Any]:
    """Return a persistence-ready history payload with source provenance."""
    rows = fetch_history_stock_fund_flow(symbol, count=count, timeout=timeout)
    return {
        "items": rows,
        "source": "easy_tdx.tdx",
        "mode": "history",
        "requested_limit": int(count),
        "history_complete": len(rows) >= min(int(count), 120),
    }


def fetch_board_list(board_type: str = "all", *, count: int = 10000, timeout: float = 15.0) -> list[dict[str, Any]]:
    from easy_tdx import BoardType, MacClient

    with _from_best_host(MacClient, timeout=timeout) as client:
        frame = client.get_board_list(_board_type(board_type, BoardType), count=max(1, int(count)))
    return _frame_records(frame)


def fetch_board_members(
    board_symbol: str,
    *,
    count: int = 100000,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    from easy_tdx import MacClient

    with _from_best_host(MacClient, timeout=timeout) as client:
        frame = client.get_board_members(board_symbol, count=max(1, int(count)))
    return _frame_records(frame)


def fetch_belong_board_rows(symbol: str, *, timeout: float = 15.0) -> list[dict[str, Any]]:
    """Fetch the industry/concept boards assigned to one stock."""
    from easy_tdx import MacClient, Market

    market, code = _market_code(symbol, Market)
    with _from_best_host(MacClient, timeout=timeout) as client:
        frame = client.get_belong_board(market, code)
    return _frame_records(frame)


def fetch_board_summary(board_symbol: str, *, timeout: float = 15.0) -> dict[str, Any]:
    from easy_tdx import MacClient

    with _from_best_host(MacClient, timeout=timeout) as client:
        summary = client.get_board_summary(board_symbol)
    result = dict(summary)
    members = result.get("members")
    result["members"] = _frame_records(members) if isinstance(members, pd.DataFrame) else members
    return result


def fetch_board_ranking(
    board_type: str = "hy",
    *,
    top_n: int = 50,
    sort_by: str = "change_pct",
    ascending: bool = False,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    from easy_tdx import BoardType, MacClient

    with _from_best_host(MacClient, timeout=timeout) as client:
        frame = client.get_board_ranking(
            _board_type(board_type, BoardType),
            top_n=max(1, int(top_n)),
            sort_by=sort_by,
            ascending=ascending,
        )
    return _frame_records(frame)


def fetch_board_change_ranking(
    board_type: str = "hy",
    *,
    days: int = 20,
    top_n: int | None = 50,
    target_date: int | None = None,
    ascending: bool = False,
    timeout: float = 15.0,
) -> list[dict[str, Any]]:
    from easy_tdx import BoardType, MacClient

    with _from_best_host(MacClient, timeout=timeout) as client:
        frame = client.get_board_change_ranking(
            _board_type(board_type, BoardType),
            target_date=target_date,
            days=max(1, int(days)),
            top_n=top_n,
            ascending=ascending,
        )
    return _frame_records(frame)


def _market_code(symbol: str, market_enum: Any) -> tuple[Any, str]:
    normalized = normalize_market_symbol(symbol)
    prefix = normalized[:2].lower()
    code = normalized[2:]
    market = {"sh": market_enum.SH, "sz": market_enum.SZ, "bj": market_enum.BJ}.get(prefix)
    if market is None:
        raise ValueError(f"unsupported A-share symbol: {symbol}")
    return market, code


def _period(value: int | str, period_enum: Any) -> Any:
    text = str(value).strip().lower()
    mapping = {
        "1": period_enum.MIN_1,
        "1m": period_enum.MIN_1,
        "5": period_enum.MIN_5,
        "5m": period_enum.MIN_5,
        "15": period_enum.MIN_15,
        "15m": period_enum.MIN_15,
        "30": period_enum.MIN_30,
        "30m": period_enum.MIN_30,
        "60": period_enum.MIN_60,
        "60m": period_enum.MIN_60,
        "day": period_enum.DAILY,
        "daily": period_enum.DAILY,
        "week": period_enum.WEEKLY,
        "weekly": period_enum.WEEKLY,
        "month": period_enum.MONTHLY,
        "monthly": period_enum.MONTHLY,
    }
    if text not in mapping:
        raise ValueError(f"unsupported easy_tdx period: {value}")
    return mapping[text]


def _adjust(value: str, adjust_enum: Any) -> Any:
    normalized = str(value or "none").strip().lower()
    mapping = {"none": adjust_enum.NONE, "qfq": adjust_enum.QFQ, "hfq": adjust_enum.HFQ}
    if normalized not in mapping:
        raise ValueError("adjust must be none, qfq, or hfq")
    return mapping[normalized]


def _board_type(value: str, board_enum: Any) -> Any:
    normalized = str(value or "all").strip().lower()
    mapping = {
        "all": board_enum.ALL,
        "hy": board_enum.HY,
        "hy2": board_enum.HY2,
        "gn": board_enum.GN,
        "fg": board_enum.FG,
        "dq": board_enum.DQ,
    }
    if normalized not in mapping:
        raise ValueError(f"unsupported board_type: {value}")
    return mapping[normalized]


def _standard_tdx_candidate_hosts(best_host: str | None, known_hosts: list[str]) -> list[str]:
    return _dedupe_hosts([best_host, *_PREFERRED_STANDARD_TDX_HOSTS, *known_hosts])


def _dedupe_hosts(hosts: list[str | None]) -> list[str]:
    seen: set[str] = set()
    output: list[str] = []
    for host in hosts:
        normalized = str(host or "").strip()
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        output.append(normalized)
    return output


def _kline_records(frame: Any, *, source: str) -> list[dict[str, Any]]:
    rows = _frame_records(frame)
    output: list[dict[str, Any]] = []
    for row in rows:
        trade_time = _coerce_datetime(row.get("datetime") or row.get("trade_time") or row.get("date"))
        if trade_time is None:
            continue
        output.append(
            {
                "trade_time": trade_time,
                "trade_date": trade_time.date(),
                "open": _number(row.get("open")),
                "high": _number(row.get("high")),
                "low": _number(row.get("low")),
                "close": _number(row.get("close")),
                "volume": _number(row.get("vol", row.get("volume")), default=0.0),
                "amount": _number(row.get("amount"), default=0.0),
                "source": source,
            }
        )
    return output


def _flow_records(frame: Any, *, code: str, trade_date: str | date | None = None) -> list[dict[str, Any]]:
    output: list[dict[str, Any]] = []
    for row in _frame_records(frame):
        values = {key: _number(row.get(key), default=0.0) for key in ("super_in", "super_out", "large_in", "large_out", "medium_in", "medium_out", "small_in", "small_out")}
        main_buy = values["super_in"] + values["large_in"]
        main_sell = values["super_out"] + values["large_out"]
        large_total = values["large_in"] + values["large_out"]
        main_net = values["super_in"] + values["large_in"] - values["super_out"] - values["large_out"]
        small_net = values["small_in"] - values["small_out"]
        effective_date = _row_date(row) or (_coerce_date(trade_date) if trade_date else None)
        output.append(
            {
                "code": code,
                "symbol": code,
                "trade_date": effective_date,
                "main_net": main_net,
                "active_net_amount_wan": main_net / 10000.0,
                "super_net": values["super_in"] - values["super_out"],
                "large_net": values["large_in"] - values["large_out"],
                "medium_net": values["medium_in"] - values["medium_out"],
                "small_net": small_net,
                "active_buy_ratio": _ratio(main_buy, main_buy + main_sell),
                "active_sell_ratio": _ratio(main_sell, main_buy + main_sell),
                "buy_big_ratio": _ratio(values["large_in"], large_total),
                "sell_big_ratio": _ratio(values["large_out"], large_total),
                "super_in": values["super_in"],
                "super_out": values["super_out"],
                "large_in": values["large_in"],
                "large_out": values["large_out"],
                "medium_in": values["medium_in"],
                "medium_out": values["medium_out"],
                "small_in": values["small_in"],
                "small_out": values["small_out"],
                "source": "easy_tdx.tdx",
                "raw": row,
            }
        )
    return output


def _quote_fields(row: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": row.get("name"),
        "last_price": row.get("close", row.get("price")),
        "pre_close": row.get("pre_close"),
        "open": row.get("open"),
        "high": row.get("high"),
        "low": row.get("low"),
        "change_pct": row.get("change_pct"),
        "turnover_rate": row.get("turnover_rate"),
        "volume_lots": row.get("vol", row.get("volume")),
        "amount_wan": _number(row.get("amount"), default=0.0) / 10000.0,
    }


def _ratio(numerator: float, denominator: float) -> float | None:
    return numerator / denominator if denominator else None


def _frame_records(frame: Any) -> list[dict[str, Any]]:
    if frame is None:
        return []
    if isinstance(frame, pd.DataFrame):
        if frame.empty:
            return []
        return [{str(key): _jsonable(value) for key, value in row.items()} for row in frame.to_dict(orient="records")]
    if isinstance(frame, list):
        return [dict(item) if isinstance(item, dict) else _object_dict(item) for item in frame]
    return [dict(frame)] if isinstance(frame, dict) else [_object_dict(frame)]


def _object_dict(value: Any) -> dict[str, Any]:
    raw = getattr(value, "__dict__", None)
    return dict(raw) if isinstance(raw, dict) else {"value": value}


def _jsonable(value: Any) -> Any:
    if isinstance(value, pd.Timestamp):
        return value.to_pydatetime()
    if isinstance(value, (datetime, date)):
        return value
    try:
        return value.item()
    except AttributeError:
        return value


def _coerce_datetime(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value.replace(tzinfo=None)
    if isinstance(value, date):
        return datetime.combine(value, datetime.min.time())
    if value in (None, ""):
        return None
    parsed = pd.to_datetime(value, errors="coerce")
    return parsed.to_pydatetime().replace(tzinfo=None) if pd.notna(parsed) else None


def _coerce_date(value: Any) -> date | None:
    parsed = _coerce_datetime(value)
    return parsed.date() if parsed else None


def _row_date(row: dict[str, Any]) -> date | None:
    if row.get("date"):
        return _coerce_date(row["date"])
    if all(row.get(key) is not None for key in ("year", "month", "day")):
        try:
            return date(int(row["year"]), int(row["month"]), int(row["day"]))
        except ValueError:
            return None
    return _coerce_date(row.get("datetime") or row.get("trade_date"))


def _date_int(value: str | date) -> int:
    parsed = _coerce_date(value)
    if parsed is None:
        raise ValueError(f"invalid trade date: {value}")
    return parsed.year * 10000 + parsed.month * 100 + parsed.day


def _number(value: Any, *, default: float | None = None) -> float | None:
    if value in (None, "", "--", "-"):
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _market_prefix(market: Any) -> str:
    return {0: "sz", 1: "sh", 2: "bj"}.get(int(market), "sh")
