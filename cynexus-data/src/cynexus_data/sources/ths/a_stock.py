from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date as date_type, datetime
from io import StringIO
from typing import Any

import requests


UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


@dataclass
class ThsAStockClient:
    session: requests.Session = field(default_factory=requests.Session)
    timeout: float = 15.0

    def eps_forecast(self, code: str) -> list[dict[str, Any]]:
        try:
            import pandas as pd
        except ImportError as exc:  # pragma: no cover - dependency is declared in project.
            raise RuntimeError("pandas is required for THS EPS forecast table parsing") from exc
        response = self.session.get(
            f"https://basic.10jqka.com.cn/new/{_plain_code(code)}/worth.html",
            headers={"User-Agent": UA, "Referer": "https://basic.10jqka.com.cn/"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        response.encoding = "gbk"
        frames = pd.read_html(StringIO(response.text))
        for frame in frames:
            columns = [str(col) for col in frame.columns]
            if any("每股收益" in col or "均值" in col for col in columns):
                return _frame_records(frame)
        return _frame_records(frames[0]) if frames else []

    def hot_reason(self, trade_date: str | None = None) -> list[dict[str, Any]]:
        day = trade_date or date_type.today().strftime("%Y-%m-%d")
        response = self.session.get(
            f"http://zx.10jqka.com.cn/event/api/getharden/date/{day}/orderby/date/orderway/desc/charset/GBK/",
            headers={"User-Agent": UA},
            timeout=self.timeout,
        )
        response.raise_for_status()
        data = response.json()
        if data.get("errocode", 0) != 0:
            raise RuntimeError(f"THS hot reason error: {data.get('errormsg') or data}")
        return [row for row in data.get("data") or [] if isinstance(row, dict)]

    def northbound_realtime(self) -> list[dict[str, Any]]:
        response = self.session.get(
            "https://data.hexin.cn/market/hsgtApi/method/dayChart/",
            headers={"User-Agent": UA, "Host": "data.hexin.cn", "Referer": "https://data.hexin.cn/"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        data = response.json()
        times = data.get("time") or []
        hgt = data.get("hgt") or []
        sgt = data.get("sgt") or []
        return [{"time": t, "hgt_yi": hgt[idx] if idx < len(hgt) else None, "sgt_yi": sgt[idx] if idx < len(sgt) else None} for idx, t in enumerate(times)]

    def limit_up_pool(self, trade_date: str) -> list[dict[str, Any]]:
        response = self.session.get(
            "https://data.10jqka.com.cn/dataapi/limit_up/limit_up_pool",
            params={
                "page": 1,
                "limit": 200,
                "field": "199112,10,9001,330323,330324,330325,9002,330329,133971,133970,1968584,3475914,9003,9004",
                "filter": "HS,GEM2STAR",
                "order_field": "330324",
                "order_type": "0",
                "date": trade_date,
            },
            headers={"User-Agent": UA, "Referer": "https://data.10jqka.com.cn/"},
            timeout=self.timeout,
        )
        response.raise_for_status()
        return [_limit_row(row) for row in ((response.json().get("data") or {}).get("info") or []) if isinstance(row, dict)]

    def hot_list(self, period: str = "hour") -> list[dict[str, Any]]:
        response = self.session.get(
            "https://dq.10jqka.com.cn/fuyao/hot_list_data/out/hot_list/v1/stock",
            params={"stock_type": "a", "type": period, "list_type": "normal"},
            headers={"User-Agent": UA},
            timeout=self.timeout,
        )
        response.raise_for_status()
        rows = ((response.json().get("data") or {}).get("stock_list") or [])
        out = []
        for item in rows:
            tag = item.get("tag") or {}
            out.append(
                {
                    "rank": item.get("order"),
                    "code": item.get("code"),
                    "name": item.get("name"),
                    "heat": item.get("rate"),
                    "pct": item.get("rise_and_fall"),
                    "rank_chg": item.get("hot_rank_chg"),
                    "concepts": tag.get("concept_tag") or [],
                    "tag": tag.get("popularity_tag") or "",
                }
            )
        return out


def _limit_row(item: dict[str, Any]) -> dict[str, Any]:
    first = item.get("first_limit_up_time")
    return {
        "code": item.get("code"),
        "name": item.get("name"),
        "price": item.get("latest"),
        "pct": item.get("change_rate"),
        "reason": item.get("reason_type") or "",
        "board_type": item.get("limit_up_type") or "",
        "seal_rate": item.get("limit_up_suc_rate"),
        "break_times": item.get("open_num") or 0,
        "seal_amount": item.get("order_amount"),
        "high_days": item.get("high_days") or "",
        "first_time": datetime.fromtimestamp(int(first)).strftime("%H:%M:%S") if first else "",
        "is_again": item.get("is_again_limit"),
    }


def _frame_records(frame: Any) -> list[dict[str, Any]]:
    records = frame.where(frame.notna(), None).to_dict(orient="records")
    return [{str(key): value for key, value in row.items()} for row in records]


def _plain_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    if "." in value:
        value = value.split(".", 1)[0]
    return value[-6:].zfill(6)
