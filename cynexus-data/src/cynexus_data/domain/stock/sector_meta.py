from __future__ import annotations

import re
from typing import Any


INDUSTRY_CATEGORY_RAW = 0
CATEGORY_LABELS = {
    0: "industries",
    2: "concepts",
    4: "styles",
}

_INDUSTRY_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"公司属于([\u4e00-\u9fa5]+)（通达信研究行业）"),
    re.compile(r"公司主营业务属于([\u4e00-\u9fa5]+)"),
    re.compile(r"公司主营([\u4e00-\u9fa5]{2,16}?)业?[，,。；;是为]"),
    re.compile(r"公司业务属于([\u4e00-\u9fa5]+)"),
    re.compile(r"公司业务为([\u4e00-\u9fa5]{2,16})"),
    re.compile(r"公司业务是([\u4e00-\u9fa5]{2,16}?)的"),
    re.compile(r"公司聚焦于([\u4e00-\u9fa5]+)"),
    re.compile(r"公司主营([\u4e00-\u9fa5]+)"),
)

_INDUSTRY_BLACKLIST = {"研发", "生产", "销售", "服务", "管理", "咨询", "的", "了", "是", "在", "和", "与", "及", "或", "等"}


def category_label(category_raw: int) -> str:
    return CATEGORY_LABELS.get(int(category_raw), f"unknown_{int(category_raw)}")


def extract_industry_from_reason(reason: str | None) -> str | None:
    if not reason:
        return None
    for pattern in _INDUSTRY_PATTERNS:
        match = pattern.search(reason)
        if not match:
            continue
        name = match.group(1).strip()
        if not name or name in _INDUSTRY_BLACKLIST:
            continue
        if len(name) > 16:
            name = name[:8]
        return name
    return None


def sector_id(category_raw: int, name: str, topic_id: str | None = None) -> str:
    token = topic_id or name
    safe = str(token).strip().replace(" ", "_").replace("|", "_")
    return f"{int(category_raw)}:{safe}"


def topic_to_sector_mapping(code: str, topic: dict[str, Any]) -> dict[str, Any] | None:
    topic_name = str(topic.get("topic_name") or topic.get("name") or "").strip()
    if not topic_name:
        return None
    raw = int(topic.get("category_raw") or 0)
    topic_id = str(topic.get("topic_id") or "").strip() or None
    return {
        "code": code,
        "sector_id": sector_id(raw, topic_name, topic_id),
        "category_raw": raw,
        "category_label": category_label(raw),
        "name": topic_name,
        "topic_id": topic_id,
        "relation_level": _optional_float(topic.get("relation_level")),
        "reason": topic.get("reason") or "",
        "source": topic.get("source") or "eltdx",
    }


def industry_to_sector_mapping(code: str, industry: str, source: str = "eltdx") -> dict[str, Any]:
    return {
        "code": code,
        "sector_id": sector_id(INDUSTRY_CATEGORY_RAW, industry),
        "category_raw": INDUSTRY_CATEGORY_RAW,
        "category_label": category_label(INDUSTRY_CATEGORY_RAW),
        "name": industry,
        "topic_id": None,
        "relation_level": None,
        "reason": "",
        "source": source,
    }


def _optional_float(value: Any) -> float | None:
    if value in (None, ""):
        return None
    return float(value)
