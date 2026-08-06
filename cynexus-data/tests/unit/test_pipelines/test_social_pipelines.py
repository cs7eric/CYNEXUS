from __future__ import annotations

import json
from datetime import datetime, timezone

from cynexus_data.jobs.contracts import JobRequest
from cynexus_data.pipelines.social.comment_sync import run as run_comment_sync
from cynexus_data.pipelines.social.creator_discovery import (
    _filter_posts_for_today,
    run as run_creator_discovery,
)
from cynexus_data.runtime.context import PipelineContext
from cynexus_data.sources.social.models import SocialPost


def test_creator_discovery_dry_run_does_not_require_browser_or_database() -> None:
    request = JobRequest(
        job_key="social_creator_discovery",
        dry_run=True,
        params={
            "platform": "xiaohongshu",
            "creators": json.dumps([{"creator_key": "creator-001", "profile_url": "https://example.test/u/1"}]),
        },
    )

    result = run_creator_discovery(PipelineContext(), request)

    assert result["status"] == "dry_run"
    assert result["creators_requested"] == 1
    assert result["posts_discovered"] == 0


def test_comment_sync_dry_run_accepts_bounded_post_scope() -> None:
    request = JobRequest(
        job_key="social_comment_sync",
        dry_run=True,
        params={
            "platform": "douyin",
            "posts": [{"post_key": "video-001", "url": "https://example.test/video/1"}],
            "max_posts": 1,
        },
    )

    result = run_comment_sync(PipelineContext(), request)

    assert result["status"] == "dry_run"
    assert result["posts_requested"] == 1
    assert result["comments_collected"] == 0


def test_creator_discovery_filters_to_shanghai_calendar_day() -> None:
    posts = [
        SocialPost(
            platform="douyin",
            post_key="today",
            url="https://example.test/video/today",
            published_at=datetime(2026, 8, 5, 1, 0, tzinfo=timezone.utc),
        ),
        SocialPost(
            platform="douyin",
            post_key="yesterday",
            url="https://example.test/video/yesterday",
            published_at=datetime(2026, 8, 4, 15, 59, tzinfo=timezone.utc),
        ),
        SocialPost(
            platform="douyin",
            post_key="unknown-date",
            url="https://example.test/video/unknown-date",
        ),
    ]

    selected = _filter_posts_for_today(
        posts,
        now=datetime(2026, 8, 5, 8, 0, tzinfo=timezone.utc),
    )

    assert [post.post_key for post in selected] == ["today"]
