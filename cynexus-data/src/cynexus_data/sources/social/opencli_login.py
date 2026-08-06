from __future__ import annotations

import subprocess
import threading
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Callable

from cynexus_data.sources.social.browser import SocialAccessError
from cynexus_data.sources.social.opencli import OpenCliSocialClient, terminate_process_tree


@dataclass
class OpenCliLoginSession:
    session_id: str
    platform: str
    profile_name: str
    client: OpenCliSocialClient
    process: subprocess.Popen[str]
    probe_url: str | None = None
    probe_login: bool = False
    status: str = "waiting_login"
    message: str | None = "请在 Chrome 中完成登录；OpenCLI 会复用 Chrome 的登录会话。"
    stdout: str = ""
    stderr: str = ""
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class OpenCliLoginSessionManager:
    def __init__(self, client_factory: Callable[[str, str], OpenCliSocialClient]) -> None:
        self._client_factory = client_factory
        self._sessions: dict[str, OpenCliLoginSession] = {}
        self._lock = threading.RLock()

    def start(self, platform: str, profile_name: str = "default", probe_url: str | None = None) -> dict[str, Any]:
        normalized = str(platform or "").strip().lower()
        if normalized not in {"douyin", "xiaohongshu"}:
            raise ValueError("platform must be one of: douyin, xiaohongshu")
        with self._lock:
            active = next(
                (
                    session
                    for session in self._sessions.values()
                    if session.platform == normalized and session.profile_name == profile_name
                    and session.status in {"waiting_login", "authenticated"}
                ),
                None,
            )
            if active and active.status == "waiting_login":
                if probe_url and not active.probe_url:
                    active.probe_url = probe_url
                self._refresh(active)
                return self._snapshot(active)
            client = self._client_factory(normalized, profile_name)
            process = client.start_login_process(probe_url)
            session = OpenCliLoginSession(
                session_id=f"social_login_{uuid.uuid4().hex}",
                platform=normalized,
                profile_name=profile_name,
                client=client,
                process=process,
                probe_url=probe_url,
                probe_login=client.login_requires_probe(),
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
            self._refresh(session, probe=True)
            result = self._snapshot(session)
            if session.status == "authenticated":
                self._stop_process(session)
                self._sessions.pop(session.session_id, None)
            return result

    def close(self, session_id: str) -> dict[str, Any]:
        with self._lock:
            session = self._require(session_id)
            if session.process.poll() is None:
                terminate_process_tree(session.process)
                try:
                    session.process.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    session.process.kill()
                    session.process.wait(timeout=3)
            session.status = "closed"
            session.message = "登录流程已关闭。"
            session.updated_at = datetime.now(timezone.utc).isoformat()
            result = self._snapshot(session)
            self._sessions.pop(session.session_id, None)
            return result

    def _refresh(self, session: OpenCliLoginSession, *, probe: bool = False) -> None:
        if session.process.poll() is None:
            if not (probe and session.probe_login):
                return
            self._probe_public_login(session)
            return
        if not session.stdout and not session.stderr:
            session.stdout, session.stderr = session.process.communicate()
        if session.process.returncode != 0:
            error = session.client.command_error(session.stdout, session.stderr, session.process.returncode or 1)
            if session.probe_login and error.code == "LOGIN_REQUIRED":
                session.status = "waiting_login"
                session.message = "请在 Chrome 打开的博主主页完成登录，然后点击“检查登录状态”。"
            else:
                session.status = "blocked" if error.code == "OPENCLI_BROWSER_NOT_CONNECTED" else "failed"
                session.message = str(error)
        elif session.probe_login:
            if not probe:
                session.status = "authenticated"
                session.message = "登录成功，OpenCLI 已保存 Chrome 登录会话。"
            else:
                self._probe_public_login(session)
        else:
            session.status = "authenticated"
            session.message = "登录成功，OpenCLI 已保存 Chrome 登录会话。"
        session.updated_at = datetime.now(timezone.utc).isoformat()

    def _probe_public_login(self, session: OpenCliLoginSession) -> None:
        try:
            session.client.probe_login(session.probe_url)
        except SocialAccessError as error:
            if error.code == "LOGIN_REQUIRED":
                session.status = "waiting_login"
                session.message = "请在 Chrome 打开的博主主页完成登录，然后点击“检查登录状态”。"
            else:
                session.status = "failed"
                session.message = str(error)
        else:
            session.status = "authenticated"
            session.message = "登录成功，OpenCLI 已保存 Chrome 登录会话。"
        session.updated_at = datetime.now(timezone.utc).isoformat()

    def _stop_process(self, session: OpenCliLoginSession) -> None:
        if session.process.poll() is None:
            terminate_process_tree(session.process)
            try:
                session.process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                session.process.kill()
                session.process.wait(timeout=3)

    def _snapshot(self, session: OpenCliLoginSession) -> dict[str, Any]:
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

    def _require(self, session_id: str) -> OpenCliLoginSession:
        session = self._sessions.get(session_id)
        if session is None:
            raise SocialAccessError("LOGIN_SESSION_NOT_FOUND", "Social login session not found")
        return session
