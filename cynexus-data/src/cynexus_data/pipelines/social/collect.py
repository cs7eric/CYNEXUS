from __future__ import annotations

import time
from datetime import datetime, timezone
from typing import Any

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.social.comment_sync import _is_post_unavailable
from cynexus_data.pipelines.social.creator_discovery import (
    _filter_posts_for_today,
    _parse_creators,
    _platform,
    _truthy,
)
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.social.models import SocialPost
from cynexus_data.storage.postgres.ids import stable_bigint_id
from cynexus_data.storage.postgres.social import SocialRepository


def run(context: PipelineContext, request: JobRequest) -> dict[str, Any]:
    """Discover today's posts and collect every visible comment in one job run."""
    started = time.perf_counter()
    platform = _platform(request.params.get("platform"))
    scope_key = str(request.params.get("scope_key") or "default")
    creators = _parse_creators(platform, request.params.get("creators"))
    result: dict[str, Any] = {
        "pipeline": "social.collect",
        "platform": platform,
        "scope_key": scope_key,
        "collection_purpose": request.params.get("collection_purpose"),
        "dry_run": request.dry_run,
        "creators_requested": len(creators),
        "creators_succeeded": 0,
        "posts_discovered": 0,
        "posts_requested": 0,
        "posts_succeeded": 0,
        "posts_unavailable": 0,
        "comments_collected": 0,
        "errors": [],
    }
    if request.dry_run:
        result["status"] = "dry_run"
        result["notes"] = ["browser and PostgreSQL access skipped"]
        result["duration_seconds"] = round(time.perf_counter() - started, 3)
        return result
    if context.hooks is None:
        raise RuntimeError("PipelineContext.hooks is required")

    repository = SocialRepository(context.hooks.postgres.get_client())
    if _truthy(request.params.get("create_table")):
        repository.create_table()
    if not creators:
        creators = repository.list_creators(platform, scope_key=scope_key)
    result["creators_requested"] = len(creators)
    if not creators:
        result["status"] = "no_targets"
        result["warnings"] = ["No social creators configured"]
        result["duration_seconds"] = round(time.perf_counter() - started, 3)
        return result

    client = context.hooks.social.get_client(
        platform,
        profile_name=str(request.params.get("profile_name") or "default"),
    )
    posts: list[SocialPost] = []
    seen_post_keys: set[str] = set()
    discovery_started = time.perf_counter()
    for creator in creators:
        try:
            creator_id = repository.upsert_creator(creator, scope_key=scope_key)
            creator_posts = _filter_posts_for_today(
                client.discover_posts(creator),
                now=datetime.now(timezone.utc),
            )
            repository.upsert_posts(creator_id, creator_posts, scope_key=scope_key)
            for post in creator_posts:
                if post.post_key not in seen_post_keys:
                    seen_post_keys.add(post.post_key)
                    posts.append(post)
            result["creators_succeeded"] += 1
            result["posts_discovered"] += len(creator_posts)
        except Exception as exc:  # noqa: BLE001
            result["errors"].append({"creator_key": creator.creator_key, "stage": "discovery", "error": str(exc)})
    result["discovery_seconds"] = round(time.perf_counter() - discovery_started, 3)

    max_posts = _optional_positive_int(request.params.get("max_posts"))
    if max_posts is not None:
        posts = posts[:max_posts]
    result["posts_requested"] = len(posts)

    comment_started = time.perf_counter()
    comment_fetch_seconds = 0.0
    for post in posts:
        post_id = stable_bigint_id("social_post", scope_key, post.platform, post.post_key)
        try:
            fetch_started = time.perf_counter()
            comments = client.fetch_comments(post)
            comment_fetch_seconds += time.perf_counter() - fetch_started
            repository.mark_post_available(post_id)
            repository.upsert_comments(post_id, comments, scope_key=scope_key, replace=True)
            repository.mark_comments_synced(post_id)
            checkpoint_key = f"social.collection.{scope_key}.{platform}.{post.post_key}"
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
        except Exception as exc:  # noqa: BLE001
            if _is_post_unavailable(exc):
                repository.mark_post_unavailable(post_id, str(exc))
                result["posts_unavailable"] += 1
            result["errors"].append({"post_key": post.post_key, "stage": "comments", "error": str(exc)})

    comment_phase_seconds = time.perf_counter() - comment_started
    result["comment_collection_seconds"] = round(comment_phase_seconds, 3)
    result["comment_fetch_seconds"] = round(comment_fetch_seconds, 3)
    result["comment_persist_seconds"] = round(max(0.0, comment_phase_seconds - comment_fetch_seconds), 3)
    result["comments_per_second"] = round(result["comments_collected"] / comment_phase_seconds, 3) if comment_phase_seconds > 0 else 0.0
    result["seconds_per_1000_comments"] = round(
        comment_phase_seconds * 1000 / result["comments_collected"], 3
    ) if result["comments_collected"] > 0 else None
    result["rows_read"] = result["posts_requested"]
    result["rows_written"] = result["comments_collected"]
    result["duration_seconds"] = round(time.perf_counter() - started, 3)
    result["success"] = not result["errors"]
    result["status"] = "success" if result["success"] else "partial_success"
    return result


def _optional_positive_int(value: Any) -> int | None:
    if value in (None, "", 0, "0"):
        return None
    try:
        parsed = int(value)
    except (TypeError, ValueError) as exc:
        raise ValueError("max_posts must be a positive integer") from exc
    if parsed <= 0:
        raise ValueError("max_posts must be a positive integer")
    return parsed
