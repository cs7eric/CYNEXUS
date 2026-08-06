from __future__ import annotations

import threading
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Callable

from cynexus_data.sources.social.browser import SocialAccessError
from cynexus_data.sources.social.cdp import CdpSocialClient


@dataclass
class CdpLoginSession:
    session_id: str
    platform: str
    profile_name: str
    client: CdpSocialClient
    playwright: Any
    browser: Any
    context: Any
    page: Any
    status: str = "waiting_login"
    message: str | None = "请在本机打开的专用 Chrome 窗口中完成登录。"
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class CdpLoginSessionManager:
    def __init__(self, client_factory: Callable[[str, str], CdpSocialClient]) -> None:
        self._client_factory = client_factory
        self._sessions: dict[str, CdpLoginSession] = {}
        self._lock = threading.RLock()

    def start(self, platform: str, profile_name: str = "default") -> dict[str, Any]:
        normalized = _platform(platform)
        with self._lock:
            active = next(
                (item for item in self._sessions.values() if item.platform == normalized and item.profile_name == profile_name),
                None,
            )
            if active:
                self._refresh(active)
                return self._snapshot(active)
            client = self._client_factory(normalized, profile_name)
            playwright, browser, context, page = client.open_login_page()
            session = CdpLoginSession(
                session_id=f"social_login_{uuid.uuid4().hex}",
                platform=normalized,
                profile_name=profile_name,
                client=client,
                playwright=playwright,
                browser=browser,
                context=context,
                page=page,
            )
            self._sessions[session.session_id] = session
            self._refresh(session)
            return self._snapshot(session)

    def get(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            self._refresh(session)
            return self._snapshot(session)

    def complete(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            self._refresh(session)
            result = self._snapshot(session)
            if session.status == "authenticated":
                self._disconnect(session)
                self._sessions.pop(session.session_id, None)
            return result

    def close(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            session.status = "closed"
            session.message = "登录会话已关闭，Chrome Profile 保留。"
            session.updated_at = datetime.now(timezone.utc).isoformat()
            result = self._snapshot(session)
            self._disconnect(session)
            self._sessions.pop(session.session_id, None)
            return result

    def _refresh(self, session: CdpLoginSession) -> None:
        try:
            if session.client.is_authenticated(session.context, session.page):
                session.status = "authenticated"
                session.message = "登录成功，会话已保存在专用 Chrome Profile。"
            else:
                session.status = "waiting_login"
                session.message = "请在本机专用 Chrome 窗口完成扫码/登录，然后点击检查登录状态。"
        except Exception as exc:  # noqa: BLE001
            session.status = "failed"
            session.message = str(exc)
        session.updated_at = datetime.now(timezone.utc).isoformat()

    def _disconnect(self, session: CdpLoginSession) -> None:
        try:
            session.playwright.stop()
        except Exception:
            pass

    def _snapshot(self, session: CdpLoginSession) -> dict[str, Any]:
        return {
            "session_id": session.session_id,
            "platform": session.platform,
            "profile_name": session.profile_name,
            "status": session.status,
            "message": session.message,
            "screenshot": None,
            "created_at": session.created_at,
            "updated_at": session.updated_at,
        }

    def _require(self, session_id: str) -> CdpLoginSession:
        session = self._sessions.get(session_id)
        if session is None:
            raise SocialAccessError("LOGIN_SESSION_NOT_FOUND", "Social login session not found")
        return session


def _platform(value: str) -> str:
    normalized = str(value or "").strip().lower()
    if normalized not in {"douyin", "xiaohongshu"}:
        raise ValueError("platform must be one of: douyin, xiaohongshu")
    return normalized
