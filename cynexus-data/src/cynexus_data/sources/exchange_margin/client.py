from __future__ import annotations

import time
from datetime import date, timedelta
from io import BytesIO
from typing import Any

import pandas as pd
import requests

from cynexus_data.sources.akshare.margin import (
    normalize_margin_sse_detail,
    normalize_margin_sse_summary,
    normalize_margin_szse_detail,
    normalize_margin_szse_summary,
)


class ExchangeMarginClient:
    def __init__(self, session: requests.Session | None = None, *, timeout: float = 15.0, retry: int = 3) -> None:
        self.session = session or requests.Session()
        self.timeout = timeout
        self.retry = retry

    def fetch_margin_market_summary(self, trade_date: date) -> list[dict[str, Any]]:
        rows = normalize_margin_sse_summary(_fetch_sse_summary(self.session, trade_date, timeout=self.timeout, retry=self.retry), fallback_trade_date=trade_date)
        rows += normalize_margin_szse_summary(_fetch_szse_summary(self.session, trade_date, timeout=self.timeout, retry=self.retry), trade_date=trade_date)
        return [_with_exchange_source(row) for row in rows]

    def fetch_margin_stock_detail(self, trade_date: date) -> list[dict[str, Any]]:
        rows = normalize_margin_sse_detail(_fetch_sse_detail(self.session, trade_date, timeout=self.timeout, retry=self.retry), fallback_trade_date=trade_date)
        rows += normalize_margin_szse_detail(_fetch_szse_detail(self.session, trade_date, timeout=self.timeout, retry=self.retry), trade_date=trade_date)
        return [_with_exchange_source(row) for row in rows]

    def fetch_margin_account_stats(self, trade_date: date | None = None) -> list[dict[str, Any]]:
        return []

    def fetch_latest_margin_trade_date(self, end_date: date | None = None, *, lookback_days: int = 20) -> date | None:
        cursor = end_date or date.today()
        for offset in range(lookback_days + 1):
            trade_date = cursor - timedelta(days=offset)
            rows = self.fetch_margin_market_summary(trade_date)
            dates = [row["trade_date"] for row in rows if row.get("trade_date") is not None]
            if dates:
                return max(dates)
        return None


def _fetch_sse_summary(session: requests.Session, trade_date: date, *, timeout: float, retry: int) -> pd.DataFrame:
    day = _date_param(trade_date)
    data = _request_json(
        session,
        "https://query.sse.com.cn/marketdata/tradedata/queryMargin.do",
        params={
            "isPagination": "true",
            "beginDate": day,
            "endDate": day,
            "tabType": "",
            "stockCode": "",
            "pageHelp.pageSize": "5000",
            "pageHelp.pageNo": "1",
            "pageHelp.beginPage": "1",
            "pageHelp.cacheSize": "1",
            "pageHelp.endPage": "5",
        },
        headers=_sse_headers(),
        timeout=timeout,
        retry=retry,
    )
    frame = pd.DataFrame(data.get("result") or [])
    if frame.empty:
        return frame
    frame.columns = [
        "_",
        "信用交易日期",
        "_",
        "融券卖出量",
        "融券余量",
        "融券余量金额",
        "_",
        "_",
        "融资买入额",
        "融资融券余额",
        "融资余额",
        "_",
        "_",
    ]
    return frame[["信用交易日期", "融资余额", "融资买入额", "融券余量", "融券余量金额", "融券卖出量", "融资融券余额"]]


def _fetch_sse_detail(session: requests.Session, trade_date: date, *, timeout: float, retry: int) -> pd.DataFrame:
    day = _date_param(trade_date)
    data = _request_json(
        session,
        "https://query.sse.com.cn/marketdata/tradedata/queryMargin.do",
        params={
            "isPagination": "true",
            "tabType": "mxtype",
            "detailsDate": day,
            "stockCode": "",
            "beginDate": "",
            "endDate": "",
            "pageHelp.pageSize": "5000",
            "pageHelp.pageCount": "50",
            "pageHelp.pageNo": "1",
            "pageHelp.beginPage": "1",
            "pageHelp.cacheSize": "1",
            "pageHelp.endPage": "21",
        },
        headers=_sse_headers(),
        timeout=timeout,
        retry=retry,
    )
    frame = pd.DataFrame(data.get("result") or [])
    if frame.empty:
        return frame
    frame.columns = [
        "_",
        "信用交易日期",
        "融券偿还量",
        "融券卖出量",
        "融券余量",
        "_",
        "_",
        "融资偿还额",
        "融资买入额",
        "_",
        "融资余额",
        "标的证券简称",
        "标的证券代码",
    ]
    return frame[["信用交易日期", "标的证券代码", "标的证券简称", "融资余额", "融资买入额", "融资偿还额", "融券余量", "融券卖出量", "融券偿还量"]]


