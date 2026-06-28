from __future__ import annotations

from cynexus_data.domain.stock.sector_meta import extract_industry_from_reason, industry_to_sector_mapping, topic_to_sector_mapping


def test_extract_industry_from_reason() -> None:
    assert extract_industry_from_reason("公司属于白酒（通达信研究行业）") == "白酒"
    assert extract_industry_from_reason("公司主营业务属于半导体设备") == "半导体设备"


def test_topic_and_industry_sector_mapping() -> None:
    topic = topic_to_sector_mapping("000001", {"topic_id": "T1", "topic_name": "人工智能", "category_raw": 2, "relation_level": 0.8, "reason": "x"})
    industry = industry_to_sector_mapping("000001", "银行")

    assert topic is not None
    assert topic["sector_id"] == "2:T1"
    assert topic["category_label"] == "concepts"
    assert industry["sector_id"] == "0:银行"
