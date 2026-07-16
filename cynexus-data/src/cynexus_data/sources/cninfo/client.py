from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any

import requests


UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


@dataclass
class CninfoClient:
    session: requests.Session = field(default_factory=requests.Session)
    timeout: float = 15.0
    _orgid_map: dict[str, str] = field(default_factory=dict)

    def announcements(self, code: str, *, page_size: int = 30) -> list[dict[str, Any]]:
        code6 = _plain_code(code)
        response = self.session.post(
            "https://www.cninfo.com.cn/new/hisAnnouncement/query",
            data={
                "stock": f"{code6},{self.org_id(code6)}",
                "tabName": "fulltext",
                "pageSize": str(page_size),
                "pageNum": "1",
                "column": "",
                "category": "",
                "plate": "",
                "seDate": "",
                "searchkey": "",
                "secid": "",
                "sortName": "",
                "sortType": "",
                "isHLtitle": "true",
            },
            headers={"User-Agent": UA, "Referer": "https://www.cninfo.com.cn/new/disclosure/stock"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        data = response.json()
        rows = []
        for item in data.get("announcements") or []:
            adjunct = item.get("adjunctUrl") or ""
            rows.append(
                {
                    "title": _strip_html(item.get("announcementTitle")),
                    "type": item.get("announcementTypeName"),
                    "date": _ts_to_date(item.get("announcementTime")),
                    "url": f"https://static.cninfo.com.cn/{adjunct}" if adjunct else "",
                    "announcement_id": item.get("announcementId"),
                }
            )
        return rows

    def market_announcements(self, *, page_size: int = 200) -> list[dict[str, Any]]:
        policy_terms = ("国务院", "证监会", "交易所", "财政部", "发改委", "工信部", "监管", "监管办法", "指导意见", "实施方案", "规划", "通知", "征求意见", "规则", "办法")
        company_terms = ("股东回报", "权益分派", "分红", "回购", "业绩", "股票", "公司", "股东", "股份有限公司", "年报", "持股")
        query_terms = ("国务院", "证监会", "交易所", "财政部", "发改委", "工信部")
        rows: dict[str, dict[str, Any]] = {}
        for searchkey in query_terms:
            response = self.session.post(
                "https://www.cninfo.com.cn/new/hisAnnouncement/query",
                data={
                    "stock": "", "tabName": "fulltext", "pageSize": str(max(20, page_size // len(query_terms))), "pageNum": "1",
                    "column": "", "category": "", "plate": "", "seDate": "", "searchkey": searchkey, "secid": "",
                    "sortName": "announcementTime", "sortType": "desc", "isHLtitle": "true",
                },
                headers={"User-Agent": UA, "Referer": "https://www.cninfo.com.cn/new/disclosure"},
                timeout=self.timeout,
            )
            response.raise_for_status()
            for item in response.json().get("announcements") or []:
                if item.get("secCode"):
                    continue
                title = _strip_html(item.get("announcementTitle"))
                if not title or not any(term in title for term in policy_terms) or any(term in title for term in company_terms):
                    continue
                announcement_id = str(item.get("announcementId") or "")
                if not announcement_id:
                    continue
                adjunct = item.get("adjunctUrl") or ""
                rows[announcement_id] = {
                    "announcement_id": announcement_id,
                    "title": title,
                    "type": item.get("announcementTypeName") or item.get("announcementType"),
                    "date": _ts_to_date(item.get("announcementTime")),
                    "url": f"https://static.cninfo.com.cn/{adjunct}" if adjunct else "",
                    "sec_code": item.get("secCode") or "",
                    "sec_name": item.get("secName") or "",
                }
        return sorted(rows.values(), key=lambda item: item.get("date") or "", reverse=True)[:page_size]

    def regulatory_announcements(self, *, page_size: int = 100) -> list[dict[str, Any]]:
        terms = ("监管函", "问询", "纪律处分", "行政处罚", "监管措施", "异常交易", "自律监管", "立案")
        rows: dict[str, dict[str, Any]] = {}
        for searchkey in terms:
            response = self.session.post(
                "https://www.cninfo.com.cn/new/hisAnnouncement/query",
                data={
                    "stock": "", "tabName": "fulltext", "pageSize": str(max(10, page_size // len(terms))), "pageNum": "1",
                    "column": "", "category": "", "plate": "", "seDate": "", "searchkey": searchkey, "secid": "",
                    "sortName": "announcementTime", "sortType": "desc", "isHLtitle": "true",
                },
                headers={"User-Agent": UA, "Referer": "https://www.cninfo.com.cn/new/disclosure"},
                timeout=self.timeout,
            )
            response.raise_for_status()
            for item in response.json().get("announcements") or []:
                title = _strip_html(item.get("announcementTitle"))
                announcement_id = str(item.get("announcementId") or "")
                if not title or not announcement_id:
                    continue
                adjunct = item.get("adjunctUrl") or ""
                rows[announcement_id] = {
                    "announcement_id": announcement_id,
                    "title": title,
                    "type": item.get("announcementTypeName") or item.get("announcementType"),
                    "date": _ts_to_date(item.get("announcementTime")),
                    "url": f"https://static.cninfo.com.cn/{adjunct}" if adjunct else "",
                    "sec_code": _plain_code(item.get("secCode") or ""),
                    "sec_name": item.get("secName") or "",
                }
        return sorted(rows.values(), key=lambda item: item.get("date") or "", reverse=True)[:page_size]

    def irm(self, code: str, *, page_size: int = 30, page_num: int = 1) -> list[dict[str, Any]]:
        code6 = _plain_code(code)
        search = self.session.post("https://irm.cninfo.com.cn/newircs/index/queryKeyboardInfo", data={"keyWord": code6}, headers={"User-Agent": UA}, timeout=self.timeout)
        search.raise_for_status()
        matches = search.json().get("data") or []
        if not matches:
            return []
        org_id = matches[0].get("secid")
        response = self.session.post(
            "https://irm.cninfo.com.cn/newircs/company/question",
            params={"_t": 1, "stockcode": code6, "orgId": org_id, "pageSize": page_size, "pageNum": page_num, "keyWord": "", "startDay": "", "endDay": ""},
            headers={"User-Agent": UA},
            timeout=self.timeout,
        )
        response.raise_for_status()
        out = []
        for item in response.json().get("rows") or []:
            pub_date = item.get("pubDate")
            out.append(
                {
                    "code": item.get("stockCode"),
                    "company": item.get("companyShortName"),
                    "question": item.get("mainContent"),
                    "answer": item.get("attachedContent"),
                    "answerer": item.get("attachedAuthor"),
                    "ask_time": datetime.fromtimestamp(pub_date / 1000).strftime("%Y-%m-%d %H:%M") if pub_date else "",
                }
            )
        return out

    def org_id(self, code: str) -> str:
        code6 = _plain_code(code)
        if not self._orgid_map:
            try:
                response = self.session.get("http://www.cninfo.com.cn/new/data/szse_stock.json", headers={"User-Agent": UA}, timeout=self.timeout)
                response.raise_for_status()
                self._orgid_map = {str(item.get("code")): str(item.get("orgId")) for item in response.json().get("stockList") or [] if item.get("code") and item.get("orgId")}
            except Exception:
                self._orgid_map = {}
        if code6 in self._orgid_map:
            return self._orgid_map[code6]
        if code6.startswith("6"):
            return f"gssh0{code6}"
        if code6.startswith(("4", "8")):
            return f"gsbj0{code6}"
        return f"gssz0{code6}"


def _ts_to_date(value: Any) -> str:
    if isinstance(value, (int, float)):
        return datetime.fromtimestamp(value / 1000).strftime("%Y-%m-%d")
    return str(value or "")[:10]


def _strip_html(value: Any) -> str:
    import re

    return re.sub(r"<[^>]+>", "", str(value or ""))


def _plain_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    if "." in value:
        value = value.split(".", 1)[0]
    return value[-6:].zfill(6)
