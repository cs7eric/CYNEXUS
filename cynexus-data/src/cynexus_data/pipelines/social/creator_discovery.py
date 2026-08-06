from __future__ import annotations

import json
from datetime import datetime, time, timedelta, timezone
from typing import Any
from zoneinfo import ZoneInfo

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.social.models import SocialCreator
from cynexus_data.storage.postgres.social import SocialRepository


SUPPORTED_PLATFORMS = {"douyin", "xiaohongshu"}
SHANGHAI_TZ = ZoneInfo("Asia/Shanghai")


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    platform = _platform(request.params.get("platform"))
    scope_key = str(request.params.get("scope_key") or "default")
    creators = _parse_creators(platform, request.params.get("creators"))[:50]
    lookback_hours = _bounded_int(request.params.get("lookback_hours"), 24, 1, 24)
    result: dict[str, Any] = {
        "pipeline": "social.creator_discovery",
        "platform": platform,
        "scope_key": scope_key,
        "dry_run": request.dry_run,
        "creators_requested": len(creators),
        "creators_succeeded": 0,
        "posts_discovered": 0,
        "errors": [],
    }
    if request.dry_run:
        result["status"] = "dry_run"
        result["notes"] = ["browser and PostgreSQL access skipped"]
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    repository = SocialRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repository.create_table()
    if not creators:
        creators = repository.list_creators(platform, scope_key=scope_key)
    if not creators:
        result["status"] = "no_targets"
        result["warnings"] = ["No social creators configured"]
        return result

    client = context.hooks.social.get_client(platform, profile_name=str(request.params.get("profile_name") or "default"))
    for creator in creators:
        try:
            creator_id = repository.upsert_creator(creator, scope_key=scope_key)
            posts = _filter_posts_for_today(
                client.discover_posts(creator),
                now=datetime.now(timezone.utc),
            )
            repository.upsert_posts(creator_id, posts, scope_key=scope_key)
            checkpoint_key = f"social.discovery.{scope_key}.{platform}.{creator.creator_key}"
            checkpoint = context.checkpoints.get(checkpoint_key) or {}
            checkpoint.update(
                {
                    "last_success_at": datetime.now(timezone.utc).isoformat(),
                    "recent_post_ids": [post.post_key for post in posts[:50]],
                    "lookback_hours": lookback_hours,
                }
            )
            context.checkpoints.put(checkpoint_key, request.job_key, checkpoint)
            result["creators_succeeded"] += 1
            result["posts_discovered"] += len(posts)
        except Exception as exc:
            result["errors"].append({"creator_key": creator.creator_key, "error": str(exc)})

    result["rows_read"] = result["creators_requested"]
    result["rows_written"] = result["posts_discovered"]
    result["success"] = not result["errors"]
    result["status"] = "success" if result["success"] else "partial_success"
    return result


def _platform(value: Any) -> str:
    platform = str(value or "").strip().lower()
    if platform not in SUPPORTED_PLATFORMS:
        raise ValueError("platform must be one of: douyin, xiaohongshu")
    return platform


def _parse_creators(platform: str, value: Any) -> list[SocialCreator]:
    if value in (None, "", []):
        return []
    if isinstance(value, str):
        value = json.loads(value)
    if not isinstance(value, list):
        raise ValueError("creators must be a JSON array")
    creators: list[SocialCreator] = []
    for item in value:
        if not isinstance(item, dict):
            raise ValueError("creator entries must be JSON objects")
        creator_key = str(item.get("creator_key") or item.get("creatorKey") or item.get("id") or "").strip()
        profile_url = str(item.get("profile_url") or item.get("profileUrl") or item.get("url") or "").strip()
        if not creator_key or not profile_url:
            raise ValueError("creator_key and profile_url are required")
        creators.append(
            SocialCreator(
                platform=platform,
                creator_key=creator_key,
                profile_url=profile_url,
                display_name=str(item.get("display_name") or item.get("displayName") or "").strip() or None,
            )
        )
    return creators


def _truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _filter_posts_for_today(
    posts: list[Any],
    *,
    known_post_keys: set[str] | None = None,
    now: datetime | None = None,
) -> list[Any]:
    """Keep only posts whose provider timestamp falls on today's Shanghai date."""
    current = (now or datetime.now(timezone.utc)).astimezone(SHANGHAI_TZ)
    start = datetime.combine(current.date(), time.min, tzinfo=SHANGHAI_TZ)
    end = start + timedelta(days=1)
    selected: list[Any] = []
    for post in posts:
        published_at = getattr(post, "published_at", None)
        if published_at is not None:
            published = _as_utc(published_at).astimezone(SHANGHAI_TZ)
            if start <= published < end:
                selected.append(post)
            continue
    return selected


def _as_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def _bounded_int(value: Any, default: int, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = default
    return max(minimum, min(maximum, parsed))
