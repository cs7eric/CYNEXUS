from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
import json
from typing import Any, Iterable

from sqlalchemy import text

from cynexus_data.sources.stock_information.content import extract_content
from cynexus_data.storage.postgres.client import PostgresClient
from cynexus_data.storage.postgres.ids import next_bigint_id


@dataclass(frozen=True)
class MarketInformationIdentity:
    source: str
    source_record_id: str
    title: str
    summary: str
    url: str | None


def extract_and_persist(client: PostgresClient, identities: Iterable[MarketInformationIdentity], *, force: bool = False) -> dict[str, int]:
    counts = {"ready": 0, "failed": 0, "needs_ocr": 0, "unsupported": 0, "summary_only": 0}
    requested = list(identities)
    eligible = requested if force else _eligible_identities(client, requested)
    counts["skipped_cached"] = 0
    for identity in eligible:
        result = extract_content(url=identity.url, title=identity.title)
        content_quality = "full_text"
        extraction_message = result.message
        if result.status != "ready" or not result.text.strip():
            fallback_text = _fallback_text(identity.title, identity.summary)
            if fallback_text:
                result = extract_content(url=identity.url, title=identity.title, inline_text=fallback_text)
                content_quality = "summary_only"
                counts["summary_only"] += 1
        payload = result.as_dict()
        if content_quality == "summary_only":
            payload["message"] = extraction_message
        _persist(client, identity, payload, content_quality)
        counts[str(result.status)] = counts.get(str(result.status), 0) + 1
    counts["skipped_cached"] = max(0, len(requested) - len(eligible))
    return counts


def recent_backfill_identities(client: PostgresClient, limit: int = 20, *, force: bool = False) -> list[MarketInformationIdentity]:
    if limit <= 0:
        return []
    cooldown_clause = "" if force else f"""
                   AND NOT EXISTS (
                       SELECT 1 FROM {client.schema}.market_information_content_cache cache
                        WHERE cache.source = raw.source AND cache.source_record_id = raw.source_record_id
                          AND cache.content_quality = 'summary_only'
                          AND cache.extracted_at >= now() - interval '6 hours'
                   )
    """
    with client.session() as db:
        rows = db.execute(
            text(
                f"""
                WITH raw AS (
                    SELECT news.source, news.news_id AS source_record_id, news.title,
                           COALESCE(news.summary, '') AS summary, news.url, news.published_at
                      FROM {client.schema}.a_stock_news news
                     WHERE news.published_at >= now() - interval '30 days'
                    UNION ALL
                    SELECT announcement.source, announcement.announcement_id, announcement.title, '',
                           announcement.url,
                           announcement.publish_date::timestamp AT TIME ZONE 'Asia/Shanghai'
                      FROM {client.schema}.a_stock_announcement announcement
                     WHERE announcement.publish_date >= current_date - 30
                       AND announcement.source IN ('cninfo.market', 'cninfo.regulation')
                )
                SELECT raw.source, raw.source_record_id, raw.title, raw.summary, raw.url
                  FROM raw
                  LEFT JOIN {client.schema}.market_information_event_member member
                    ON member.source = raw.source AND member.source_record_id = raw.source_record_id
                  LEFT JOIN {client.schema}.market_information_event_cluster cluster ON cluster.id = member.cluster_id
                 WHERE NULLIF(trim(raw.url), '') IS NOT NULL
                   AND NOT EXISTS (
                       SELECT 1 FROM {client.schema}.market_information_content_cache cache
                        WHERE cache.source = raw.source AND cache.source_record_id = raw.source_record_id
                          AND cache.status = 'ready' AND cache.content_quality IN ('full_text', 'ocr')
                   )
                   {cooldown_clause}
                 ORDER BY COALESCE(cluster.high_value, false) DESC,
                          COALESCE(cluster.priority_score, 0) DESC,
                          raw.published_at DESC
                 LIMIT :limit
                """
            ),
            {"limit": limit},
        ).mappings().all()
    return [MarketInformationIdentity(**dict(row)) for row in rows]


