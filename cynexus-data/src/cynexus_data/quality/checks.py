from __future__ import annotations

from typing import Any


def assert_no_empty_job_key(job_key: str) -> None:
    if not job_key.strip():
        raise ValueError("job_key is required")


def validate_ohlcv_record(record: dict[str, Any]) -> list[str]:
    issues: list[str] = []
    open_price = _number(record.get("open"))
    high = _number(record.get("high"))
    low = _number(record.get("low"))
    close = _number(record.get("close"))
    volume = _number(record.get("volume"))
    amount = _number(record.get("amount"))
    if min(open_price, high, low, close) <= 0:
        issues.append("non_positive_price")
    if high < max(open_price, low, close):
        issues.append("high_below_price")
    if low > min(open_price, high, close):
        issues.append("low_above_price")
    if volume < 0:
        issues.append("negative_volume")
    if amount < 0:
        issues.append("negative_amount")
    return issues


def summarize_ohlcv_issues(records: list[dict[str, Any]]) -> dict[str, int]:
    summary: dict[str, int] = {}
    for record in records:
        for issue in validate_ohlcv_record(record):
            summary[issue] = summary.get(issue, 0) + 1
    return summary


def _number(value: Any) -> float:
    if value is None:
        return 0.0
    return float(value)
