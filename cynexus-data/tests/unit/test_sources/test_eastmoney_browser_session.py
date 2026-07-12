from __future__ import annotations

import json

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
