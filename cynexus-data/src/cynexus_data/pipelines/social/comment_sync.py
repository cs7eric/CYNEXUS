from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.social.creator_discovery import _filter_posts_for_today
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.social.models import SocialPost
from cynexus_data.sources.social.parser import parse_datetime_value
from cynexus_data.storage.postgres.ids import stable_bigint_id
from cynexus_data.storage.postgres.social import SocialRepository


SUPPORTED_PLATFORMS = {"douyin", "xiaohongshu"}


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    platform = _platform(request.params.get("platform"))
    scope_key = str(request.params.get("scope_key") or "default")
    posts = _parse_posts(platform, request.params.get("posts"))
    result: dict[str, Any] = {
        "pipeline": "social.comment_sync",
        "platform": platform,
        "scope_key": scope_key,
        "dry_run": request.dry_run,
        "posts_requested": len(posts),
        "posts_succeeded": 0,
        "comments_collected": 0,
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
    if not posts:
        posts = [
            SocialPost(
                platform=platform,
                post_key=str(row["post_key"]),
                url=str(row["url"]),
                title=row.get("title"),
                published_at=row.get("published_at"),
                raw_payload=row.get("raw_payload") or {},
            )
            for row in repository.list_active_posts(
                platform,
                active_post_days=_bounded_int(request.params.get("active_post_days"), 30, 1, 90),
                scope_key=scope_key,
                today_only=True,
            )
        ]
    else:
        posts = _filter_posts_for_today(posts, now=datetime.now(timezone.utc))
    max_posts = _bounded_int(request.params.get("max_posts"), len(posts), 1, 200)
    posts = posts[:max_posts]
    result["posts_requested"] = len(posts)
    if not posts:
        result["status"] = "no_targets"
        result["warnings"] = ["No social posts configured"]
        return result

    client = context.hooks.social.get_client(platform, profile_name=str(request.params.get("profile_name") or "default"))
    for post in posts:
        try:
            comments = client.fetch_comments(post)
            post_id = stable_bigint_id("social_post", scope_key, post.platform, post.post_key)
            # A comment fetch is a snapshot. Replace the previous malformed or
            # stale DOM rows so a parser correction is visible immediately.
            repository.upsert_comments(post_id, comments, scope_key=scope_key, replace=True)
            repository.mark_comments_synced(post_id)
            checkpoint_key = f"social.comments.{scope_key}.{platform}.{post.post_key}"
            checkpoint = context.checkpoints.get(checkpoint_key) or {}
            checkpoint.update(
                {
                    "last_success_at": datetime.now(timezone.utc).isoformat(),
                    "comments_seen": len(comments),
                }
            )
            context.checkpoints.put(checkpoint_key, request.job_key, checkpoint)
            result["posts_succeeded"] += 1
            result["comments_collected"] += len(comments)
        except Exception as exc:
            result["errors"].append({"post_key": post.post_key, "error": str(exc)})

    result["rows_read"] = result["posts_requested"]
    result["rows_written"] = result["comments_collected"]
    result["success"] = not result["errors"]
    result["status"] = "success" if result["success"] else "partial_success"
    return result


def _platform(value: Any) -> str:
    platform = str(value or "").strip().lower()
    if platform not in SUPPORTED_PLATFORMS:
        raise ValueError("platform must be one of: douyin, xiaohongshu")
    return platform


def _parse_posts(platform: str, value: Any) -> list[SocialPost]:
    if value in (None, "", []):
        return []
    if isinstance(value, str):
        value = json.loads(value)
    if not isinstance(value, list):
        raise ValueError("posts must be a JSON array")
    posts: list[SocialPost] = []
    for item in value:
        if not isinstance(item, dict):
            raise ValueError("post entries must be JSON objects")
        post_key = str(item.get("post_key") or item.get("id") or "").strip()
        url = str(item.get("url") or "").strip()
        if not post_key or not url:
            raise ValueError("post_key and url are required")
        posts.append(
            SocialPost(
                platform=platform,
                post_key=post_key,
                url=url,
                title=item.get("title"),
                published_at=parse_datetime_value(item.get("published_at")),
            )
        )
    return posts


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