def _fetch_szse_summary(session: requests.Session, trade_date: date, *, timeout: float, retry: int) -> pd.DataFrame:
    data = _request_json(
        session,
        "https://www.szse.cn/api/report/ShowReport/data",
        params={
            "SHOWTYPE": "JSON",
            "CATALOGID": "1837_xxpl",
            "txtDate": _dash_date_param(trade_date),
            "tab1PAGENO": "1",
            "random": str(time.time()),
        },
        headers=_szse_headers(),
        timeout=timeout,
        retry=retry,
    )
    table = data[0].get("data") if isinstance(data, list) and data else []
    frame = pd.DataFrame(table or [])
    if frame.empty:
        return frame
    frame.columns = ["融资买入额", "融资余额", "融券卖出量", "融券余量", "融券余额", "融资融券余额"]
    return frame


def _fetch_szse_detail(session: requests.Session, trade_date: date, *, timeout: float, retry: int) -> pd.DataFrame:
    response = _request(
        session,
        "https://www.szse.cn/api/report/ShowReport",
        params={
            "SHOWTYPE": "xlsx",
            "CATALOGID": "1837_xxpl",
            "txtDate": _dash_date_param(trade_date),
            "tab2PAGENO": "1",
            "TABKEY": "tab2",
            "random": str(time.time()),
        },
        headers=_szse_headers(),
        timeout=timeout,
        retry=retry,
    )
    try:
        frame = pd.read_excel(BytesIO(response.content), engine="openpyxl", dtype={"证券代码": str})
    except Exception:
        return pd.DataFrame()
    if frame.empty:
        return frame
    frame.columns = ["证券代码", "证券简称", "融资买入额", "融资余额", "融券卖出量", "融券余量", "融券余额", "融资融券余额"]
    return frame


def _request_json(session: requests.Session, url: str, *, params: dict[str, Any], headers: dict[str, str], timeout: float, retry: int) -> Any:
    response = _request(session, url, params=params, headers=headers, timeout=timeout, retry=retry)
    return response.json()


def _request(session: requests.Session, url: str, *, params: dict[str, Any], headers: dict[str, str], timeout: float, retry: int) -> requests.Response:
    last_exc: Exception | None = None
    for attempt in range(max(retry, 1)):
        try:
            response = session.get(url, params=params, headers=headers, timeout=timeout)
            response.raise_for_status()
            return response
        except Exception as exc:  # noqa: BLE001
            last_exc = exc
            if attempt < max(retry, 1) - 1:
                time.sleep(0.5 * (2**attempt))
    assert last_exc is not None
    raise last_exc


def _with_exchange_source(row: dict[str, Any]) -> dict[str, Any]:
    source = str(row.get("source") or "")
    source_map = {
        "akshare.stock_margin_sse": "exchange.sse.margin_summary",
        "akshare.stock_margin_szse": "exchange.szse.margin_summary",
        "akshare.stock_margin_detail_sse": "exchange.sse.margin_detail",
        "akshare.stock_margin_detail_szse": "exchange.szse.margin_detail",
    }
    row["source"] = source_map.get(source, source)
    row["extra"] = {**(row.get("extra") or {}), "provider": "exchange"}
    return row


def _date_param(value: date) -> str:
    return value.strftime("%Y%m%d")


def _dash_date_param(value: date) -> str:
    return value.strftime("%Y-%m-%d")


def _sse_headers() -> dict[str, str]:
    return {
        "Referer": "https://www.sse.com.cn/",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36",
    }


def _szse_headers() -> dict[str, str]:
    return {
        "Referer": "https://www.szse.cn/disclosure/margin/margin/index.html",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36",
    }
