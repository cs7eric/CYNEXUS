from __future__ import annotations

import hashlib
import re
from datetime import datetime, timedelta, timezone
from html import unescape
from typing import Any
from urllib.parse import urlparse
from zoneinfo import ZoneInfo

from cynexus_data.sources.social.models import SocialComment, SocialPost


PLATFORM_POST_PATTERNS: dict[str, tuple[re.Pattern[str], ...]] = {
    "xiaohongshu": (
        re.compile(r"/(?:explore|discovery/item)/([\w-]+)"),
        re.compile(r"/user/profile/[A-Za-z0-9]+/([\w-]+)"),
    ),
    "douyin": (re.compile(r"/(?:video|note)/([\w-]+)"),),
}


def parse_post_links(platform: str, rows: list[dict[str, Any]]) -> list[SocialPost]:
    posts: list[SocialPost] = []
    seen: set[str] = set()
    for row in rows:
        url = str(row.get("href") or "").strip()
        post_key = extract_post_key(platform, url) or str(row.get("post_key") or "").strip()
        if not post_key or not url or post_key in seen:
            continue
        seen.add(post_key)
        posts.append(
            SocialPost(
                platform=platform,
                post_key=post_key,
                url=url,
                title=_clean_text(row.get("text") or row.get("aria_label")),
                published_at=_parse_datetime(row.get("published_at")),
                raw_payload={"href": url, "text": row.get("text"), "aria_label": row.get("aria_label")},
            )
        )
    return posts


def parse_comments(platform: str, post_key: str, rows: list[dict[str, Any]]) -> list[SocialComment]:
    comments: list[SocialComment] = []
    seen: set[str] = set()
    for row in rows:
        comment_key = str(row.get("comment_key") or row.get("id") or "").strip()
        content = _clean_text(row.get("content") or row.get("text")) or ""
        if not comment_key or not content or comment_key in seen:
            continue
        seen.add(comment_key)
        comments.append(
            SocialComment(
                platform=platform,
                comment_key=comment_key,
                post_key=post_key,
                content=content,
                parent_comment_key=_optional_text(row.get("parent_comment_key") or row.get("parent_id")),
                published_at=parse_comment_datetime(row.get("published_at")),
                like_count=_optional_int(row.get("like_count")),
                reply_count=_optional_int(row.get("reply_count")),
                author_platform_id_hash=_hash_author(row.get("author_id")),
                raw_payload={key: value for key, value in row.items() if key not in {"content", "text"}},
            )
        )
    return comments


def extract_post_key(platform: str, url: str) -> str | None:
    normalized = urlparse(url).path
    for pattern in PLATFORM_POST_PATTERNS.get(platform, ()):
        match = pattern.search(normalized)
        if match:
            return match.group(1)
    return None


def parse_datetime_value(value: Any) -> datetime | None:
    """Parse a provider timestamp when the source exposes an ISO-like value."""
    return _parse_datetime(value)


def parse_comment_datetime(value: Any, *, now: datetime | None = None) -> datetime | None:
    """Parse the relative timestamps used in visible Chinese comment rows."""
    parsed = _parse_datetime(value)
    if parsed is not None:
        return parsed
    text = _clean_text(value)
    if not text:
        return None
    current = now or datetime.now(ZoneInfo("Asia/Shanghai"))
    if current.tzinfo is None:
        current = current.replace(tzinfo=ZoneInfo("Asia/Shanghai"))
    if "刚刚" in text:
        return current
    match = re.search(r"(\d+)\s*(秒|分钟|小时|天)前", text)
    if match:
        amount = int(match.group(1))
        unit = {"秒": "seconds", "分钟": "minutes", "小时": "hours", "天": "days"}[match.group(2)]
        return current - timedelta(**{unit: amount})
    if text.startswith("昨天"):
        clock = re.search(r"(\d{1,2}):(\d{2})", text)
        base = current - timedelta(days=1)
        return base.replace(hour=int(clock.group(1)), minute=int(clock.group(2)), second=0, microsecond=0) if clock else base.replace(hour=0, minute=0, second=0, microsecond=0)
    if text.startswith("前天"):
        clock = re.search(r"(\d{1,2}):(\d{2})", text)
        base = current - timedelta(days=2)
        return base.replace(hour=int(clock.group(1)), minute=int(clock.group(2)), second=0, microsecond=0) if clock else base.replace(hour=0, minute=0, second=0, microsecond=0)
    return None


def _clean_text(value: Any) -> str | None:
    if value in (None, ""):
        return None
    cleaned = re.sub(r"\s+", " ", unescape(str(value))).strip()
    return cleaned or None


def _optional_text(value: Any) -> str | None:
    cleaned = _clean_text(value)
    return cleaned or None


def _parse_datetime(value: Any) -> datetime | None:
    if value in (None, ""):
        return None
    if isinstance(value, datetime):
        return value
    if isinstance(value, (int, float)) or (isinstance(value, str) and value.strip().isdigit()):
        try:
            timestamp = float(value)
            if timestamp > 10_000_000_000:
                timestamp /= 1_000
            return datetime.fromtimestamp(timestamp, tz=timezone.utc)
        except (TypeError, ValueError, OverflowError, OSError):
            return None
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None


def _optional_int(value: Any) -> int | None:
    if value in (None, ""):
        return None
    try:
        return int(str(value).replace(",", ""))
    except (TypeError, ValueError):
        return None


def _hash_author(value: Any) -> str | None:
    cleaned = _clean_text(value)
    if not cleaned:
        return None
    return hashlib.sha256(cleaned.encode("utf-8")).hexdigest()
