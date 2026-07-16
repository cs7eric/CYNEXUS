from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from zoneinfo import ZoneInfo
import time
import random
from typing import Any

import requests


@dataclass
class CailianClient:
    session: requests.Session = field(default_factory=requests.Session)
    timeout: float = 15.0

    def __post_init__(self) -> None:
        self.session.headers.setdefault("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
        self.session.headers.setdefault("Accept", "application/json, text/plain, */*")
        self.session.headers.setdefault("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

    def telegraph(self, *, page_size: int = 50) -> list[dict[str, Any]]:
        self.session.get("https://www.cls.cn/", headers={"Referer": "https://www.cls.cn/"}, timeout=self.timeout)
        response = None
        for attempt in range(3):
            response = self.session.get(
                "https://www.cls.cn/api/cache",
                params={"name": "telegraph", "_": f"{int(time.time() * 1000)}-{random.randint(1000, 999999)}", "r": str(random.random())},
                headers={"Referer": "https://www.cls.cn/telegraph", "Origin": "https://www.cls.cn", "Sec-Fetch-Dest": "empty", "Sec-Fetch-Mode": "cors", "Sec-Fetch-Site": "same-origin"},
                timeout=self.timeout,
            )
            if response.status_code != 418:
                break
            time.sleep(1.5 * (attempt + 1))
        assert response is not None
        response.raise_for_status()
        rows = ((response.json().get("data") or {}).get("roll_data") or [])[:page_size]
        result: list[dict[str, Any]] = []
        for row in rows:
            if not isinstance(row, dict) or not row.get("id"):
                continue
            title = str(row.get("title") or row.get("brief") or row.get("content") or "").strip()
            if not title:
                continue
            ctime = row.get("ctime")
            published = datetime.fromtimestamp(float(ctime), tz=ZoneInfo("Asia/Shanghai")).isoformat() if ctime else ""
            result.append(
                {
                    "id": str(row["id"]),
                    "title": title,
                    "summary": str(row.get("brief") or row.get("content") or "").strip()[:500],
                    "time": published,
                    "source": "财联社",
                    "url": f"https://www.cls.cn/detail/{row['id']}",
                    "stock_list": row.get("stock_list") or [],
                    "subjects": row.get("subjects") or [],
                    "raw_payload": row,
                }
            )
        return result
