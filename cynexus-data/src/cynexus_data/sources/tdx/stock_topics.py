from __future__ import annotations

from dataclasses import asdict
from typing import Any

from cynexus_data.domain.stock.sector_meta import extract_industry_from_reason, industry_to_sector_mapping, topic_to_sector_mapping


def create_eltdx_client(pool_size: int = 8, timeout: float = 8.0, host: str | None = None):
    try:
        import eltdx  # type: ignore
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError("eltdx is required for stock sector sync") from exc
    if host:
        return eltdx.TdxClient(host=host, pool_size=pool_size, timeout=timeout)
    return eltdx.TdxClient(pool_size=pool_size, timeout=timeout)


def fetch_stock_topics(client, code: str) -> list[dict[str, Any]]:
    response = client.helpers.stock_topics(code)
    topics = getattr(response, "topics", None) or []
    normalized: list[dict[str, Any]] = []
    for topic in topics:
        raw = asdict(topic) if hasattr(topic, "__dataclass_fields__") else dict(topic)
        normalized.append(
            {
                "topic_id": raw.get("topic_id"),
                "topic_name": raw.get("topic_name"),
                "reason": raw.get("reason") or "",
                "relation_level": float(raw.get("relation_level") or 0),
                "category_raw": int(raw.get("category_raw") or 0),
                "source": "eltdx",
            }
        )
    return normalized


def stock_topics_to_sector_mappings(code: str, topics: list[dict[str, Any]]) -> list[dict[str, Any]]:
    mappings: list[dict[str, Any]] = []
    industry: str | None = None
    for topic in topics:
        if industry is None:
            industry = extract_industry_from_reason(topic.get("reason"))
        mapping = topic_to_sector_mapping(code, topic)
        if mapping is not None:
            mappings.append(mapping)
    if industry:
        mappings.append(industry_to_sector_mapping(code, industry, source="eltdx"))
    deduped = {mapping["sector_id"]: mapping for mapping in mappings}
    return list(deduped.values())
