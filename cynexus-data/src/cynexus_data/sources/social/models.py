from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any


@dataclass(frozen=True)
class SocialCreator:
    platform: str
    creator_key: str
    profile_url: str
    display_name: str | None = None


@dataclass(frozen=True)
class SocialPost:
    platform: str
    post_key: str
    url: str
    title: str | None = None
    published_at: datetime | None = None
    raw_payload: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class SocialComment:
    platform: str
    comment_key: str
    post_key: str
    content: str
    parent_comment_key: str | None = None
    published_at: datetime | None = None
    like_count: int | None = None
    reply_count: int | None = None
    author_platform_id_hash: str | None = None
    raw_payload: dict[str, Any] = field(default_factory=dict)
