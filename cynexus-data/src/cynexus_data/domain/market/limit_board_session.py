from __future__ import annotations

from datetime import date, datetime, time
from math import ceil
from statistics import mean, median
from typing import Any, Iterable
from zoneinfo import ZoneInfo


SHANGHAI = ZoneInfo("Asia/Shanghai")
SESSION_WINDOWS = {
    "morning": (time(9, 30), time(11, 30)),
    "afternoon": (time(13, 0), time(15, 0)),
}


def session_bounds(trade_date: date, trade_session: str) -> tuple[datetime, datetime]:
    try:
        start, end = SESSION_WINDOWS[trade_session]
    except KeyError as exc:
        raise ValueError(f"unsupported trade session: {trade_session}") from exc
    return (
        datetime.combine(trade_date, start, tzinfo=SHANGHAI),
        datetime.combine(trade_date, end, tzinfo=SHANGHAI),
    )


def current_trade_session(now: datetime | None = None) -> str | None:
    local = (now or datetime.now(SHANGHAI)).astimezone(SHANGHAI)
    current = local.timetz().replace(tzinfo=None)
    for trade_session, (start, end) in SESSION_WINDOWS.items():
        if start <= current < end:
            return trade_session
    return None


def closed_trade_session(now: datetime | None = None) -> str | None:
    local = (now or datetime.now(SHANGHAI)).astimezone(SHANGHAI)
    current = local.timetz().replace(tzinfo=None)
    if current >= SESSION_WINDOWS["afternoon"][1]:
        return "afternoon"
    if current >= SESSION_WINDOWS["morning"][1] and current < SESSION_WINDOWS["afternoon"][0]:
        return "morning"
    return None


