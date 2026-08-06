from __future__ import annotations

from datetime import datetime
from zoneinfo import ZoneInfo

from cynexus_data.sources.social.parser import parse_comment_datetime, parse_comments, parse_post_links


def test_parse_post_links_extracts_platform_ids_and_deduplicates() -> None:
    posts = parse_post_links(
        "xiaohongshu",
        [
            {"href": "https://www.xiaohongshu.com/explore/note-001", "text": "第一篇"},
            {"href": "https://www.xiaohongshu.com/explore/note-001", "text": "重复"},
            {"href": "https://www.xiaohongshu.com/explore/note-002", "text": "第二篇"},
        ],
    )

    assert [post.post_key for post in posts] == ["note-001", "note-002"]
    assert posts[0].title == "第一篇"


def test_parse_comments_normalizes_counts_and_hashes_author() -> None:
    comments = parse_comments(
        "douyin",
        "video-001",
        [
            {
                "id": "comment-001",
                "content": "  很有帮助  ",
                "like_count": "1,234",
                "reply_count": "2",
                "author_id": "user-001",
            },
            {"id": "comment-001", "content": "duplicate"},
        ],
    )

    assert len(comments) == 1
    assert comments[0].content == "很有帮助"
    assert comments[0].like_count == 1234
    assert len(comments[0].author_platform_id_hash or "") == 64


def test_parse_comment_datetime_handles_relative_xiaohongshu_time() -> None:
    current = datetime(2026, 8, 5, 22, 23, 24, tzinfo=ZoneInfo("Asia/Shanghai"))

    parsed = parse_comment_datetime("7小时前北京", now=current)

    assert parsed == datetime(2026, 8, 5, 15, 23, 24, tzinfo=ZoneInfo("Asia/Shanghai"))
