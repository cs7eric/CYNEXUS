from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from html import unescape
import re
from typing import Any
from urllib.parse import urljoin

import requests


UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
SINA_OPT_HEADERS = {"Referer": "https://stock.finance.sina.com.cn/", "User-Agent": UA}
SINA_NEWS_BASE_URL = "https://vip.stock.finance.sina.com.cn/"
SINA_NEWS_ITEM_PATTERN = re.compile(
    r"(?P<date>\d{4}-\d{2}-\d{2})\s*(?:&nbsp;|\xa0)*"
    r"(?P<time>\d{2}:\d{2})\s*(?:&nbsp;|\xa0)*"
    r"<a[^>]+href=['\"](?P<url>[^'\"]+)['\"][^>]*>(?P<title>[^<]+)</a>",
    re.IGNORECASE,
)


@dataclass
class SinaAStockClient:
    session: requests.Session = field(default_factory=requests.Session)
    timeout: float = 15.0

    def financial_report(self, code: str, *, report_type: str = "lrb", num: int = 8) -> list[dict[str, Any]]:
        response = self.session.get(
            "https://quotes.sina.cn/cn/api/openapi.php/CompanyFinanceService.getFinanceReport2022",
            params={"paperCode": _sina_symbol(code), "source": report_type, "type": "0", "page": "1", "num": str(num)},
            headers={"User-Agent": UA},
            timeout=self.timeout,
        )
        response.raise_for_status()
        report_list = (((response.json().get("result") or {}).get("data") or {}).get("report_list") or {})
        out = []
        for period in sorted(report_list.keys(), reverse=True)[:num]:
            obj = report_list.get(period) or {}
            record: dict[str, Any] = {"report_date": f"{period[:4]}-{period[4:6]}-{period[6:8]}"}
            for item in obj.get("data") or []:
                title = item.get("item_title")
                if not title or item.get("item_value") is None:
                    continue
                record[str(title)] = item.get("item_value")
                if item.get("item_tongbi") not in (None, ""):
                    record[f"{title}_yoy"] = item.get("item_tongbi")
            out.append(record)
        return out

    def stock_news(self, code: str, *, page: int = 1) -> list[dict[str, Any]]:
        """Return Sina stock-news rows with their original article URLs.

        Sina renders this legacy stock-specific list server-side, making it a
        stable fallback when EastMoney's search endpoint omits article URLs.
        """
        response = self.session.get(
            urljoin(SINA_NEWS_BASE_URL, "corp/view/vCB_AllNewsStock.php"),
            params={"symbol": _sina_symbol(code), "Page": str(max(page, 1))},
            headers=SINA_OPT_HEADERS,
            timeout=self.timeout,
        )
        response.raise_for_status()
        encoding = getattr(response, "encoding", None)
        if not encoding or str(encoding).lower() in {"ascii", "iso-8859-1"}:
            response.encoding = "gb18030"
        return _parse_stock_news(response.text)

    def option_codes(self, *, underlying: str = "510050", call: bool = True) -> dict[str, list[str]]:
        cate = {"510050": "50ETF", "510300": "300ETF", "588000": "科创50ETF", "510500": "500ETF"}.get(str(underlying), "50ETF")
        response = self.session.get(
            f"https://stock.finance.sina.com.cn/futures/api/openapi.php/StockOptionService.getStockName",
            params={"exchange": "null", "cate": cate},
            headers=SINA_OPT_HEADERS,
            timeout=self.timeout,
        )
        response.raise_for_status()
        months = (((response.json().get("result") or {}).get("data") or {}).get("contractMonth") or [])
        months = [str(month).replace("-", "")[2:] for month in months[1:]]
        flag = "OP_UP_" if call else "OP_DOWN_"
        out: dict[str, list[str]] = {}
        for month in months:
            codes = [code.replace("CON_OP_", "") for code in self._hq_list(f"{flag}{underlying}{month}") if str(code).startswith("CON_OP_")]
            if codes:
                out[month] = codes
        return out

    def option_tquote(self, code: str) -> dict[str, Any]:
        values = self._hq_list(f"CON_OP_{code}")
        if len(values) < 43:
            return {}
        return {
            "bid_vol": _value(values[0]),
            "bid": _value(values[1]),
            "last": _value(values[2]),
            "ask": _value(values[3]),
            "ask_vol": _value(values[4]),
            "open_interest": _value(values[5]),
            "pct": _value(values[6]),
            "strike": _value(values[7]),
            "prev_close": _value(values[8]),
            "open": _value(values[9]),
            "limit_up": _value(values[10]),
            "limit_down": _value(values[11]),
            "name": values[37],
            "amplitude": _value(values[38]),
            "high": _value(values[39]),
            "low": _value(values[40]),
            "volume": _value(values[41]),
            "amount": _value(values[42]),
        }

    def option_greeks(self, code: str) -> dict[str, Any]:
        raw = self._hq_list(f"CON_SO_{code}")
        if len(raw) < 16:
            return {}
        values = [raw[0]] + raw[4:]
        return {
            "name": values[0],
            "volume": _value(values[1]),
            "delta": _value(values[2]),
            "gamma": _value(values[3]),
            "theta": _value(values[4]),
            "vega": _value(values[5]),
            "iv": _value(values[6]),
            "high": _value(values[7]),
            "low": _value(values[8]),
            "trade_code": values[9],
            "strike": _value(values[10]),
            "last": _value(values[11]),
            "theory": _value(values[12]),
        }

    def _hq_list(self, param: str) -> list[str]:
        response = self.session.get(f"https://hq.sinajs.cn/list={param}", headers=SINA_OPT_HEADERS, timeout=self.timeout)
        response.raise_for_status()
        response.encoding = "gbk"
        text = response.text
        return text.split('"')[1].split(",") if '"' in text else []


def _sina_symbol(code: str) -> str:
    code6 = _plain_code(code)
    return ("sh" if code6.startswith("6") else "sz") + code6


def _plain_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    if "." in value:
        value = value.split(".", 1)[0]
    return value[-6:].zfill(6)


def _value(value: Any) -> Any:
    try:
        return float(value)
    except (TypeError, ValueError):
        return value


def _parse_stock_news(html: str) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for match in SINA_NEWS_ITEM_PATTERN.finditer(html or ""):
        title = unescape(match.group("title")).strip()
        href = unescape(match.group("url")).strip()
        if not title or not href:
            continue
        published_at = f"{match.group('date')} {match.group('time')}"
        try:
            datetime.strptime(published_at, "%Y-%m-%d %H:%M")
        except ValueError:
            continue
        rows.append(
            {
                "title": title,
                "content": "",
                "time": published_at,
                "source": "Sina Finance",
                "url": urljoin(SINA_NEWS_BASE_URL, href),
            }
        )
    return rows
