from __future__ import annotations

from datetime import date
from typing import Any


SECTION_ORDER = ["period_stats", "main_capital_flow", "related", "constituents", "main_control"]


def build_sector_quote_detail_snapshot(row: dict[str, Any], *, snapshot_date: date) -> dict[str, Any] | None:
    code6 = _code6(row.get("code6") or row.get("sector_code") or row.get("sectorCode"))
    name = _text(row.get("name") or row.get("sector_name") or row.get("sectorName"))
    if not code6 or not name:
        return None
    sections = row.get("sections") if isinstance(row.get("sections"), dict) else {}
    normalized_sections = {
        section: _list_of_dicts(sections.get(section))
        for section in SECTION_ORDER
        if section in sections
    }
    counts = {section: len(items) for section, items in normalized_sections.items()}
    errors = row.get("errors") if isinstance(row.get("errors"), dict) else {}
    payload = {
        "snapshotDate": snapshot_date.isoformat(),
        "code6": code6,
        "fullCode": f"sh{code6}",
        "name": name,
        "seedCode": _text(row.get("seed_code") or row.get("seedCode")),
        "ok": bool(row.get("ok", not errors)),
        "sections": normalized_sections,
        "sectionCounts": counts,
        "errors": dict(errors),
        "source": _text(row.get("source")) or "eltdx.f10.theme_market",
    }
    return {
        "snapshot_date": snapshot_date,
        "code6": code6,
        "name": name,
        "seed_code": payload["seedCode"],
        "payload": payload,
        "section_counts": counts,
        "source": payload["source"],
    }


def build_sector_quote_detail_snapshots(rows: list[dict[str, Any]], *, snapshot_date: date) -> list[dict[str, Any]]:
    snapshots = [build_sector_quote_detail_snapshot(row, snapshot_date=snapshot_date) for row in rows]
    return [snapshot for snapshot in snapshots if snapshot is not None]


def _list_of_dicts(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        return []
    return [dict(item) for item in value if isinstance(item, dict)]


def _code6(value: Any) -> str | None:
    text = _text(value)
    if not text:
        return None
    if text.lower().startswith(("sh", "sz", "bj")):
        text = text[2:]
    return text if len(text) == 6 and text.isdigit() else None


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None