def identities_from_batches(batches: dict[str, list[dict[str, Any]]]) -> list[MarketInformationIdentity]:
    values: list[MarketInformationIdentity] = []
    for row in batches.get("a_stock_news", []):
        source = str(row.get("source") or "")
        if _market_source(source):
            values.append(MarketInformationIdentity(source, str(row.get("news_id") or ""), str(row.get("title") or ""), str(row.get("summary") or ""), row.get("url")))
    for row in batches.get("a_stock_announcement", []):
        source = str(row.get("source") or "")
        if source in {"cninfo.market", "cninfo.regulation"}:
            values.append(MarketInformationIdentity(source, str(row.get("announcement_id") or ""), str(row.get("title") or ""), "", row.get("url")))
    dedup: dict[tuple[str, str], MarketInformationIdentity] = {}
    for value in values:
        if value.source_record_id and value.title:
            dedup[(value.source, value.source_record_id)] = value
    return list(dedup.values())


def _persist(client: PostgresClient, identity: MarketInformationIdentity, result: dict[str, Any], content_quality: str) -> None:
    content_text = str(result.get("text") or "")
    content_hash = str(result.get("content_hash") or sha256(_fallback_text(identity.title, identity.summary).encode("utf-8")).hexdigest())
    with client.session() as db:
        db.execute(
            text(
                f"""
                INSERT INTO {client.schema}.market_information_content_cache (
                    id, source, source_record_id, source_url, title, content_text, content_hash,
                    content_type, content_quality, status, failure_message, extracted_at, updated_at
                ) VALUES (
                    :id, :source, :source_record_id, :source_url, :title, :content_text, :content_hash,
                    :content_type, :content_quality, :status, :failure_message, CAST(:extracted_at AS timestamptz), now()
                )
                ON CONFLICT (source, source_record_id, content_hash) DO UPDATE SET
                    source_url = EXCLUDED.source_url,
                    title = EXCLUDED.title,
                    content_text = EXCLUDED.content_text,
                    content_type = EXCLUDED.content_type,
                    content_quality = EXCLUDED.content_quality,
                    status = EXCLUDED.status,
                    failure_message = EXCLUDED.failure_message,
                    extracted_at = EXCLUDED.extracted_at,
                    updated_at = now()
                """
            ),
            {
                "id": next_bigint_id(),
                "source": identity.source,
                "source_record_id": identity.source_record_id,
                "source_url": result.get("source_url") or identity.url,
                "title": result.get("title") or identity.title,
                "content_text": content_text,
                "content_hash": content_hash,
                "content_type": result.get("content_type"),
                "content_quality": content_quality,
                "status": result.get("status") or "failed",
                "failure_message": result.get("message"),
                "extracted_at": result.get("extracted_at"),
            },
        )


def _eligible_identities(client: PostgresClient, identities: list[MarketInformationIdentity]) -> list[MarketInformationIdentity]:
    unique = {(item.source, item.source_record_id): item for item in identities if item.source and item.source_record_id}
    if not unique:
        return []
    requested = json.dumps(
        [{"source": source, "source_record_id": record_id} for source, record_id in unique], ensure_ascii=False
    )
    with client.session() as db:
        rows = db.execute(
            text(
                f"""
                WITH requested AS (
                    SELECT * FROM jsonb_to_recordset(CAST(:requested AS jsonb))
                         AS value(source text, source_record_id text)
                )
                SELECT requested.source, requested.source_record_id,
                       bool_or(cache.status = 'ready' AND cache.content_quality IN ('full_text', 'ocr')) AS has_full_text,
                       max(cache.extracted_at) FILTER (WHERE cache.content_quality = 'summary_only') AS last_summary_attempt
                  FROM requested
                  LEFT JOIN {client.schema}.market_information_content_cache cache
                    ON cache.source = requested.source AND cache.source_record_id = requested.source_record_id
                 GROUP BY requested.source, requested.source_record_id
                """
            ),
            {"requested": requested},
        ).mappings().all()
    now = datetime.now(timezone.utc)
    eligible: list[MarketInformationIdentity] = []
    for row in rows:
        identity = unique[(str(row["source"]), str(row["source_record_id"]))]
        last_attempt = row["last_summary_attempt"]
        recent_attempt = last_attempt is not None and (now - last_attempt).total_seconds() < 6 * 3600
        if not bool(row["has_full_text"]) and not recent_attempt:
            eligible.append(identity)
    return eligible


def _fallback_text(title: str, summary: str) -> str:
    return "\n".join(item.strip() for item in (title, summary) if item and item.strip())


def _market_source(source: str) -> bool:
    return source in {"cls.telegraph", "eastmoney.7x24"} or source.startswith(
        ("gov.cn.", "pboc.", "stats.gov.cn.", "mof.", "ndrc.", "csrc.", "szse.", "xinhua.", "cctv.", "caam.", "chinania.")
    )
