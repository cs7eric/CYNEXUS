from __future__ import annotations

from dataclasses import dataclass, field
import os
import secrets
from typing import Any

import requests


@dataclass
class IwencaiClient:
    session: requests.Session = field(default_factory=requests.Session)
    base_url: str = field(default_factory=lambda: os.environ.get("IWENCAI_BASE_URL", "https://openapi.iwencai.com"))
    api_key: str = field(default_factory=lambda: os.environ.get("IWENCAI_API_KEY", ""))
    timeout: float = 30.0

    def search(self, query: str, *, channel: str = "report", size: int = 50) -> list[dict[str, Any]]:
        payload = {"channels": [channel], "app_id": "AIME_SKILL", "query": query, "size": size}
        data = self._post("/v1/comprehensive/search", payload)
        return data.get("data") or []

    def query2data(self, query: str, *, page: int = 1, limit: int = 50) -> list[dict[str, Any]]:
        payload = {"query": query, "page": str(page), "limit": str(limit), "is_cache": "1", "expand_index": "true"}
        data = self._post("/v1/query2data", payload)
        return data.get("datas") or []

    def _post(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        if not self.api_key:
            raise RuntimeError("IWENCAI_API_KEY is required for iwencai live requests")
        response = self.session.post(f"{self.base_url.rstrip('/')}/{path.lstrip('/')}", json=payload, headers={**_claw_headers(), "Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"}, timeout=self.timeout)
        response.raise_for_status()
        data = response.json()
        if data.get("status_code", 0) != 0:
            raise RuntimeError(f"iwencai error: {data.get('status_msg') or data}")
        return data


def dedup_articles(articles: list[dict[str, Any]]) -> list[dict[str, Any]]:
    best: dict[str, dict[str, Any]] = {}
    for item in articles:
        uid = str(item.get("uid") or f"{item.get('title', '')}|{item.get('publish_date', '')}")
        score = _float(item.get("score")) or 0.0
        if uid not in best or score > (_float(best[uid].get("score")) or 0.0):
            best[uid] = item
    return sorted(best.values(), key=lambda row: str(row.get("publish_date") or ""), reverse=True)


def _claw_headers(call_type: str = "normal") -> dict[str, str]:
    return {
        "X-Claw-Call-Type": call_type,
        "X-Claw-Skill-Id": "report-search",
        "X-Claw-Skill-Version": "2.0.0",
        "X-Claw-Plugin-Id": "none",
        "X-Claw-Plugin-Version": "none",
        "X-Claw-Trace-Id": secrets.token_hex(32),
    }


def _float(value: Any) -> float | None:
    try:
        return float(value)
    except (TypeError, ValueError):
        return None
