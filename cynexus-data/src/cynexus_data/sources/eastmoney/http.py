from __future__ import annotations

from dataclasses import dataclass, field
import json
import os
import random
import re
import threading
import time
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from requests.exceptions import ProxyError, RequestException
from urllib3.util.retry import Retry

from cynexus_data.sources.eastmoney.browser_session import browser_session


UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)


@dataclass
class EastMoneyHttpClient:
    session: requests.Session = field(default_factory=requests.Session)
    min_interval: float = 2.0
    jitter: tuple[float, float] = (0.5, 2.0)
    timeout: float = 15.0

    def __post_init__(self) -> None:
        self.session.headers.setdefault("User-Agent", UA)
        self.session.headers.setdefault("Accept", "application/json, text/plain, */*")
        self.session.headers.setdefault("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7")
        self.session.headers.setdefault("Connection", "keep-alive")
        self._load_configured_cookies()
        retry = Retry(
            total=3,
            connect=3,
            read=2,
            backoff_factor=1.2,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["GET", "POST"],
            respect_retry_after_header=True,
        )
        adapter = HTTPAdapter(max_retries=retry)
        self.session.mount("https://", adapter)
        self.session.mount("http://", adapter)
        self._last_call = 0.0
        self._lock = threading.Lock()
        self._circuit_open_until: dict[str, float] = {}

    def get(
        self,
        url: str,
        *,
        params: dict[str, Any] | None = None,
        headers: dict[str, str] | None = None,
        timeout: float | None = None,
        **kwargs: Any,
    ) -> requests.Response:
        self._throttle()
        try:
            response = self.session.get(url, params=params, headers=headers, timeout=timeout or self.timeout, **kwargs)
        except (ProxyError, RequestException):
            self._cooldown()
            response = self._no_proxy_session().get(url, params=params, headers=headers, timeout=timeout or self.timeout, **kwargs)
        response.raise_for_status()
        return response

    def post(
        self,
        url: str,
        *,
        json_body: dict[str, Any] | None = None,
        data: dict[str, Any] | None = None,
        headers: dict[str, str] | None = None,
        timeout: float | None = None,
        **kwargs: Any,
    ) -> requests.Response:
        self._throttle()
        try:
            response = self.session.post(url, json=json_body, data=data, headers=headers, timeout=timeout or self.timeout, **kwargs)
        except (ProxyError, RequestException):
            self._cooldown()
            response = self._no_proxy_session().post(url, json=json_body, data=data, headers=headers, timeout=timeout or self.timeout, **kwargs)
        response.raise_for_status()
        return response

    def _throttle(self) -> None:
        with self._lock:
            wait = self.min_interval - (time.time() - self._last_call)
            if wait > 0:
                time.sleep(wait + random.uniform(*self.jitter))
            self._last_call = time.time()

    def _no_proxy_session(self) -> requests.Session:
        session = requests.Session()
        session.trust_env = False
        session.headers.update(self.session.headers)
        return session

    def _cooldown(self) -> None:
        time.sleep(random.uniform(2.0, 5.0))

    def _load_configured_cookies(self) -> None:
        configured: dict[str, str] = {}
        for cookie_name, env_name in (
            ("qgqp_b_id", "EASTMONEY_QGQP_B_ID"),
            ("nid18", "EASTMONEY_NID18"),
        ):
            value = os.getenv(env_name, "").strip()
            if value:
                configured[cookie_name] = value
        if len(configured) < 2:
            configured = {**browser_session.load_cached_cookies(), **configured}
        if configured:
            self.set_eastmoney_cookies(configured)

    def set_eastmoney_cookies(self, cookies: dict[str, str]) -> None:
        values = [f"{name}={cookies[name]}" for name in ("qgqp_b_id", "nid18") if cookies.get(name)]
        if values:
            self.session.headers["Cookie"] = "; ".join(values)

    def refresh_browser_cookies(self) -> dict[str, str]:
        cookies = browser_session.refresh_cookies()
        self.set_eastmoney_cookies(cookies)
        return cookies

    def circuit_open(self, key: str) -> bool:
        with self._lock:
            return self._circuit_open_until.get(key, 0.0) > time.time()

    def open_circuit(self, key: str, cooldown_seconds: float = 900.0) -> None:
        with self._lock:
            self._circuit_open_until[key] = time.time() + max(1.0, cooldown_seconds)

    def close_circuit(self, key: str) -> None:
        with self._lock:
            self._circuit_open_until.pop(key, None)


def parse_jsonp(text: str) -> dict[str, Any]:
    match = re.search(r"^[^(]*\((.*)\)\s*;?\s*$", text, flags=re.S)
    if not match:
        return json.loads(text)
    return json.loads(match.group(1))


def diff_rows(payload: dict[str, Any]) -> list[dict[str, Any]]:
    diff = ((payload.get("data") or {}) if isinstance(payload, dict) else {}).get("diff") or []
    if isinstance(diff, dict):
        return [row for row in diff.values() if isinstance(row, dict)]
    return [row for row in diff if isinstance(row, dict)]


def em_secid(code: str) -> str:
    value = str(code or "").strip().lower()
    if value.startswith(("sh", "sz", "bj")):
        value = value[2:]
    value = value[-6:].zfill(6)
    market = "1" if value.startswith(("5", "6", "9")) else "0"
    return f"{market}.{value}"


def plain_code(value: str) -> str:
    text = str(value or "").strip().upper()
    if text.startswith(("SH", "SZ", "BJ")):
        text = text[2:]
    if "." in text:
        text = text.split(".", 1)[0]
    return text[-6:].zfill(6)