def aggregate_limit_board_session(
    rows: Iterable[dict[str, Any]],
    *,
    trade_date: date,
    trade_session: str,
    sample_interval_seconds: int = 5,
    source: str = "eltdx.quotes",
    algorithm_version: str = "v1",
) -> list[dict[str, Any]]:
    start, end = session_bounds(trade_date, trade_session)
    grouped: dict[str, list[dict[str, Any]]] = {}
    for row in rows:
        code = str(row.get("code") or "").strip()[-6:].zfill(6)
        captured_at = row.get("captured_at")
        if code and isinstance(captured_at, datetime):
            grouped.setdefault(code, []).append(row)

    expected_samples = max(1, int((end - start).total_seconds() // sample_interval_seconds))
    summaries: list[dict[str, Any]] = []
    for code, samples in grouped.items():
        samples.sort(key=lambda row: row["captured_at"])
        direction = next((str(row.get("direction")) for row in samples if row.get("direction") in {"up", "down"}), None)
        if direction is None:
            continue
        first = samples[0]
        last = samples[-1]
        first_touch_at: datetime | None = None
        first_sealed_at: datetime | None = None
        last_sealed_at: datetime | None = None
        peak_amount_at: datetime | None = None
        previous_sealed = False
        ever_on_board = False
        ever_sealed = False
        open_count = 0
        reseal_count = 0
        sealed_amounts: list[tuple[datetime, float]] = []
        sealed_duration_seconds = 0
        gap_count = 0

        for index, sample in enumerate(samples):
            captured_at = sample["captured_at"]
            is_on_board = bool(sample.get("is_on_board")) or str(sample.get("board_status")) in {"touch", "sealed"}
            is_sealed = bool(sample.get("is_sealed")) or str(sample.get("board_status")) == "sealed"
            if is_on_board and first_touch_at is None:
                first_touch_at = captured_at
            if is_sealed and first_sealed_at is None:
                first_sealed_at = captured_at
            if is_sealed:
                last_sealed_at = captured_at
                amount = _number_or_none(sample.get("sealed_amount"))
                if amount is not None:
                    sealed_amounts.append((captured_at, amount))
            if previous_sealed and not is_sealed:
                open_count += 1
            if ever_sealed and not previous_sealed and is_sealed:
                reseal_count += 1
            ever_on_board = ever_on_board or is_on_board
            ever_sealed = ever_sealed or is_sealed
            previous_sealed = is_sealed

            if index + 1 < len(samples):
                delta = (samples[index + 1]["captured_at"] - captured_at).total_seconds()
                if delta > sample_interval_seconds * 1.5:
                    gap_count += max(1, int(round(delta / sample_interval_seconds)) - 1)
                if is_sealed and 0 < delta <= sample_interval_seconds * 1.5:
                    sealed_duration_seconds += int(min(delta, sample_interval_seconds))
            elif is_sealed:
                sealed_duration_seconds += sample_interval_seconds

        if sealed_amounts:
            peak_time, peak_amount = max(sealed_amounts, key=lambda item: item[1])
            peak_amount_at = peak_time
            first_amount = sealed_amounts[0][1]
            last_amount = sealed_amounts[-1][1]
            amount_change = last_amount - first_amount
            amount_slope = _slope(sealed_amounts)
            trend_direction = _trend_direction(first_amount, last_amount)
        else:
            peak_amount = None
            first_amount = None
            last_amount = None
            amount_change = None
            amount_slope = None
            trend_direction = "insufficient"

        closing_status = "sealed" if bool(last.get("is_sealed")) else "touch" if bool(last.get("is_on_board")) else "opened" if ever_on_board else "unknown"
        valid_sample_count = sum(1 for sample in samples if sample.get("captured_at") and sample.get("direction"))
        summaries.append({
            "trade_date": trade_date,
            "trade_session": trade_session,
            "code": code,
            "name": first.get("name") or last.get("name"),
            "market": first.get("market") or last.get("market"),
            "direction": direction,
            "closing_status": closing_status,
            "is_sealed_at_close": bool(last.get("is_sealed")),
            "first_touch_at": first_touch_at,
            "first_sealed_at": first_sealed_at,
            "last_sealed_at": last_sealed_at,
            "last_observed_at": last["captured_at"],
            "sealed_duration_seconds": sealed_duration_seconds,
            "open_count": open_count,
            "reseal_count": reseal_count,
            "sample_count": len(samples),
            "valid_sample_count": valid_sample_count,
            "gap_count": gap_count,
            "coverage_ratio": min(1.0, valid_sample_count / expected_samples),
            "max_sealed_amount": peak_amount,
            "avg_sealed_amount": mean(amount for _, amount in sealed_amounts) if sealed_amounts else None,
            "median_sealed_amount": median(amount for _, amount in sealed_amounts) if sealed_amounts else None,
            "peak_sealed_amount_at": peak_amount_at,
            "first_sealed_amount": first_amount,
            "last_sealed_amount": last_amount,
            "sealed_amount_change": amount_change,
            "sealed_amount_slope": amount_slope,
            "trend_direction": trend_direction,
            "source": source,
            "algorithm_version": algorithm_version,
            "trend_features": {
                "sample_interval_seconds": sample_interval_seconds,
                "expected_samples": expected_samples,
                "sealed_amount_points": _trend_points(sealed_amounts),
            },
        })
    return summaries


def _trend_points(points: list[tuple[datetime, float]], maximum: int = 120) -> list[list[float]]:
    if not points:
        return []
    stride = max(1, ceil(len(points) / maximum))
    origin = points[0][0]
    selected = points[::stride]
    if selected[-1] != points[-1]:
        selected.append(points[-1])
    return [[round((timestamp - origin).total_seconds(), 3), round(amount, 6)] for timestamp, amount in selected]


def _slope(points: list[tuple[datetime, float]]) -> float | None:
    if len(points) < 2:
        return None
    origin = points[0][0]
    xs = [(timestamp - origin).total_seconds() for timestamp, _ in points]
    ys = [amount for _, amount in points]
    x_mean = mean(xs)
    y_mean = mean(ys)
    denominator = sum((x - x_mean) ** 2 for x in xs)
    return None if denominator == 0 else sum((x - x_mean) * (y - y_mean) for x, y in zip(xs, ys)) / denominator


def _trend_direction(first: float, last: float) -> str:
    baseline = max(abs(first), 1.0)
    change_ratio = (last - first) / baseline
    if abs(change_ratio) < 0.01:
        return "flat"
    return "increasing" if change_ratio > 0 else "decreasing"


def _number_or_none(value: Any) -> float | None:
    if value in (None, ""):
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None
