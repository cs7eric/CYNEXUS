from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import subprocess
from dataclasses import replace
from datetime import date, datetime, time, timedelta
from pathlib import Path
from typing import Any
from urllib.parse import urlparse
from zoneinfo import ZoneInfo

from cynexus_data.connections.models import Connection
from cynexus_data.sources.social.browser import SocialAccessError
from cynexus_data.sources.social.models import SocialComment, SocialCreator, SocialPost
from cynexus_data.sources.social.parser import extract_post_key, parse_datetime_value


class OpenCliSocialClient:
    """Read-only social collector backed by the user's Chrome + OpenCLI Bridge."""

    def __init__(self, platform: str, connection: Connection, profile_name: str = "default") -> None:
        self.platform = platform
        self.connection = connection
        configured_profile = os.getenv(f"CYNEXUS_OPENCLI_PROFILE_{platform.upper()}", "").strip()
        self.profile_name = configured_profile or profile_name
        configured = connection.extra.get("extra") if isinstance(connection.extra.get("extra"), dict) else connection.extra
        self._configured = configured
        self.timeout_seconds = _bounded_int(configured.get("opencli_timeout_seconds"), 120, 10, 900)
        self.login_timeout_seconds = _bounded_int(configured.get("opencli_login_timeout_seconds"), 300, 30, 900)
        self.post_limit = _bounded_int(configured.get("opencli_post_limit"), 15, 1, 100)
        self.comment_limit = _bounded_int(configured.get("opencli_comment_limit"), 50, 1, 50)
        self.publish_date_probe_wait_seconds = _bounded_int(configured.get("opencli_publish_date_probe_wait_seconds"), 2, 1, 10)
        self.package = str(configured.get("opencli_package") or "@jackwener/opencli@1.8.6")
        self.executable = str(configured.get("opencli_executable") or os.getenv("CYNEXUS_OPENCLI_EXECUTABLE") or "").strip()

    def discover_posts(self, creator: SocialCreator) -> list[SocialPost]:
        if self.platform == "xiaohongshu":
            rows = self._run_json(
                "xiaohongshu",
                "user",
                creator.profile_url,
                "--limit",
                str(self.post_limit),
            )
            posts = [self._xhs_post(creator, row) for row in rows if isinstance(row, dict)]
            return self._probe_xhs_publish_dates(posts)

        sec_uid = _douyin_sec_uid(creator.creator_key, creator.profile_url)
        rows = self._run_json(
            "douyin",
            "user-videos",
            sec_uid,
            "--limit",
            str(min(self.post_limit, 20)),
            "--with_comments",
            "true",
            "--comment_limit",
            str(min(self.comment_limit, 10)),
        )
        return [self._douyin_post(creator, sec_uid, row) for row in rows if isinstance(row, dict)]

    def fetch_comments(self, post: SocialPost) -> list[SocialComment]:
        if self.platform == "xiaohongshu":
            rows = self._run_json(
                "xiaohongshu",
                "comments",
                post.url,
                "--limit",
                str(self.comment_limit),
                "--with-replies",
                "true",
            )
            return _comments_from_rows(self.platform, post.post_key, rows)

        raw = post.raw_payload if isinstance(post.raw_payload, dict) else {}
        sec_uid = str(raw.get("sec_uid") or "").strip()
        if not sec_uid:
            raise SocialAccessError(
                "OPENCLI_MISSING_CREATOR_ID",
                f"Douyin post {post.post_key} does not retain sec_uid; rediscover the creator first",
            )
        rows = self._run_json(
            "douyin",
            "user-videos",
            sec_uid,
            "--limit",
            "20",
            "--with_comments",
            "true",
            "--comment_limit",
            str(min(self.comment_limit, 10)),
        )
        for row in rows:
            if isinstance(row, dict) and str(row.get("aweme_id") or "").strip() == post.post_key:
                return _comments_from_rows(self.platform, post.post_key, row.get("top_comments") or [])
        return []

    def start_login_process(self, login_url: str | None = None) -> subprocess.Popen[str]:
        self.ensure_browser_bridge()
        if self.platform == "xiaohongshu":
            # The adapter's `login` command targets creator.xiaohongshu.com,
            # while discovery/comments use the public www.xiaohongshu.com
            # session. Keep the public profile tab open while the user logs in.
            if login_url:
                return self._spawn(
                    "xiaohongshu",
                    "user",
                    login_url,
                    "--limit",
                    "1",
                    "--window",
                    "foreground",
                    "--site-session",
                    "persistent",
                    "--keep-tab",
                    "true",
                )
            return self._spawn(
                "browser",
                "default",
                "tab",
                "new",
                "https://www.xiaohongshu.com",
                "--keep-tab",
                "true",
            )
        return self._spawn(
            self.platform,
            "login",
            "--timeout",
            str(self.login_timeout_seconds),
            "--window",
            "foreground",
            "--site-session",
            "persistent",
        )

    def login_requires_probe(self) -> bool:
        return self.platform == "xiaohongshu"

    def probe_login(self, profile_url: str | None = None) -> None:
        if self.platform == "xiaohongshu":
            if not profile_url:
                raise SocialAccessError(
                    "LOGIN_REQUIRED",
                    "请先配置一个小红书博主主页链接，才能检查公开站点登录状态。",
                )
            self._run_json(
                "xiaohongshu",
                "user",
                profile_url,
                "--limit",
                "1",
                "--window",
                "background",
                "--site-session",
                "persistent",
                "--keep-tab",
                "true",
            )

    def ensure_browser_bridge(self) -> None:
        """Fail fast when the Chrome extension/bridge is unavailable."""
        process = self._spawn(self.platform, "whoami", "-f", "json")
        try:
            stdout, stderr = process.communicate(timeout=20)
        except subprocess.TimeoutExpired as exc:
            terminate_process_tree(process)
            raise SocialAccessError("OPENCLI_BROWSER_TIMEOUT", "OpenCLI Browser Bridge 响应超时") from exc
        if process.returncode != 0:
            error = self.command_error(stdout, stderr, process.returncode)
            if error.code == "LOGIN_REQUIRED":
                return
            raise error

    def command_error(self, stdout: str, stderr: str, returncode: int) -> SocialAccessError:
        combined = f"{stderr}\n{stdout}".strip()
        code = _opencli_error_code(combined)
        message = _opencli_error_message(combined) or f"OpenCLI exited with code {returncode}"
        mapped = {
            "BROWSER_CONNECT": "OPENCLI_BROWSER_NOT_CONNECTED",
            "AUTH_REQUIRED": "LOGIN_REQUIRED",
            "SECURITY_BLOCK": "PLATFORM_BLOCKED",
            "EMPTY_RESULT": "NO_DATA",
        }.get(code, code or "OPENCLI_COMMAND_FAILED")
        if mapped == "OPENCLI_BROWSER_NOT_CONNECTED":
            message = "OpenCLI Browser Bridge 未连接。请安装并启用 Chrome 扩展，然后保持 Chrome 打开。"
        elif mapped == "LOGIN_REQUIRED":
            message = "Chrome 中的小红书/抖音会话尚未登录，请先在页面点击登录并完成授权。"
        return SocialAccessError(mapped, message)

    def _run_json(self, *args: str) -> list[dict[str, Any]]:
        process = self._spawn(*args, "-f", "json")
        try:
            stdout, stderr = process.communicate(timeout=self.timeout_seconds)
        except subprocess.TimeoutExpired as exc:
            terminate_process_tree(process)
            raise SocialAccessError("OPENCLI_TIMEOUT", f"OpenCLI command timed out after {self.timeout_seconds}s") from exc
        if process.returncode != 0:
            raise self.command_error(stdout, stderr, process.returncode)
        try:
            payload = json.loads(stdout)
        except json.JSONDecodeError as exc:
            raise SocialAccessError("OPENCLI_INVALID_OUTPUT", "OpenCLI returned invalid JSON output") from exc
        if isinstance(payload, dict):
            return [payload]
        if not isinstance(payload, list):
            raise SocialAccessError("OPENCLI_INVALID_OUTPUT", "OpenCLI returned a non-array result")
        return [row for row in payload if isinstance(row, dict)]

    def _spawn(self, *args: str) -> subprocess.Popen[str]:
        command = self._command_prefix()
        if self.profile_name and self.profile_name != "default":
            command.extend(["--profile", self.profile_name])
        command.extend(args)
        try:
            return subprocess.Popen(
                command,
                cwd=str(_project_root()),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding="utf-8",
                errors="replace",
            )
        except FileNotFoundError as exc:
            raise SocialAccessError(
                "OPENCLI_NOT_INSTALLED",
                "未找到 OpenCLI。请安装 Node.js，并确保 npx 或 opencli 在 PATH 中。",
            ) from exc

    def _command_prefix(self) -> list[str]:
        executable = self.executable or shutil.which("opencli")
        if executable:
            return [executable]
        if os.name == "nt":
            node = shutil.which("node.exe") or shutil.which("node")
            if node:
                npx_cli = Path(node).resolve().parent / "node_modules" / "npm" / "bin" / "npx-cli.js"
                if npx_cli.exists():
                    # Calling npx.cmd through Windows command processing lets
                    # ampersands in xsec_token URLs become shell operators.
                    return [node, str(npx_cli), "--yes", self.package]
        npx = shutil.which("npx.cmd") or shutil.which("npx")
        if npx:
            return [npx, "--yes", self.package]
        return ["opencli"]

    def _xhs_post(self, creator: SocialCreator, row: dict[str, Any]) -> SocialPost:
        url = str(row.get("url") or "").strip()
        post_key = str(row.get("id") or extract_post_key(self.platform, url) or "").strip()
        if not post_key or not url or "xsec_token=" not in url:
            raise SocialAccessError(
                "OPENCLI_INVALID_POST_URL",
                "OpenCLI 小红书结果缺少带 xsec_token 的帖子 URL，拒绝拼接无效链接",
            )
        return SocialPost(
            platform=self.platform,
            post_key=post_key,
            url=url,
            title=_optional_text(row.get("title")),
            published_at=parse_datetime_value(
                row.get("published_at")
                or row.get("publish_time")
                or row.get("create_time")
                or row.get("time")
                or row.get("timestamp")
            ),
            raw_payload={"creator_key": creator.creator_key, "source": row},
        )

    def _probe_xhs_publish_dates(self, posts: list[SocialPost]) -> list[SocialPost]:
        """XHS public user output omits publish time; read it from each note page."""
        if not posts:
            return posts
        session = f"cynexus-social-date-{hashlib.sha1(self.platform.encode('utf-8')).hexdigest()[:8]}"
        tab_id: str | None = None
        enriched: list[SocialPost] = []
        today = datetime.now(SHANGHAI_TZ).date()
        try:
            tab_id = self._opencli_browser_tab(session, posts[0].url)
            for post in posts:
                if post is not posts[0]:
                    self._run_browser_text(session, "open", post.url, "--tab", tab_id)
                self._run_browser_text(
                    session,
                    "wait",
                    "time",
                    str(self.publish_date_probe_wait_seconds),
                    "--tab",
                    tab_id,
                )
                body = self._run_browser_text(session, "eval", "document.body.innerText", "--tab", tab_id)
                published_at = _extract_xhs_publish_date(body, post.title, datetime.now(SHANGHAI_TZ))
                if published_at is None:
                    continue
                published_date = published_at.astimezone(SHANGHAI_TZ).date()
                if published_date < today:
                    # OpenCLI's user command is newest-first. Once the first
                    # historical note is reached, do not open the remaining
                    # historical URLs just to discard them later.
                    break
                if published_date == today:
                    enriched.append(replace(post, published_at=published_at))
        finally:
            if tab_id:
                self._run_browser_text(session, "tab", "close", tab_id)
        return enriched

    def _opencli_browser_tab(self, session: str, url: str) -> str:
        payload = self._run_browser_text(session, "tab", "new", url)
        try:
            parsed = json.loads(payload)
            page = str(parsed.get("page") or "").strip()
        except json.JSONDecodeError as exc:
            raise SocialAccessError("OPENCLI_DATE_PROBE_INVALID_OUTPUT", "OpenCLI Browser Bridge 返回了无效的标签页信息") from exc
        if not page:
            raise SocialAccessError("OPENCLI_DATE_PROBE_INVALID_OUTPUT", "OpenCLI Browser Bridge 没有返回标签页标识")
        return page

    def _run_browser_text(self, session: str, *args: str) -> str:
        process = self._spawn("browser", session, *args)
        try:
            stdout, stderr = process.communicate(timeout=self.timeout_seconds)
        except subprocess.TimeoutExpired as exc:
            terminate_process_tree(process)
            raise SocialAccessError("OPENCLI_TIMEOUT", f"OpenCLI browser command timed out after {self.timeout_seconds}s") from exc
        if process.returncode != 0:
            raise self.command_error(stdout, stderr, process.returncode)
        return stdout.strip()

    def _douyin_post(self, creator: SocialCreator, sec_uid: str, row: dict[str, Any]) -> SocialPost:
        post_key = str(row.get("aweme_id") or row.get("id") or "").strip()
        if not post_key:
            raise SocialAccessError("OPENCLI_INVALID_POST", "OpenCLI 抖音结果缺少 aweme_id")
        return SocialPost(
            platform=self.platform,
            post_key=post_key,
            url=f"https://www.douyin.com/video/{post_key}",
            title=_optional_text(row.get("title")),
            published_at=parse_datetime_value(row.get("published_at") or row.get("create_time")),
            raw_payload={"creator_key": creator.creator_key, "sec_uid": sec_uid, "source": row},
        )


