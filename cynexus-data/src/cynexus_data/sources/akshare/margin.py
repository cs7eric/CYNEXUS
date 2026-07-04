from __future__ import annotations

from datetime import date, datetime
from typing import Any


def fetch_margin_market_summary(ak: Any, trade_date: date) -> list[dict[str, Any]]:
    day = _date_param(trade_date)
    rows: list[dict[str, Any]] = []
    sse = _safe_fetch(lambda: ak.stock_margin_sse(start_date=day, end_date=day))
    rows.extend(normalize_margin_sse_summary(sse, fallback_trade_date=trade_date))
    szse = _safe_fetch(lambda: ak.stock_margin_szse(date=day))
    rows.extend(normalize_margin_szse_summary(szse, trade_date=trade_date))
    return rows


def fetch_margin_stock_detail(ak: Any, trade_date: date) -> list[dict[str, Any]]:
    day = _date_param(trade_date)
    rows: list[dict[str, Any]] = []
    sse = _safe_fetch(lambda: ak.stock_margin_detail_sse(date=day))
    rows.extend(normalize_margin_sse_detail(sse, fallback_trade_date=trade_date))
    szse = _safe_fetch(lambda: ak.stock_margin_detail_szse(date=day))
    rows.extend(normalize_margin_szse_detail(szse, trade_date=trade_date))
    return rows


def fetch_margin_account_stats(ak: Any, trade_date: date | None = None) -> list[dict[str, Any]]:
    frame = _safe_fetch(lambda: ak.stock_margin_account_info())
    return normalize_margin_account_stats(frame, trade_date=trade_date)


def fetch_latest_margin_trade_date(ak: Any, end_date: date | None = None) -> date | None:
    frame = _safe_fetch(lambda: ak.stock_margin_account_info())
    dates = []
    for row in _records(frame):
        row_date = _parse_date(row.get("日期"))
        if row_date is None:
            continue
        if end_date is not None and row_date > end_date:
            continue
        dates.append(row_date)
    return max(dates) if dates else None


def _safe_fetch(fetcher: Any) -> Any:
    try:
        return fetcher()
    except ValueError as exc:
        message = str(exc)
        if "Length mismatch" in message and "Expected axis has 0 elements" in message:
            return None
        raise


def normalize_margin_sse_summary(frame: Any, *, fallback_trade_date: date) -> list[dict[str, Any]]:
    return [
        {
            "trade_date": _parse_date(row.get("信用交易日期")) or fallback_trade_date,
            "exchange": "SSE",
            "source": "akshare.stock_margin_sse",
            "financing_balance": _yuan_to_yi(row.get("融资余额")),
            "financing_buy_amount": _yuan_to_yi(row.get("融资买入额")),
            "financing_repay_amount": None,
            "securities_lending_balance": _number(row.get("融券余量")),
            "securities_lending_balance_amount": _yuan_to_yi(row.get("融券余量金额")),
            "securities_lending_sell_volume": _number(row.get("融券卖出量")),
            "securities_lending_repay_volume": None,
            "margin_balance": _yuan_to_yi(row.get("融资融券余额")),
            "extra": {},
        }
        for row in _records(frame)
    ]


def normalize_margin_szse_summary(frame: Any, *, trade_date: date) -> list[dict[str, Any]]:
    return [
        {
            "trade_date": trade_date,
            "exchange": "SZSE",
            "source": "akshare.stock_margin_szse",
            "financing_balance": _number(row.get("融资余额")),
            "financing_buy_amount": _number(row.get("融资买入额")),
            "financing_repay_amount": None,
            "securities_lending_balance": _number(row.get("融券余量")),
            "securities_lending_balance_amount": _number(row.get("融券余额")),
            "securities_lending_sell_volume": _number(row.get("融券卖出量")),
            "securities_lending_repay_volume": None,
            "margin_balance": _number(row.get("融资融券余额")),
            "extra": {},
        }
        for row in _records(frame)
    ]


