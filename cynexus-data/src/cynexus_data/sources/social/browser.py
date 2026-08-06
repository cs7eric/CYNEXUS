from __future__ import annotations

import os
import threading
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Iterator
from urllib.parse import urlparse

from cynexus_data.connections.models import Connection
from cynexus_data.sources.social.models import SocialComment, SocialCreator, SocialPost
from cynexus_data.sources.social.parser import parse_comments, parse_post_links


DEFAULT_POST_SELECTORS = {
    "xiaohongshu": "a[href*='/explore/'], a[href*='/discovery/item/']",
    "douyin": "a[href*='/video/'], a[href*='/note/']",
}
DEFAULT_COMMENT_SELECTORS = {
    "xiaohongshu": "[data-comment-id], [data-id*='comment'], [class*='comment']",
    "douyin": "[data-comment-id], [data-e2e*='comment'], [class*='comment']",
}


class SocialAccessError(RuntimeError):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code


class SocialBrowserClient:
    def __init__(self, platform: str, connection: Connection, profile_name: str = "default") -> None:
        self.platform = platform
        self.connection = connection
        self.profile_name = profile_name
        configured = connection.extra.get("extra") if isinstance(connection.extra.get("extra"), dict) else connection.extra
        self._configured = configured
        root = connection.root_path or os.getenv("CYNEXUS_DATA_VAR_ROOT") or "var"
        self.profile_dir = Path(root) / "social-browser-profiles" / platform / profile_name
        self.headless = _truthy(configured.get("headless", True))
        self.wait_ms = _bounded_int(configured.get("wait_ms"), 2_000, 0, 30_000)
        self.scroll_rounds = _bounded_int(configured.get("scroll_rounds"), 12, 1, 60)
        self.scroll_wait_ms = _bounded_int(configured.get("scroll_wait_ms"), 800, 100, 10_000)
        self.timeout_ms = _timeout_ms(connection.timeout, 30_000)
        self.executable_path = _optional_path(configured.get("executable_path"))
        self._lock = threading.Lock()

    @contextmanager
    def session(self) -> Iterator[Any]:
        with self._lock:
            playwright, context = self.launch_persistent_context()
            try:
                yield context
            finally:
                context.close()
                playwright.stop()

    def launch_persistent_context(self, *, headless_override: bool | None = None) -> tuple[Any, Any]:
        try:
            from playwright.sync_api import sync_playwright
        except ImportError as exc:
            raise SocialAccessError("PLAYWRIGHT_UNAVAILABLE", "Playwright is required for social browser collection") from exc

        self.profile_dir.mkdir(parents=True, exist_ok=True)
        playwright = sync_playwright().start()
        launch_kwargs: dict[str, Any] = {
            "headless": self.headless if headless_override is None else headless_override,
            "locale": "zh-CN",
            "viewport": {"width": 412, "height": 915},
            "args": ["--no-sandbox", "--disable-dev-shm-usage"],
        }
        if self.executable_path:
            launch_kwargs["executable_path"] = self.executable_path
        try:
            return playwright, playwright.chromium.launch_persistent_context(str(self.profile_dir), **launch_kwargs)
        except Exception:
            playwright.stop()
            raise

    def discover_posts(self, creator: SocialCreator) -> list[SocialPost]:
        with self.session() as context:
            page = context.pages[0] if context.pages else context.new_page()
            self._goto(page, creator.profile_url)
            selector = self._selector("post_link_selectors", DEFAULT_POST_SELECTORS[self.platform])
            rows = page.locator(selector).evaluate_all(
                """els => els.map(el => ({
                    href: el.href || el.getAttribute('href'),
                    text: el.innerText || el.textContent || '',
                    aria_label: el.getAttribute('aria-label') || '',
                    published_at: el.getAttribute('data-publish-time') || el.getAttribute('datetime') || ''
                }))"""
            )
            return parse_post_links(self.platform, rows if isinstance(rows, list) else [])

    def fetch_comments(self, post: SocialPost) -> list[SocialComment]:
        with self.session() as context:
            page = context.pages[0] if context.pages else context.new_page()
            self._goto(page, post.url)
            selector = self._selector("comment_selectors", DEFAULT_COMMENT_SELECTORS[self.platform])
            collected: list[dict[str, Any]] = []
            stable_rounds = 0
            for _ in range(self.scroll_rounds):
                rows = page.locator(selector).evaluate_all(
                    """els => els.map(el => ({
                        id: el.getAttribute('data-comment-id') || el.getAttribute('data-id') || el.id || '',
                        parent_id: el.getAttribute('data-parent-id') || '',
                        content: el.innerText || el.textContent || '',
                        like_count: el.getAttribute('data-like-count') || '',
                        reply_count: el.getAttribute('data-reply-count') || '',
                        author_id: el.getAttribute('data-author-id') || ''
                    }))"""
                )
                before = len(collected)
                if isinstance(rows, list):
                    collected.extend(rows)
                if len(collected) == before:
                    stable_rounds += 1
                else:
                    stable_rounds = 0
                if stable_rounds >= 2:
                    break
                page.mouse.wheel(0, 1_200)
                page.wait_for_timeout(self.scroll_wait_ms)
            return parse_comments(self.platform, post.post_key, collected)

    def _goto(self, page: Any, url: str) -> None:
        try:
            page.goto(url, wait_until="domcontentloaded", timeout=self.timeout_ms)
            page.wait_for_timeout(self.wait_ms)
        except Exception as exc:
            raise SocialAccessError("PAGE_LOAD_FAILED", f"Failed to load social page: {url}") from exc
        current_url = str(getattr(page, "url", "") or "")
        if any(token in current_url.lower() for token in ("login", "passport", "account")):
            raise SocialAccessError("LOGIN_REQUIRED", f"Authorized browser session is not logged in for {self.platform}")
        body_text = ""
        try:
            body_text = str(page.locator("body").inner_text(timeout=1_000) or "")[:10_000]
        except Exception:
            pass
        if self.platform == "xiaohongshu":
            if "300012" in current_url or "IP存在风险" in body_text:
                raise SocialAccessError(
                    "PLATFORM_BLOCKED",
                    "Xiaohongshu rejected the current network environment (300012)",
                )
            if "登录后" in body_text and ("扫码" in body_text or "新用户可直接登录" in body_text):
                raise SocialAccessError("LOGIN_REQUIRED", "Authorized browser session is not logged in for xiaohongshu")
            target_path = urlparse(str(url)).path.rstrip("/")
            current_path = urlparse(current_url).path.rstrip("/")
            if target_path.startswith("/user/profile/") and current_path in {"", "/", "/explore"}:
                raise SocialAccessError("PAGE_REDIRECTED", "Xiaohongshu redirected the creator profile to the Explore page")
            if target_path.startswith("/explore/") and current_path in {"", "/", "/explore"}:
                raise SocialAccessError("PAGE_REDIRECTED", "Xiaohongshu redirected the post to the Explore page")

    def _selector(self, key: str, default: str) -> str:
        values = self._configured.get(key) or {}
        if isinstance(values, dict):
            value = values.get(self.platform)
            if value:
                return str(value)
        return default


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _bounded_int(value: Any, default: int, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = default
    return max(minimum, min(maximum, parsed))


def _timeout_ms(value: Any, default: int) -> int:
    try:
        seconds = float(value)
    except (TypeError, ValueError):
        return default
    return max(1_000, min(120_000, int(seconds * 1_000)))


def _optional_path(value: Any) -> str | None:
    if not value:
        return None
    path = Path(str(value))
    return str(path) if path.exists() else None
