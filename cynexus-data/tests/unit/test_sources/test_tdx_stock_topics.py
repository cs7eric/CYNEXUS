from __future__ import annotations

from dataclasses import dataclass

from cynexus_data.sources.tdx.stock_topics import stock_topics_to_sector_mappings


@dataclass
class Topic:
    topic_id: str
    topic_name: str
    reason: str
    relation_level: float
    category_raw: int


def test_stock_topics_to_sector_mappings_adds_derived_industry() -> None:
    topics = [
        {
            "topic_id": "T1",
            "topic_name": "人工智能",
            "reason": "公司属于软件服务（通达信研究行业）",
            "relation_level": 0.7,
            "category_raw": 2,
            "source": "eltdx",
        }
    ]

    mappings = stock_topics_to_sector_mappings("000001", topics)

    sector_ids = {item["sector_id"] for item in mappings}
    assert "2:T1" in sector_ids
    assert "0:软件服务" in sector_ids
