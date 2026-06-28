from __future__ import annotations

import json
from datetime import date
from typing import Any


def parse_date(value: Any) -> date | None:
    if value in (None, ""):
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value)[:10])


def parse_rows(value: Any) -> list[dict[str, Any]] | None:
    if value in (None, ""):
        return None
    if isinstance(value, list):
        return [row for row in value if isinstance(row, dict)]
    parsed = json.loads(str(value))
    if not isinstance(parsed, list):
        raise ValueError("rows must be a JSON list")
    return [row for row in parsed if isinstance(row, dict)]


def parse_object(value: Any) -> dict[str, Any] | None:
    if value in (None, ""):
        return None
    if isinstance(value, dict):
        return value
    parsed = json.loads(str(value))
    if not isinstance(parsed, dict):
        raise ValueError("value must be a JSON object")
    return parsed


def parse_int(value: Any, default: int) -> int:
    if value in (None, ""):
        return default
    return int(value)


def truthy(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return False
    return str(value).lower() in {"1", "true", "yes", "y", "on"}