def _comments_from_rows(platform: str, post_key: str, rows: Any) -> list[SocialComment]:
    if not isinstance(rows, list):
        return []
    comments: list[SocialComment] = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        content = _optional_text(row.get("text") or row.get("content"))
        if not content:
            continue
        parent = _optional_text(row.get("reply_to") or row.get("parent_id"))
        author = _optional_text(row.get("userId") or row.get("author") or row.get("nickname"))
        published = _optional_text(row.get("time") or row.get("published_at"))
        identity = "|".join((platform, post_key, author or "", content, parent or "", published or ""))
        comment_key = str(row.get("comment_key") or row.get("id") or hashlib.sha256(identity.encode("utf-8")).hexdigest())
        comments.append(
            SocialComment(
                platform=platform,
                comment_key=comment_key,
                post_key=post_key,
                content=content,
                parent_comment_key=parent,
                published_at=parse_datetime_value(row.get("published_at")),
                like_count=_optional_int(row.get("likes") or row.get("like_count") or row.get("digg_count")),
                reply_count=_optional_int(row.get("reply_count")),
                author_platform_id_hash=_hash(author),
                raw_payload=row,
            )
        )
    return comments


SHANGHAI_TZ = ZoneInfo("Asia/Shanghai")


def _extract_xhs_publish_date(body: str, title: str | None, now: datetime) -> datetime | None:
    lines = [line.strip() for line in str(body or "").splitlines() if line.strip()]
    start = 0
    if title:
        try:
            start = len(lines) - 1 - lines[::-1].index(title) + 1
        except ValueError:
            pass
    for line in lines[start : start + 20]:
        if line == "今天" or line.startswith("今天 "):
            return datetime.combine(now.astimezone(SHANGHAI_TZ).date(), time.min, tzinfo=SHANGHAI_TZ)
        if line == "昨天" or line.startswith("昨天 "):
            return datetime.combine(now.astimezone(SHANGHAI_TZ).date() - timedelta(days=1), time.min, tzinfo=SHANGHAI_TZ)

        match = re.search(r"(?<!\d)(\d{4})[-/年](\d{1,2})[-/月](\d{1,2})(?:日)?", line)
        if match:
            year, month, day = (int(value) for value in match.groups())
        else:
            match = re.search(r"(?<!\d)(\d{1,2})[-/月](\d{1,2})(?:日)?", line)
            if not match:
                continue
            year = now.astimezone(SHANGHAI_TZ).year
            month, day = (int(value) for value in match.groups())
            candidate = date(year, month, day)
            if candidate > now.astimezone(SHANGHAI_TZ).date() + timedelta(days=1):
                year -= 1
        try:
            return datetime.combine(date(year, month, day), time.min, tzinfo=SHANGHAI_TZ)
        except ValueError:
            return None
    return None


