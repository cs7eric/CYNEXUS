from __future__ import annotations

import base64
import re
import threading
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Callable

from cynexus_data.sources.social.browser import SocialAccessError, SocialBrowserClient


PLATFORM_HOME_URLS = {
    "xiaohongshu": "https://www.xiaohongshu.com/",
    "douyin": "https://www.douyin.com/",
}


@dataclass
class SocialLoginSession:
    session_id: str
    platform: str
    profile_name: str
    client: SocialBrowserClient
    playwright: Any
    context: Any
    page: Any
    baseline_cookies: dict[str, str]
    status: str = "waiting_login"
    message: str | None = None
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class SocialLoginSessionManager:
    def __init__(self, client_factory: Callable[[str, str], SocialBrowserClient]) -> None:
        self._client_factory = client_factory
        self._sessions: dict[str, SocialLoginSession] = {}
        self._lock = threading.RLock()

    def start(
        self,
        platform: str,
        profile_name: str = "default",
        *,
        headless_override: bool | None = True,
    ) -> dict[str, Any]:
        normalized = _platform(platform)
        with self._lock:
            active = next(
                (
                    session
                    for session in self._sessions.values()
                    if session.platform == normalized and session.profile_name == profile_name
                ),
                None,
            )
            if active:
                return self._snapshot(active, include_screenshot=True)

            client = self._client_factory(normalized, profile_name)
            playwright, context = client.launch_persistent_context(headless_override=headless_override)
            try:
                page = context.pages[0] if context.pages else context.new_page()
                page.goto(PLATFORM_HOME_URLS[normalized], wait_until="domcontentloaded", timeout=client.timeout_ms)
                page.wait_for_timeout(client.wait_ms)
                _try_open_login(page)
                baseline_cookies = {
                    str(item.get("name")): str(item.get("value"))
                    for item in context.cookies()
                    if item.get("name")
                }
                session = SocialLoginSession(
                    session_id=f"social_login_{uuid.uuid4().hex}",
                    platform=normalized,
                    profile_name=profile_name,
                    client=client,
                    playwright=playwright,
                    context=context,
                    page=page,
                    baseline_cookies=baseline_cookies,
                )
                issue = _detect_login_page_issue(page, normalized)
                if issue:
                    session.status, session.message = issue
                self._sessions[session.session_id] = session
                return self._snapshot(session, include_screenshot=True)
            except Exception:
                context.close()
                playwright.stop()
                raise

    def get(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            return self._snapshot(session, include_screenshot=True)

    def complete(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            current_cookies = {
                str(item.get("name")): str(item.get("value"))
                for item in session.context.cookies()
                if item.get("name")
            }
            changed = any(
                current_cookies.get(name) != value
                for name, value in session.baseline_cookies.items()
            ) or any(name not in session.baseline_cookies for name in current_cookies)
            body_text = _body_text(session.page)
            current_url = str(getattr(session.page, "url", "") or "").lower()
            still_login = any(token in current_url for token in ("login", "passport", "account"))
            still_login = still_login or ("登录" in body_text and "退出登录" not in body_text)
            if not changed or still_login:
                session.status = "waiting_login"
                session.message = "尚未确认登录，请完成扫码后再次检查。"
                session.updated_at = datetime.now(timezone.utc).isoformat()
                return self._snapshot(session, include_screenshot=True)

            session.status = "authenticated"
            session.message = "登录成功，会话已保存。"
            session.updated_at = datetime.now(timezone.utc).isoformat()
            result = self._snapshot(session, include_screenshot=False)
            self._close_locked(session)
            return result

    def close(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            session.status = "closed"
            session.message = "登录会话已关闭。"
            session.updated_at = datetime.now(timezone.utc).isoformat()
            result = self._snapshot(session, include_screenshot=False)
            self._close_locked(session)
            return result

    def _snapshot(self, session: SocialLoginSession, *, include_screenshot: bool) -> dict[str, Any]:
        screenshot = _screenshot_data_url(session.page) if include_screenshot else None
        return {
            "session_id": session.session_id,
            "platform": session.platform,
            "profile_name": session.profile_name,
            "status": session.status,
            "message": session.message,
            "screenshot": screenshot,
            "created_at": session.created_at,
            "updated_at": session.updated_at,
        }

    def _require(self, session_id: str) -> SocialLoginSession:
        session = self._sessions.get(session_id)
        if session is None:
            raise SocialAccessError("LOGIN_SESSION_NOT_FOUND", "Social login session not found")
        return session

    def _close_locked(self, session: SocialLoginSession) -> None:
        self._sessions.pop(session.session_id, None)
        try:
            session.context.close()
        finally:
            session.playwright.stop()


def _try_open_login(page: Any) -> None:
    for pattern in ("登录", "登陆", "Login"):
        try:
            page.get_by_text(re.compile(pattern, re.IGNORECASE)).first.click(timeout=1_500)
            page.wait_for_timeout(800)
            return
        except Exception:
            continue


def _body_text(page: Any) -> str:
    try:
        return str(page.locator("body").inner_text(timeout=1_000) or "")[:10_000]
    except Exception:
        return ""


def _detect_login_page_issue(page: Any, platform: str) -> tuple[str, str] | None:
    """Classify platform-level login failures before showing a scan workflow."""
    current_url = str(getattr(page, "url", "") or "")
    body_text = _body_text(page)
    if platform == "xiaohongshu" and (
        "300012" in current_url
        or "300012" in body_text
        or "IP存在风险" in body_text
    ):
        return (
            "blocked",
            "小红书拒绝了当前采集器网络环境（错误码 300012），因此未生成二维码。请切换可靠网络环境后重试。",
        )
    return None


def _screenshot_data_url(page: Any) -> str | None:
    try:
        encoded = base64.b64encode(page.screenshot(type="png")).decode("ascii")
        return f"data:image/png;base64,{encoded}"
    except Exception:
        return None


def _platform(value: str) -> str:
    normalized = str(value or "").strip().lower()
    if normalized not in PLATFORM_HOME_URLS:
        raise ValueError("platform must be one of: douyin, xiaohongshu")
    return normalized