def normalize_margin_sse_detail(frame: Any, *, fallback_trade_date: date) -> list[dict[str, Any]]:
    return [
        {
            "trade_date": _parse_date(row.get("信用交易日期")) or fallback_trade_date,
            "exchange": "SSE",
            "stock_code": _stock_code(row.get("标的证券代码")),
            "stock_name": _text(row.get("标的证券简称")),
            "source": "akshare.stock_margin_detail_sse",
            "financing_balance": _number(row.get("融资余额")),
            "financing_buy_amount": _number(row.get("融资买入额")),
            "financing_repay_amount": _number(row.get("融资偿还额")),
            "securities_lending_balance": _number(row.get("融券余量")),
            "securities_lending_balance_amount": None,
            "securities_lending_sell_volume": _number(row.get("融券卖出量")),
            "securities_lending_repay_volume": _number(row.get("融券偿还量")),
            "margin_balance": None,
            "extra": {},
        }
        for row in _records(frame)
        if _stock_code(row.get("标的证券代码"))
    ]


def normalize_margin_szse_detail(frame: Any, *, trade_date: date) -> list[dict[str, Any]]:
    return [
        {
            "trade_date": trade_date,
            "exchange": "SZSE",
            "stock_code": _stock_code(row.get("证券代码")),
            "stock_name": _text(row.get("证券简称")),
            "source": "akshare.stock_margin_detail_szse",
            "financing_balance": _number(row.get("融资余额")),
            "financing_buy_amount": _number(row.get("融资买入额")),
            "financing_repay_amount": None,
            "securities_lending_balance": _number(row.get("融券余量")),
            "securities_lending_balance_amount": _number(row.get("融券余额")),
            "securities_lending_sell_volume": _number(row.get("融券卖出量")),
            "securities_lending_repay_volume": None,
            "margin_balance": _number(row.get("融资融券余额")),
            "extra": {},
        }
        for row in _records(frame)
        if _stock_code(row.get("证券代码"))
    ]


def normalize_margin_account_stats(frame: Any, *, trade_date: date | None = None) -> list[dict[str, Any]]:
    rows = []
    for row in _records(frame):
        row_date = _parse_date(row.get("日期"))
        if row_date is None or (trade_date is not None and row_date != trade_date):
            continue
        rows.append(
            {
                "trade_date": row_date,
                "source": "akshare.stock_margin_account_info",
                "financing_balance": _number(row.get("融资余额")),
                "securities_lending_balance": _number(row.get("融券余额")),
                "financing_buy_amount": _number(row.get("融资买入额")),
                "securities_lending_sell_amount": _number(row.get("融券卖出额")),
                "broker_count": _integer(row.get("证券公司数量")),
                "business_department_count": _integer(row.get("营业部数量")),
                "personal_investor_count": _integer(row.get("个人投资者数量")),
                "institution_investor_count": _integer(row.get("机构投资者数量")),
                "active_investor_count": _integer(row.get("参与交易的投资者数量")),
                "liability_investor_count": _integer(row.get("有融资融券负债的投资者数量")),
                "total_guarantee": _number(row.get("担保物总价值")),
                "average_guarantee_ratio": _number(row.get("平均维持担保比例")),
                "extra": {},
            }
        )
    return rows


def _records(frame: Any) -> list[dict[str, Any]]:
    if frame is None:
        return []
    empty = getattr(frame, "empty", False)
    if empty:
        return []
    if hasattr(frame, "to_dict"):
        return list(frame.to_dict(orient="records"))
    return list(frame)


def _date_param(value: date) -> str:
    return value.strftime("%Y%m%d")


def _parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    text = str(value).strip()
    if not text:
        return None
    if len(text) == 8 and text.isdigit():
        return date.fromisoformat(f"{text[:4]}-{text[4:6]}-{text[6:]}")
    return date.fromisoformat(text[:10])


def _number(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        import pandas as pd

        parsed = pd.to_numeric(pd.Series([str(value).replace(",", "")]), errors="coerce").iloc[0]
        if pd.isna(parsed):
            return None
        return float(parsed)
    except Exception:
        return None


def _yuan_to_yi(value: Any) -> float | None:
    parsed = _number(value)
    return round(parsed / 1e8, 6) if parsed is not None else None


def _integer(value: Any) -> int | None:
    parsed = _number(value)
    return int(parsed) if parsed is not None else None


def _stock_code(value: Any) -> str | None:
    text = _text(value)
    return text.zfill(6) if text else None


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).replace("&nbsp;", "").strip()
    return text or None
