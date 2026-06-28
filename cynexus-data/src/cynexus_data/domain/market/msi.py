from __future__ import annotations

from datetime import date
from typing import Any


WEIGHTS: dict[str, float] = {
    "vol": 0.15,
    "turnover": 0.15,
    "price_strength": 0.10,
    "risk_appetite": 0.10,
    "breadth": 0.15,
    "limit_emotion": 0.15,
    "profit_effect": 0.10,
    "sector_breadth": 0.05,
    "style_risk": 0.05,
}
MISSING_SCORE = 50.0


def calculate_market_sentiment_index(
    *,
    trade_date: date,
    components: dict[str, float | None],
    require_full: bool = False,
) -> dict[str, Any] | None:
    normalized = {key: _clamp_score(components.get(key)) for key in WEIGHTS}
    present = [key for key, value in normalized.items() if value is not None]
    if not present:
        return None
    if require_full and len(present) < len(WEIGHTS):
        return None
    composite = round(sum(WEIGHTS[key] * (normalized[key] if normalized[key] is not None else MISSING_SCORE) for key in WEIGHTS), 2)
    return {
        "tradeDate": trade_date.isoformat(),
        "components": normalized,
        "weights": dict(WEIGHTS),
        "compositeScore": composite,
        "componentCount": len(present),
        "level": msi_level(composite),
        "source": "composite",
    }


def msi_level(score: float) -> str:
    if score >= 70:
        return "hot"
    if score >= 55:
        return "active"
    if score >= 45:
        return "normal"
    if score >= 30:
        return "weak"
    return "ice"


def percentile_score(history_values: list[float], current_value: float | None) -> float | None:
    if current_value is None:
        return None
    values = [float(value) for value in history_values if value is not None]
    if not values:
        return MISSING_SCORE
    lower_count = sum(1 for value in values if value < current_value)
    return round(lower_count / len(values) * 100, 2)


def sector_breadth_score(*, advancing: int, total: int) -> float | None:
    if total <= 0:
        return None
    return round(max(0.0, min(1.0, advancing / total)) * 100, 2)


def _clamp_score(value: float | int | str | None) -> float | None:
    if value in (None, ""):
        return None
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return round(max(0.0, min(100.0, parsed)), 2)
