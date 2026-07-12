from __future__ import annotations

import json
import os
from pathlib import Path
import threading
import time
from typing import Any

from cynexus_data.config.settings import PROJECT_ROOT


COOKIE_NAMES = ("qgqp_b_id", "nid18")
MOBILE_USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36"
)


class EastMoneyBrowserSession:
    def __init__(self, root: Path | None = None) -> None:
        var_root = Path(os.getenv("CYNEXUS_DATA_VAR_ROOT") or (root or PROJECT_ROOT) / "var")
        self.profile_dir = var_root / "eastmoney-browser-profile"
        self.cache_path = var_root / "eastmoney-browser-cookies.json"
        self._lock = threading.Lock()

    def load_cached_cookies(self) -> dict[str, str]:
        try:
            payload = json.loads(self.cache_path.read_text(encoding="utf-8"))
        except (FileNotFoundError, OSError, ValueError, TypeError):
            return {}
        cookies = payload.get("cookies") if isinstance(payload, dict) else None
        if not isinstance(cookies, dict):
            return {}
        return {
            name: str(cookies[name]).strip()
            for name in COOKIE_NAMES
            if str(cookies.get(name) or "").strip()
        }

    def refresh_cookies(self, wait_seconds: float = 5.0) -> dict[str, str]:
        with self._lock:
            self.profile_dir.mkdir(parents=True, exist_ok=True)
            try:
                from playwright.sync_api import sync_playwright
            except ImportError as exc:
                raise RuntimeError("Playwright is required to refresh EastMoney browser cookies") from exc

            with sync_playwright() as playwright:
                context = playwright.chromium.launch_persistent_context(
                    str(self.profile_dir),
                    headless=True,
                    executable_path=os.getenv("EASTMONEY_CHROMIUM_EXECUTABLE") or None,
                    user_agent=MOBILE_USER_AGENT,
                    viewport={"width": 412, "height": 915},
                    locale="zh-CN",
                    args=["--no-sandbox", "--disable-dev-shm-usage"],
                )
                try:
                    page = context.pages[0] if context.pages else context.new_page()
                    page.goto("https://www.eastmoney.com/", wait_until="domcontentloaded", timeout=30_000)
                    page.wait_for_timeout(max(1_000, int(wait_seconds * 1_000)))
                    cookies = {
                        item["name"]: item["value"]
                        for item in context.cookies()
                        if item.get("name") in COOKIE_NAMES and item.get("value")
                    }
                finally:
                    context.close()

            missing = [name for name in COOKIE_NAMES if not cookies.get(name)]
            if missing:
                raise RuntimeError(f"EastMoney browser session did not produce cookies: {', '.join(missing)}")
            self._write_cache(cookies)
            return cookies

    def _write_cache(self, cookies: dict[str, str]) -> None:
        self.cache_path.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.cache_path.with_suffix(".tmp")
        temporary.write_text(
            json.dumps(
                {"cookies": cookies, "refreshed_at": int(time.time())},
                ensure_ascii=True,
                separators=(",", ":"),
            ),
            encoding="utf-8",
        )
        temporary.replace(self.cache_path)


browser_session = EastMoneyBrowserSession()