def _douyin_sec_uid(creator_key: str, profile_url: str) -> str:
    parsed = urlparse(profile_url)
    path_parts = [part for part in parsed.path.split("/") if part]
    if path_parts:
        candidate = path_parts[-1]
        if candidate not in {"user", "profile"}:
            return candidate
    key = str(creator_key or "").strip()
    if key:
        return key
    raise SocialAccessError("OPENCLI_MISSING_CREATOR_ID", "抖音博主配置缺少 sec_uid 或有效主页链接")


def _opencli_error_code(value: str) -> str | None:
    for line in value.splitlines():
        stripped = line.strip()
        if stripped.lower().startswith("code:"):
            return stripped.split(":", 1)[1].strip().strip("'\"") or None
        if '"code"' in stripped:
            try:
                parsed = json.loads(stripped.rstrip(","))
                if isinstance(parsed, dict) and parsed.get("code"):
                    return str(parsed["code"])
            except json.JSONDecodeError:
                continue
    return None


def _opencli_error_message(value: str) -> str | None:
    for line in value.splitlines():
        stripped = line.strip()
        if stripped.lower().startswith("message:"):
            return stripped.split(":", 1)[1].strip().strip("'\"") or None
    return None


def _project_root() -> Path:
    configured = os.getenv("CYNEXUS_DATA_PROJECT_ROOT")
    return Path(configured).expanduser() if configured else Path.cwd()


def _bounded_int(value: Any, default: int, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = default
    return max(minimum, min(maximum, parsed))


def _optional_text(value: Any) -> str | None:
    if value in (None, ""):
        return None
    return " ".join(str(value).split()) or None


def _optional_int(value: Any) -> int | None:
    try:
        return int(str(value).replace(",", ""))
    except (TypeError, ValueError):
        return None


def _hash(value: str | None) -> str | None:
    return hashlib.sha256(value.encode("utf-8")).hexdigest() if value else None


def terminate_process_tree(process: subprocess.Popen[str]) -> None:
    """Stop npx/cmd/node descendants as one bounded local command."""
    if process.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(
            ["taskkill.exe", "/PID", str(process.pid), "/T", "/F"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )
        return
    process.kill()
