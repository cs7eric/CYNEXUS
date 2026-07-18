from __future__ import annotations

import json
import sys
from types import ModuleType

from cynexus_data.sources.eastmoney.browser_session import EastMoneyBrowserSession


def test_browser_session_reads_cached_required_cookies(tmp_path) -> None:
    session = EastMoneyBrowserSession(root=tmp_path)
    session.cache_path.parent.mkdir(parents=True, exist_ok=True)
    session.cache_path.write_text(
        json.dumps(
            {
                "cookies": {
                    "qgqp_b_id": "visitor",
                    "nid18": "session",
                    "ignored": "tracking",
                }
            }
        ),
        encoding="utf-8",
    )

    assert session.load_cached_cookies() == {"qgqp_b_id": "visitor", "nid18": "session"}


def test_browser_session_ignores_incomplete_or_invalid_cache(tmp_path) -> None:
    session = EastMoneyBrowserSession(root=tmp_path)
    session.cache_path.parent.mkdir(parents=True, exist_ok=True)
    session.cache_path.write_text("not-json", encoding="utf-8")

    assert session.load_cached_cookies() == {}


def test_browser_session_requests_json_from_persistent_context(monkeypatch, tmp_path) -> None:
    calls = {}

    class Response:
        ok = True
        status = 200

        def json(self):
            return {"data": {"klines": ["2026-07-16,-1,2,3,-4,-5,0,0,0,0,0,1,2,3,4"]}}

    class Request:
        def get(self, url, *, params, headers, timeout):
            calls["request"] = (url, params, headers, timeout)
            return Response()

    class Page:
        def goto(self, url, *, wait_until, timeout):
            calls["page"] = (url, wait_until, timeout)

    class Context:
        pages = []
        request = Request()

        def new_page(self):
            return Page()

        def close(self):
            calls["closed"] = True

    class Chromium:
        def launch_persistent_context(self, profile, **kwargs):
            calls["profile"] = profile
            calls["launch"] = kwargs
            return Context()

    class Playwright:
        chromium = Chromium()

    class PlaywrightContextManager:
        def __enter__(self):
            return Playwright()

        def __exit__(self, exc_type, exc, tb):
            return False

    package = ModuleType("playwright")
    api = ModuleType("playwright.sync_api")
    api.sync_playwright = lambda: PlaywrightContextManager()
    monkeypatch.setitem(sys.modules, "playwright", package)
    monkeypatch.setitem(sys.modules, "playwright.sync_api", api)

    session = EastMoneyBrowserSession(root=tmp_path)
    payload = session.request_json(
        "https://push2his.eastmoney.com/api/qt/stock/fflow/daykline/get",
        params={"secid": "1.000001"},
        headers={"Referer": "https://data.eastmoney.com/zjlx/dpzjlx.html"},
    )

    assert payload["data"]["klines"]
    assert calls["profile"].endswith("eastmoney-browser-profile")
    assert calls["page"][0].endswith("dpzjlx.html")
    assert calls["closed"] is True
