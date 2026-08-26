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
    algorithm_version: str = "v4",
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
        last_turnover_amount = _sample_turnover(last)
        # A capture can be delayed, but a long gap is not evidence that the
        # stock stayed sealed between two observations.
        sealed_duration_seconds = 0.0
        gap_count = 0
        observed_duration_seconds = 0.0
        max_observed_gap_seconds = 0.0
        max_trusted_gap_seconds = max(15, sample_interval_seconds * 3)

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
            sample_turnover = _sample_turnover(sample)
            if sample_turnover is not None:
                last_turnover_amount = sample_turnover
            if previous_sealed and not is_sealed:
                open_count += 1
            if ever_sealed and not previous_sealed and is_sealed:
                reseal_count += 1
            ever_on_board = ever_on_board or is_on_board
            ever_sealed = ever_sealed or is_sealed
            previous_sealed = is_sealed

            if index + 1 < len(samples):
                delta = (samples[index + 1]["captured_at"] - captured_at).total_seconds()
                max_observed_gap_seconds = max(max_observed_gap_seconds, delta)
                if delta > sample_interval_seconds * 1.5:
                    gap_count += max(1, int(round(delta / sample_interval_seconds)) - 1)
                # Each sample confirms one nominal observation window. Using
                # the wall-clock delta here would count provider delays as
                # sealed time and overstate duration.
                if delta > 0 and delta <= max_trusted_gap_seconds:
                    observed_duration_seconds += sample_interval_seconds
                if is_sealed and delta > 0 and delta <= max_trusted_gap_seconds:
                    sealed_duration_seconds += sample_interval_seconds
            elif is_sealed:
                sealed_duration_seconds += sample_interval_seconds

        if not ever_on_board:
            continue

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

        trend_offsets, trend_amounts = _compact_trend_series(sealed_amounts)

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
            "sealed_duration_seconds": int(round(sealed_duration_seconds)),
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
            "last_turnover_amount": last_turnover_amount,
            "last_sealed_turnover_ratio": _turnover_ratio(last_amount, last_turnover_amount),
            "max_sealed_turnover_ratio": _turnover_ratio(peak_amount, last_turnover_amount),
            "sealed_amount_change": amount_change,
            "sealed_amount_slope": amount_slope,
            "trend_direction": trend_direction,
            "trend_series_origin_at": sealed_amounts[0][0] if sealed_amounts else None,
            "trend_time_offsets": trend_offsets,
            "trend_sealed_amounts": trend_amounts,
            "source": source,
            "algorithm_version": algorithm_version,
            "trend_features": {
                "sample_interval_seconds": sample_interval_seconds,
                "expected_samples": expected_samples,
                "observed_interval_count": max(0, len(samples) - 1),
                "observed_duration_seconds": int(round(observed_duration_seconds)),
                "max_observed_gap_seconds": int(round(max_observed_gap_seconds)),
                "duration_is_partial": gap_count > 0,
                "quality_status": "partial" if gap_count > 0 else "observed",
                "duration_basis": "nominal_sample_windows",
                "trend_series_format": "offset_seconds_amount_yuan_v1",
                "trend_series_point_count": len(trend_offsets),
            },
        })
    return summaries


def _compact_trend_series(points: list[tuple[datetime, float]], maximum: int = 120) -> tuple[list[int], list[int]]:
    """Encode chart data as paired compact PG arrays rather than JSON points.

    The offset fits in a smallint for a single A-share session and each amount
    is rounded to yuan for display; that keeps one 120-point trend around a
    kilobyte while retaining the shape required by the UI chart.
    """
    if not points:
        return [], []
    if maximum <= 1:
        selected = [points[-1]]
    elif len(points) <= maximum:
        selected = list(points)
    else:
        stride = max(1, ceil(len(points) / maximum))
        selected = list(points[::stride])
        if selected[-1] != points[-1]:
            selected.append(points[-1])
        if len(selected) > maximum:
            selected = selected[: maximum - 1] + [points[-1]]
    origin = points[0][0]
    return (
        [max(0, int(round((timestamp - origin).total_seconds()))) for timestamp, _ in selected],
        [int(round(amount)) for _, amount in selected],
    )


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


def _sample_turnover(sample: dict[str, Any] | None) -> float | None:
    if not sample:
        return None
    turnover = _number_or_none(sample.get("turnover_amount"))
    return turnover if turnover is not None else _number_or_none(sample.get("amount"))


def _turnover_ratio(sealed: float | None, turnover: float | None) -> float | None:
    if sealed is None or turnover is None or turnover <= 0:
        return None
    return sealed / turnover


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
