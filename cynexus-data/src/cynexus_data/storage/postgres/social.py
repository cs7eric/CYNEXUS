from __future__ import annotations

import json
from datetime import datetime, time, timezone, timedelta
from pathlib import Path
from typing import Any
from zoneinfo import ZoneInfo

from sqlalchemy import text

from cynexus_data.config.settings import PROJECT_ROOT
from cynexus_data.sources.social.models import SocialComment, SocialCreator, SocialPost
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import stable_bigint_id


DDL_PATH = PROJECT_ROOT / "sql" / "postgres" / "20260805_create_social_monitoring_poc.sql"
SHANGHAI_TZ = ZoneInfo("Asia/Shanghai")


class SocialRepository:
    def __init__(self, client: PostgresClient) -> None:
        self.client = client

    def create_table(self) -> None:
        sql = Path(DDL_PATH).read_text(encoding="utf-8").replace("{schema}", self.client.schema)
        with self.client.engine.begin() as connection:
            from sqlalchemy import text

            connection.execute(text(sql))

    def upsert_creator(self, creator: SocialCreator, *, scope_key: str = "default") -> int:
        creator_id = stable_bigint_id("social_creator_monitor", scope_key, creator.platform, creator.creator_key)
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    INSERT INTO {self.client.schema}.social_creator_monitor (
                        id, scope_key, platform, creator_key, profile_url, display_name,
                        enabled, last_seen_at, created_at, updated_at
                    ) VALUES (
                        :id, :scope_key, :platform, :creator_key, :profile_url, :display_name,
                        true, now(), now(), now()
                    )
                    ON CONFLICT (scope_key, platform, creator_key) DO UPDATE SET
                        profile_url = EXCLUDED.profile_url,
                        display_name = COALESCE(EXCLUDED.display_name, social_creator_monitor.display_name),
                        enabled = true,
                        last_seen_at = now(),
                        updated_at = now()
                    """
                ),
                {
                    "id": creator_id,
                    "scope_key": scope_key,
                    "platform": creator.platform,
                    "creator_key": creator.creator_key,
                    "profile_url": creator.profile_url,
                    "display_name": creator.display_name,
                },
            )
        return creator_id

    def list_creators(self, platform: str, *, scope_key: str = "default") -> list[SocialCreator]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT platform, creator_key, profile_url, display_name
                      FROM {self.client.schema}.social_creator_monitor
                     WHERE scope_key = :scope_key AND platform = :platform AND enabled = true
                     ORDER BY creator_key
                    """
                ),
                {"scope_key": scope_key, "platform": platform},
            ).mappings().all()
        return [SocialCreator(**dict(row)) for row in rows]

    def list_post_keys(self, platform: str, *, scope_key: str = "default") -> set[str]:
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT post_key
                      FROM {self.client.schema}.social_post
                     WHERE scope_key = :scope_key AND platform = :platform
                    """
                ),
                {"scope_key": scope_key, "platform": platform},
            ).scalars().all()
        return {str(value) for value in rows}

    def upsert_posts(self, creator_id: int, posts: list[SocialPost], *, scope_key: str = "default") -> int:
        with self.client.session() as db:
            for post in posts:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.social_post (
                            id, scope_key, creator_id, platform, post_key, url, title, published_at,
                            discovered_at, last_comment_sync_at, raw_payload, created_at, updated_at
                        ) VALUES (
                            :id, :scope_key, :creator_id, :platform, :post_key, :url, :title, :published_at,
                            now(), NULL, CAST(:raw_payload AS jsonb), now(), now()
                        )
                        ON CONFLICT (scope_key, platform, post_key) DO UPDATE SET
                            creator_id = EXCLUDED.creator_id,
                            url = EXCLUDED.url,
                            title = COALESCE(EXCLUDED.title, social_post.title),
                            published_at = COALESCE(EXCLUDED.published_at, social_post.published_at),
                            raw_payload = EXCLUDED.raw_payload,
                            updated_at = now()
                        """
                    ),
                    {
                        "id": stable_bigint_id("social_post", scope_key, post.platform, post.post_key),
                        "scope_key": scope_key,
                        "creator_id": creator_id,
                        "platform": post.platform,
                        "post_key": post.post_key,
                        "url": post.url,
                        "title": post.title,
                        "published_at": post.published_at,
                        "raw_payload": _json_payload(post.raw_payload),
                    },
                )
        return len(posts)

    def list_active_posts(
        self,
        platform: str,
        *,
        active_post_days: int = 30,
        scope_key: str = "default",
        limit: int | None = None,
        today_only: bool = True,
    ) -> list[dict[str, Any]]:
        limit_clause = "LIMIT :limit" if limit is not None else ""
        if today_only:
            current = datetime.now(SHANGHAI_TZ)
            start = datetime.combine(current.date(), time.min, tzinfo=SHANGHAI_TZ).astimezone(timezone.utc)
            end = (datetime.combine(current.date(), time.min, tzinfo=SHANGHAI_TZ) + timedelta(days=1)).astimezone(timezone.utc)
            scope_clause = "AND p.published_at >= :published_start AND p.published_at < :published_end"
            scope_params = {"published_start": start, "published_end": end}
        else:
            scope_clause = "AND p.discovered_at >= now() - (:active_post_days * interval '1 day')"
            scope_params = {"active_post_days": active_post_days}
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT p.id, p.platform, p.post_key, p.url, p.title, p.published_at,
                           p.discovered_at, p.last_comment_sync_at, p.raw_payload,
                           c.creator_key, COALESCE(c.display_name, c.creator_key) AS display_name,
                           COUNT(sc.id)::int AS comment_count
                      FROM {self.client.schema}.social_post p
                      JOIN {self.client.schema}.social_creator_monitor c ON c.id = p.creator_id
                      LEFT JOIN {self.client.schema}.social_comment sc ON sc.post_id = p.id
                     WHERE p.scope_key = :scope_key
                       AND p.platform = :platform
                       {scope_clause}
                     GROUP BY p.id, p.platform, p.post_key, p.url, p.title, p.published_at,
                              p.discovered_at, p.last_comment_sync_at, p.raw_payload,
                              c.creator_key, c.display_name
                     ORDER BY COALESCE(p.published_at, p.discovered_at) DESC
                     {limit_clause}
                    """
                ),
                {
                    "scope_key": scope_key,
                    "platform": platform,
                    **scope_params,
                    **({"limit": limit} if limit is not None else {}),
                },
            ).mappings().all()
        return [dict(row) for row in rows]

    def get_post(self, platform: str, post_key: str, *, scope_key: str = "default") -> dict[str, Any] | None:
        with self.client.session() as db:
            row = db.execute(
                text(
                    f"""
                    SELECT p.id, p.platform, p.post_key, p.url, p.title, p.published_at,
                           p.discovered_at, p.last_comment_sync_at, p.raw_payload,
                           c.creator_key, COALESCE(c.display_name, c.creator_key) AS display_name,
                           COUNT(sc.id)::int AS comment_count
                      FROM {self.client.schema}.social_post p
                      JOIN {self.client.schema}.social_creator_monitor c ON c.id = p.creator_id
                      LEFT JOIN {self.client.schema}.social_comment sc ON sc.post_id = p.id
                     WHERE p.scope_key = :scope_key
                       AND p.platform = :platform
                       AND p.post_key = :post_key
                     GROUP BY p.id, p.platform, p.post_key, p.url, p.title, p.published_at,
                              p.discovered_at, p.last_comment_sync_at, p.raw_payload,
                              c.creator_key, c.display_name
                    """
                ),
                {"scope_key": scope_key, "platform": platform, "post_key": post_key},
            ).mappings().first()
        return dict(row) if row else None

    def list_comments(
        self,
        platform: str,
        post_key: str,
        *,
        scope_key: str = "default",
        limit: int | None = None,
    ) -> list[dict[str, Any]]:
        limit_clause = "LIMIT :limit" if limit is not None else ""
        params: dict[str, Any] = {"scope_key": scope_key, "platform": platform, "post_key": post_key}
        if limit is not None:
            params["limit"] = max(1, int(limit))
        with self.client.session() as db:
            rows = db.execute(
                text(
                    f"""
                    SELECT sc.comment_key, sc.post_id, sc.platform, sc.parent_comment_key,
                           sc.content, sc.published_at, sc.like_count, sc.reply_count,
                           sc.author_platform_id_hash,
                           NULLIF(sc.raw_payload->>'author_name', '') AS author_name,
                           sc.first_seen_at, sc.last_seen_at,
                           sc.source_fetched_at, sc.raw_payload
                      FROM {self.client.schema}.social_comment sc
                      JOIN {self.client.schema}.social_post p ON p.id = sc.post_id
                     WHERE sc.scope_key = :scope_key
                       AND sc.platform = :platform
                       AND p.post_key = :post_key
                     ORDER BY COALESCE(sc.published_at, sc.first_seen_at) DESC
                     {limit_clause}
                    """
                ),
                params,
            ).mappings().all()
        return [dict(row) for row in rows]

    def upsert_comments(
        self,
        post_id: int,
        comments: list[SocialComment],
        *,
        scope_key: str = "default",
        replace: bool = False,
    ) -> int:
        captured_at = datetime.now(timezone.utc)
        with self.client.session() as db:
            if replace:
                db.execute(
                    text(
                        f"""
                        DELETE FROM {self.client.schema}.social_comment
                         WHERE scope_key = :scope_key AND post_id = :post_id
                        """
                    ),
                    {"scope_key": scope_key, "post_id": post_id},
                )
            for comment in comments:
                db.execute(
                    text(
                        f"""
                        INSERT INTO {self.client.schema}.social_comment (
                            id, scope_key, post_id, platform, comment_key, parent_comment_key, content,
                            published_at, like_count, reply_count, author_platform_id_hash,
                            first_seen_at, last_seen_at, source_fetched_at, raw_payload, created_at, updated_at
                        ) VALUES (
                            :id, :scope_key, :post_id, :platform, :comment_key, :parent_comment_key, :content,
                            :published_at, :like_count, :reply_count, :author_platform_id_hash,
                            now(), now(), :source_fetched_at, CAST(:raw_payload AS jsonb), now(), now()
                        )
                        ON CONFLICT (scope_key, platform, comment_key) DO UPDATE SET
                            post_id = EXCLUDED.post_id,
                            parent_comment_key = EXCLUDED.parent_comment_key,
                            content = EXCLUDED.content,
                            published_at = COALESCE(EXCLUDED.published_at, social_comment.published_at),
                            like_count = EXCLUDED.like_count,
                            reply_count = EXCLUDED.reply_count,
                            author_platform_id_hash = COALESCE(EXCLUDED.author_platform_id_hash, social_comment.author_platform_id_hash),
                            last_seen_at = now(),
                            source_fetched_at = EXCLUDED.source_fetched_at,
                            raw_payload = EXCLUDED.raw_payload,
                            updated_at = now()
                        """
                    ),
                    {
                        "id": stable_bigint_id("social_comment", scope_key, comment.platform, comment.comment_key),
                        "scope_key": scope_key,
                        "post_id": post_id,
                        "platform": comment.platform,
                        "comment_key": comment.comment_key,
                        "parent_comment_key": comment.parent_comment_key,
                        "content": comment.content,
                        "published_at": comment.published_at,
                        "like_count": comment.like_count,
                        "reply_count": comment.reply_count,
                        "author_platform_id_hash": comment.author_platform_id_hash,
                        "source_fetched_at": captured_at,
                        "raw_payload": _json_payload(comment.raw_payload),
                    },
                )
        return len(comments)

    def mark_comments_synced(self, post_id: int) -> None:
        with self.client.session() as db:
            db.execute(
                text(
                    f"""
                    UPDATE {self.client.schema}.social_post
                       SET last_comment_sync_at = now(), updated_at = now()
                     WHERE id = :post_id
                    """
                ),
                {"post_id": post_id},
            )


def _json_payload(value: Any) -> str:
    return json.dumps(value or {}, ensure_ascii=False, default=str, allow_nan=False)
