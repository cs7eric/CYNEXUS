from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timedelta, timezone
try:
    from enum import StrEnum
except ImportError:  # Python 3.10 compatibility
    from enum import Enum

    class StrEnum(str, Enum):
        """Backport of :class:`enum.StrEnum` for the project's Python floor."""

        def __str__(self) -> str:
            return self.value
from math import ceil, isfinite
from statistics import median
from typing import Any, Iterable, Mapping


class Direction(StrEnum):
    RISE = "rise"
    FALL = "fall"


class QualityGrade(StrEnum):
    A = "A"
    B = "B"
    C = "C"
    D = "D"


class ConfidenceLevel(StrEnum):
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INSUFFICIENT = "insufficient"


class EpisodeStage(StrEnum):
    EMERGING = "emerging"
    CONFIRMED = "confirmed"
    SPREADING = "spreading"
    CROWDED = "crowded"
    DECAYING = "decaying"
    FAILED = "failed"
    REVERSED = "reversed"
    ENDED = "ended"


class AdviceAction(StrEnum):
    OBSERVE = "observe"
    WAIT_CONFIRMATION = "wait_confirmation"
    PROBE_REFERENCE = "probe_reference"
    PULLBACK_CONFIRMATION_REFERENCE = "pullback_confirmation_reference"
    HOLD_OBSERVE = "hold_observe"
    DO_NOT_CHASE = "do_not_chase"
    REDUCE_AVOID_REFERENCE = "reduce_avoid_reference"
    INVALIDATION_EXIT_REFERENCE = "invalidation_exit_reference"


@dataclass(frozen=True)
class DataQuality:
    grade: QualityGrade = QualityGrade.D
    coverage_ratio: float = 0.0
    fresh_ratio: float = 0.0
    stale_ratio: float = 1.0
    missing_field_ratio: float = 1.0
    mapped_industry_ratio: float = 0.0
    provider_error_count: int = 0
    provider_divergence_pct: float | None = None
    max_data_age_ms: int | None = None
    warnings: tuple[str, ...] = ()

    @classmethod
    def from_counts(
        cls,
        *,
        eligible_count: int,
        observed_count: int,
        fresh_count: int,
        mapped_count: int,
        missing_count: int = 0,
        provider_error_count: int = 0,
        max_data_age_ms: int | None = None,
        warnings: Iterable[str] = (),
    ) -> "DataQuality":
        eligible = max(0, int(eligible_count))
        observed = max(0, int(observed_count))
        denominator = max(1, eligible)
        coverage = min(1.0, observed / denominator)
        fresh = min(1.0, max(0, int(fresh_count)) / max(1, observed)) if observed else 0.0
        mapped = min(1.0, max(0, int(mapped_count)) / max(1, observed)) if observed else 0.0
        missing = min(1.0, max(0, int(missing_count)) / max(1, observed)) if observed else 1.0
        stale = max(0.0, 1.0 - fresh)
        if coverage >= 0.985 and fresh >= 0.97 and mapped >= 0.97 and missing <= 0.03:
            grade = QualityGrade.A
        elif coverage >= 0.985 and fresh >= 0.97 and missing <= 0.03:
            # A complete, fresh capture remains usable when the constituent
            # mapping is temporarily unavailable.  The missing mapping is
            # surfaced through mapped_industry_ratio/warnings and prevents
            # the stronger A grade.
            grade = QualityGrade.B
        elif coverage >= 0.95 and fresh >= 0.92 and mapped >= 0.90:
            grade = QualityGrade.B
        elif coverage >= 0.80:
            grade = QualityGrade.C
        else:
            grade = QualityGrade.D
        return cls(
            grade=grade,
            coverage_ratio=round(coverage, 6),
            fresh_ratio=round(fresh, 6),
            stale_ratio=round(stale, 6),
            missing_field_ratio=round(missing, 6),
            mapped_industry_ratio=round(mapped, 6),
            provider_error_count=max(0, int(provider_error_count)),
            max_data_age_ms=max_data_age_ms,
            warnings=tuple(str(item) for item in warnings if str(item).strip()),
        )


@dataclass(frozen=True)
class ConfidenceEvidence:
    score: float = 0.0
    level: ConfidenceLevel = ConfidenceLevel.INSUFFICIENT
    timing_lead: float = 0.0
    relative_strength: float = 0.0
    activity_confirmation: float = 0.0
    follower_response: float = 0.0
    ex_leader_persistence: float = 0.0
    market_compatibility: float = 0.0
    quality_penalty: float = 0.0
    reasons: tuple[str, ...] = ()
    counter_evidence: tuple[str, ...] = ()


@dataclass(frozen=True)
class MarketContextSnapshot:
    context_id: str
    trade_date: str
    bucket_at: datetime
    as_of: datetime
    trading_phase: str = "unknown"
    regime: str = "unknown"
    regime_strength: float = 0.0
    indices: dict[str, Any] = field(default_factory=dict)
    breadth: dict[str, Any] = field(default_factory=dict)
    liquidity: dict[str, Any] = field(default_factory=dict)
    sentiment: dict[str, Any] = field(default_factory=dict)
    style: dict[str, Any] = field(default_factory=dict)
    active_episode_count: int = 0
    data_quality: DataQuality = field(default_factory=DataQuality)
    provenance: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class StockSignalEvent:
    signal_event_id: str
    trade_date: str
    code: str
    name: str | None
    market: str | None
    industry_code: str | None
    industry_name: str | None
    membership_snapshot_date: str | None
    event_at: datetime
    detected_at: datetime
    published_at: datetime
    direction: Direction
    signal_kind: str
    window_seconds: int
    start_price: float | None
    last_price: float | None
    return_pct: float | None
    sector_return_pct: float | None
    sector_excess_pct: float | None
    market_return_pct: float | None
    market_excess_pct: float | None
    amount_velocity: float | None
    amount_acceleration: float | None
    volume_acceleration: float | None
    turnover_acceleration: float | None
    limit_transition: str | None
    cohorts: tuple[str, ...]
    threshold_snapshot: dict[str, Any]
    market_context_id: str | None
    confidence: ConfidenceEvidence
    data_quality: DataQuality
    status: str = "open"
    provenance: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class EpisodeMemberResponse:
    episode_id: str
    code: str
    name: str | None
    role: str
    first_signal_event_id: str | None
    first_response_at: datetime | None
    lag_seconds: int | None
    return_at_response_pct: float | None
    sector_excess_at_response_pct: float | None
    peak_return_pct: float | None
    close_return_pct: float | None
    contribution_share: float | None
    activity_acceleration: float | None
    response_evidence: dict[str, Any]
    data_quality: DataQuality


@dataclass(frozen=True)
class SectorEpisode:
    episode_id: str
    trade_date: str
    sector_type: str
    sector_code: str | None
    sector_name: str
    direction: Direction
    stage: EpisodeStage
    started_at: datetime
    confirmed_at: datetime | None
    last_evidence_at: datetime
    ended_at: datetime | None
    market_context_at_start_id: str | None
    market_context_at_confirm_id: str | None
    eligible_count: int
    observed_count: int
    responded_count: int
    coverage_ratio: float
    coverage_peak: float
    median_lag_seconds: int | None
    p75_lag_seconds: int | None
    sector_equal_return_pct: float | None
    sector_median_return_pct: float | None
    ex_leader_return_pct: float | None
    weighted_return_pct: float | None
    benchmark_excess_pct: float | None
    breadth_delta_pct_point: float | None
    amount_acceleration: float | None
    leader_candidates: tuple[dict[str, Any], ...]
    suppressor_candidates: tuple[dict[str, Any], ...]
    confidence: ConfidenceEvidence
    data_quality: DataQuality
    failure_reasons: tuple[str, ...] = ()
    version: int = 1
    members: tuple[EpisodeMemberResponse, ...] = ()
    provenance: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class EpisodeOutcome:
    episode_id: str
    anchor_type: str
    evaluated_at: datetime
    forward_sector_return_5m_pct: float | None
    forward_sector_return_15m_pct: float | None
    forward_sector_return_30m_pct: float | None
    forward_sector_return_close_pct: float | None
    forward_excess_5m_pct: float | None
    forward_excess_15m_pct: float | None
    forward_excess_30m_pct: float | None
    forward_excess_close_pct: float | None
    leader_forward_return_pct: float | None
    ex_leader_forward_return_pct: float | None
    max_favorable_excursion_pct: float | None
    max_adverse_excursion_pct: float | None
    coverage_peak: float | None
    persistence_trading_seconds: int | None
    close_retention_ratio: float | None
    outcome: str
    failure_reasons: tuple[str, ...]
    quality: DataQuality
    algorithm_version: str


@dataclass(frozen=True)
class TradeReferenceAdvice:
    advice_id: str
    episode_id: str
    target_type: str
    target_code: str | None
    target_name: str
    position_state: str
    action_class: AdviceAction
    generated_at: datetime
    as_of: datetime
    valid_until: datetime
    horizon: str
    confidence: float
    entry_conditions: tuple[str, ...]
    invalidation_conditions: tuple[str, ...]
    risk_reference: dict[str, Any]
    reward_reference: dict[str, Any]
    market_compatibility: dict[str, Any]
    evidence: tuple[str, ...]
    counter_evidence: tuple[str, ...]
    disclaimer_code: str
    policy_version: str
    data_quality: DataQuality


@dataclass(frozen=True)
class TransmissionConfig:
    algorithm_version: str = "transmission-v1"
    threshold_set_id: str = "default-v1"
    signal_window_seconds: int = 60
    max_gap_seconds: int = 300
    rapid_return_pct: float = 0.8
    follower_return_pct: float = 0.3
    minimum_confirmation_coverage: float = 0.15
    minimum_confirmation_breadth_pct_point: float = 10.0
    confirmation_window_seconds: int = 180
    merge_gap_seconds: int = 120
    cooldown_seconds: int = 60
    crowded_coverage: float = 0.55
    decay_coverage_drop: float = 0.30
    eligible_count_by_sector: Mapping[str, int] | None = None
    full_confirmation_required: bool = False


def build_transmission_snapshot(
    rows: list[dict[str, Any]],
    previous_by_code: dict[str, dict[str, Any]],
    *,
    market_context: MarketContextSnapshot | None = None,
    config: TransmissionConfig | None = None,
    detected_at: datetime | None = None,
) -> dict[str, Any]:
    """Build deterministic signal/Episode/advice facts from one quote capture.

    This is intentionally pure: provider calls, persistence and scheduling stay
    in pipelines/storage. The first implementation can run on the existing
    capture pool; full-market confirmation is enabled once the collector passes
    its coverage SLO.
    """
    cfg = config or TransmissionConfig()
    if not rows:
        return {"signal_events": [], "episodes": [], "advice": [], "quality": DataQuality()}
    now = detected_at or _datetime_from_rows(rows) or datetime.now(timezone.utc)
    context = market_context or _default_context(now, rows)
    quality = _rows_quality(
        rows,
        eligible_count_override=(
            sum((cfg.eligible_count_by_sector or {}).values())
            if cfg.full_confirmation_required and cfg.eligible_count_by_sector
            else None
        ),
    )
    returns_by_code: dict[str, float] = {}
    previous_price_by_code: dict[str, float] = {}
    gaps_by_code: dict[str, int] = {}
    for row in rows:
        code = _plain_code(row.get("code"))
        previous = previous_by_code.get(code) or previous_by_code.get(str(row.get("code") or "")) or {}
        current_price = _number_or_none(row.get("last_price"))
        previous_price = _number_or_none(previous.get("last_price"))
        current_at = _as_datetime(row.get("captured_at"))
        previous_at = _as_datetime(previous.get("captured_at"))
        if current_price is None or previous_price is None or previous_price <= 0:
            continue
        gap = int(round((current_at - previous_at).total_seconds())) if current_at and previous_at else cfg.signal_window_seconds
        if gap < 0 or gap > cfg.max_gap_seconds:
            continue
        value = (current_price / previous_price - 1.0) * 100.0
        returns_by_code[code] = value
        previous_price_by_code[code] = previous_price
        gaps_by_code[code] = max(1, gap)

    sector_groups: dict[tuple[str, str, Direction], list[dict[str, Any]]] = {}
    for row in rows:
        code = _plain_code(row.get("code"))
        value = returns_by_code.get(code)
        if value is None or abs(value) < cfg.rapid_return_pct:
            continue
        direction = Direction.RISE if value > 0 else Direction.FALL
        sector_code, sector_name = _sector(row)
        sector_groups.setdefault((sector_code, sector_name, direction), []).append(row)

    events: list[StockSignalEvent] = []
    for (sector_code, sector_name, direction), group in sector_groups.items():
        group_returns = [returns_by_code.get(_plain_code(row.get("code"))) for row in group]
        group_returns = [value for value in group_returns if value is not None]
        sector_median = median(group_returns) if group_returns else None
        for row in group:
            code = _plain_code(row.get("code"))
            value = returns_by_code.get(code)
            if value is None:
                continue
            event_at = _as_datetime(row.get("captured_at")) or now
            sector_excess = value - sector_median if sector_median is not None else None
            evidence = _signal_confidence(value, sector_excess, row, quality, context)
            event_key = f"{context.trade_date}:{code}:{direction.value}:{event_at.isoformat()}:{cfg.algorithm_version}"
            events.append(StockSignalEvent(
                signal_event_id=_stable_id("signal", event_key),
                trade_date=context.trade_date,
                code=code,
                name=str(row.get("name") or "") or None,
                market=str(row.get("market") or "") or None,
                industry_code=sector_code,
                industry_name=sector_name,
                membership_snapshot_date=str(row.get("membership_snapshot_date") or "") or None,
                event_at=event_at,
                detected_at=now,
                published_at=now,
                direction=direction,
                signal_kind="composite",
                window_seconds=gaps_by_code.get(code, cfg.signal_window_seconds),
                start_price=previous_price_by_code.get(code),
                last_price=_number_or_none(row.get("last_price")),
                return_pct=_round(value),
                sector_return_pct=_round(sector_median),
                sector_excess_pct=_round(sector_excess),
                market_return_pct=_number_or_none(context.indices.get("benchmarkReturnPct")),
                market_excess_pct=_round(value - _number_or_none(context.indices.get("benchmarkReturnPct"), 0.0)),
                amount_velocity=_amount_velocity(row, previous_by_code.get(code), gaps_by_code.get(code, cfg.signal_window_seconds)),
                amount_acceleration=None,
                volume_acceleration=None,
                turnover_acceleration=None,
                limit_transition=_limit_transition(previous_by_code.get(code), row),
                cohorts=_cohorts(row),
                threshold_snapshot={
                    "rapidReturnPct": cfg.rapid_return_pct,
                    "windowSeconds": cfg.signal_window_seconds,
                    "maxGapSeconds": cfg.max_gap_seconds,
                    "thresholdSetId": cfg.threshold_set_id,
                },
                market_context_id=context.context_id,
                confidence=evidence,
                data_quality=quality,
                provenance={"algorithmVersion": cfg.algorithm_version, "captureAt": event_at.isoformat()},
            ))

    episodes = _build_episodes(rows, events, returns_by_code, context, quality, cfg, now)
    advice = [_build_advice(episode, context, cfg, now) for episode in episodes]
    return {
        "schema_version": 2,
        "algorithm_version": cfg.algorithm_version,
        "threshold_set_id": cfg.threshold_set_id,
        "context": context,
        "quality": quality,
        "confirmation_scope": {
            "full_confirmation_required": cfg.full_confirmation_required,
            "eligible_sector_count": len(cfg.eligible_count_by_sector or {}),
        },
        "signal_events": events,
        "episodes": episodes,
        "advice": advice,
    }


def evaluate_episode_outcome(
    episode: SectorEpisode | dict[str, Any],
    captures: Iterable[list[dict[str, Any]]],
    *,
    evaluated_at: datetime | None = None,
    benchmark_by_capture: dict[datetime, float] | None = None,
    algorithm_version: str = "transmission-outcome-v1",
) -> EpisodeOutcome:
    """Evaluate forward behaviour using only captures after an episode anchor.

    This function is deliberately post-market/replay-only.  It never changes
    the episode or advice and it uses the episode start as the anchor, making
    the boundary between information available at signal time and subsequent
    outcome facts explicit.
    """
    episode_dict = to_jsonable(episode) if not isinstance(episode, dict) else episode
    episode_id = str(episode_dict.get("episode_id") or "")
    started_at = _as_datetime(episode_dict.get("started_at")) or evaluated_at or datetime.now(timezone.utc)
    direction = str(episode_dict.get("direction") or Direction.RISE.value)
    sign = 1.0 if direction == Direction.RISE.value else -1.0
    sector_code = episode_dict.get("sector_code")
    sector_name = str(episode_dict.get("sector_name") or "未分类")
    leader_candidates = episode_dict.get("leader_candidates") or []
    leader_code = str((leader_candidates[0] or {}).get("code") or "") if leader_candidates else ""

    ordered_captures = sorted(
        [list(rows) for rows in captures if rows],
        key=_capture_time_safe,
    )
    anchor_rows = next((rows for rows in ordered_captures if _capture_time_safe(rows) >= started_at), [])
    anchor_prices = {
        _plain_code(row.get("code")): _number_or_none(row.get("last_price"))
        for row in anchor_rows
        if _number_or_none(row.get("last_price")) is not None
    }
    if not anchor_prices:
        return EpisodeOutcome(
            episode_id=episode_id,
            anchor_type="episode_start",
            evaluated_at=evaluated_at or started_at,
            forward_sector_return_5m_pct=None,
            forward_sector_return_15m_pct=None,
            forward_sector_return_30m_pct=None,
            forward_sector_return_close_pct=None,
            forward_excess_5m_pct=None,
            forward_excess_15m_pct=None,
            forward_excess_30m_pct=None,
            forward_excess_close_pct=None,
            leader_forward_return_pct=None,
            ex_leader_forward_return_pct=None,
            max_favorable_excursion_pct=None,
            max_adverse_excursion_pct=None,
            coverage_peak=0.0,
            persistence_trading_seconds=None,
            close_retention_ratio=None,
            outcome="insufficient_data",
            failure_reasons=("缺少 Episode 启动时的有效价格",),
            quality=DataQuality(),
            algorithm_version=algorithm_version,
        )

    points: list[tuple[datetime, float | None, float | None, float | None, float]] = []
    for rows in ordered_captures:
        captured_at = _capture_time_safe(rows)
        if captured_at < started_at:
            continue
        sector_values: list[float] = []
        ex_values: list[float] = []
        leader_return: float | None = None
        for row in rows:
            code = _plain_code(row.get("code"))
            anchor = anchor_prices.get(code)
            last = _number_or_none(row.get("last_price"))
            if anchor is None or last is None or anchor <= 0:
                continue
            value = (last / anchor - 1.0) * 100.0
            row_sector_code, row_sector_name = _sector(row)
            if (sector_code and row_sector_code == sector_code) or (not sector_code and row_sector_name == sector_name):
                sector_values.append(value)
                if code == leader_code:
                    leader_return = value
                else:
                    ex_values.append(value)
        if not sector_values:
            continue
        benchmark = None
        if benchmark_by_capture:
            benchmark = benchmark_by_capture.get(captured_at)
        points.append((captured_at, median(sector_values), leader_return, median(ex_values) if ex_values else None, float(len(sector_values))))

    def at_or_after(seconds: int) -> tuple[float | None, float | None]:
        target = started_at + timedelta(seconds=seconds)
        point = next((item for item in points if item[0] >= target), None)
        if point is None:
            return None, None
        return point[1], point[1] - (benchmark_by_capture.get(point[0], 0.0) if benchmark_by_capture else 0.0)

    sector_5m, excess_5m = at_or_after(300)
    sector_15m, excess_15m = at_or_after(900)
    sector_30m, excess_30m = at_or_after(1800)
    close_sector = points[-1][1] if points else None
    close_excess = points[-1][1] - (benchmark_by_capture.get(points[-1][0], 0.0) if benchmark_by_capture else 0.0) if points else None
    favorable = [sign * (point[1] or 0.0) for point in points]
    adverse = [sign * (point[1] or 0.0) for point in points]
    max_favorable = max(favorable) if favorable else None
    max_adverse = min(adverse) if adverse else None
    leader_forward = points[-1][2] if points else None
    ex_leader_forward = points[-1][3] if points else None
    coverage_peak = max((point[4] / max(1, int(episode_dict.get("eligible_count") or 0)) for point in points), default=0.0)
    persistence = int((points[-1][0] - started_at).total_seconds()) if points else None
    close_retention = None
    if max_favorable is not None and max_favorable > 0 and close_sector is not None:
        close_retention = _clip01(sign * close_sector / max_favorable)
    outcome, failure_reasons = _classify_outcome(sign, sector_5m, sector_15m, sector_30m, close_sector, coverage_peak)
    quality = DataQuality.from_counts(
        eligible_count=int(episode_dict.get("eligible_count") or 0),
        observed_count=int(episode_dict.get("observed_count") or 0),
        fresh_count=int(episode_dict.get("observed_count") or 0),
        mapped_count=int(episode_dict.get("observed_count") or 0),
        warnings=() if points else ("未来窗口数据不足",),
    )
    return EpisodeOutcome(
        episode_id=episode_id,
        anchor_type="episode_start",
        evaluated_at=evaluated_at or (_capture_time_safe(ordered_captures[-1]) if ordered_captures else started_at),
        forward_sector_return_5m_pct=_round(sector_5m),
        forward_sector_return_15m_pct=_round(sector_15m),
        forward_sector_return_30m_pct=_round(sector_30m),
        forward_sector_return_close_pct=_round(close_sector),
        forward_excess_5m_pct=_round(excess_5m),
        forward_excess_15m_pct=_round(excess_15m),
        forward_excess_30m_pct=_round(excess_30m),
        forward_excess_close_pct=_round(close_excess),
        leader_forward_return_pct=_round(leader_forward),
        ex_leader_forward_return_pct=_round(ex_leader_forward),
        max_favorable_excursion_pct=_round(max_favorable),
        max_adverse_excursion_pct=_round(max_adverse),
        coverage_peak=_round(coverage_peak),
        persistence_trading_seconds=persistence,
        close_retention_ratio=_round(close_retention),
        outcome=outcome,
        failure_reasons=tuple(failure_reasons),
        quality=quality,
        algorithm_version=algorithm_version,
    )


def _classify_outcome(
    sign: float,
    sector_5m: float | None,
    sector_15m: float | None,
    sector_30m: float | None,
    close_sector: float | None,
    coverage_peak: float,
) -> tuple[str, tuple[str, ...]]:
    if sector_5m is None and sector_15m is None:
        return "insufficient_data", ("未来窗口数据不足",)
    directional = lambda value: value is not None and sign * value > 0.15
    opposite = lambda value: value is not None and sign * value < -0.15
    if close_sector is not None and opposite(close_sector):
        return "reversed", ("收盘方向与 Episode 相反",)
    if directional(sector_30m) and directional(close_sector):
        return "sustained", ()
    if directional(sector_15m) or directional(sector_30m):
        return "late_spread", ("早期窗口确认不足",)
    if coverage_peak < 0.15:
        return "failed", ("板块覆盖率未达到确认门槛",)
    return "failed", ("后续板块收益未延续",)


def _build_episodes(
    rows: list[dict[str, Any]],
    events: list[StockSignalEvent],
    returns_by_code: dict[str, float],
    context: MarketContextSnapshot,
    quality: DataQuality,
    cfg: TransmissionConfig,
    now: datetime,
) -> list[SectorEpisode]:
    groups: dict[tuple[str | None, str, Direction], list[StockSignalEvent]] = {}
    for event in events:
        groups.setdefault((event.industry_code, event.industry_name or "未分类", event.direction), []).append(event)
    result: list[SectorEpisode] = []
    for (sector_code, sector_name, direction), group in groups.items():
        ordered = sorted(group, key=lambda event: (event.event_at, event.code))
        started_at = ordered[0].event_at
        eligible_rows = [row for row in rows if _sector(row) == (sector_code, sector_name)]
        sector_key = sector_code or sector_name
        eligible_count = (
            int((cfg.eligible_count_by_sector or {}).get(sector_key, len(eligible_rows)))
            if cfg.full_confirmation_required
            else len(eligible_rows)
        )
        observed = [row for row in eligible_rows if _plain_code(row.get("code")) in returns_by_code]
        sign = 1.0 if direction is Direction.RISE else -1.0
        threshold = cfg.follower_return_pct
        members: list[EpisodeMemberResponse] = []
        for row in observed:
            code = _plain_code(row.get("code"))
            value = returns_by_code.get(code)
            if value is None or sign * value < threshold:
                continue
            first_event = next((event for event in ordered if event.code == code), None)
            first_at = first_event.event_at if first_event else _as_datetime(row.get("captured_at"))
            lag = max(0, int(round((first_at - started_at).total_seconds()))) if first_at else None
            role = "first_wave" if lag is not None and lag <= 60 else "second_wave"
            members.append(EpisodeMemberResponse(
                episode_id="pending",
                code=code,
                name=str(row.get("name") or "") or None,
                role=role,
                first_signal_event_id=first_event.signal_event_id if first_event else None,
                first_response_at=first_at,
                lag_seconds=lag,
                return_at_response_pct=_round(value),
                sector_excess_at_response_pct=first_event.sector_excess_pct if first_event else None,
                peak_return_pct=_round(value),
                close_return_pct=_number_or_none(row.get("change_pct")),
                contribution_share=None,
                activity_acceleration=None,
                response_evidence={"followerReturnThresholdPct": threshold},
                data_quality=quality,
            ))
        responded_count = len(members)
        coverage = responded_count / max(1, eligible_count)
        lags = [item.lag_seconds for item in members if item.lag_seconds is not None]
        group_returns = [returns_by_code.get(_plain_code(row.get("code"))) for row in observed]
        group_returns = [value for value in group_returns if value is not None]
        median_return = median(group_returns) if group_returns else None
        equal_return = sum(group_returns) / len(group_returns) if group_returns else None
        leader = ordered[0]
        leader_value = returns_by_code.get(leader.code)
        ex_values = [value for code, value in returns_by_code.items() if code != leader.code and code in {_plain_code(row.get("code")) for row in observed}]
        ex_leader = sum(ex_values) / len(ex_values) if ex_values else None
        breadth_delta = (sum(1 for value in group_returns if sign * value > 0) / max(1, len(group_returns)) - 0.5) * 100
        confirmed = (
            responded_count >= max(3, ceil(eligible_count * cfg.minimum_confirmation_coverage))
            and coverage >= cfg.minimum_confirmation_coverage
            and (ex_leader is None or sign * ex_leader >= cfg.follower_return_pct)
            and breadth_delta >= cfg.minimum_confirmation_breadth_pct_point
        )
        stage = EpisodeStage.CONFIRMED if confirmed else EpisodeStage.EMERGING
        if confirmed and coverage >= cfg.crowded_coverage and any(member.role == "second_wave" for member in members):
            stage = EpisodeStage.CROWDED
        elif confirmed and len(ordered) > responded_count:
            stage = EpisodeStage.SPREADING
        leader_evidence = _leader_evidence(leader, members, ex_leader, coverage, context, quality, direction)
        episode_id = _stable_id("episode", f"{context.trade_date}:{sector_code}:{sector_name}:{direction.value}:{started_at.isoformat()}")
        fixed_members = tuple(
            EpisodeMemberResponse(**{**asdict(member), "episode_id": episode_id})
            for member in members
        )
        result.append(SectorEpisode(
            episode_id=episode_id,
            trade_date=context.trade_date,
            sector_type="ths_industry",
            sector_code=sector_code,
            sector_name=sector_name,
            direction=direction,
            stage=stage,
            started_at=started_at,
            confirmed_at=started_at if confirmed else None,
            last_evidence_at=now,
            ended_at=None,
            market_context_at_start_id=context.context_id,
            market_context_at_confirm_id=context.context_id if confirmed else None,
            eligible_count=eligible_count,
            observed_count=len(observed),
            responded_count=responded_count,
            coverage_ratio=round(coverage, 6),
            coverage_peak=round(coverage, 6),
            median_lag_seconds=int(median(lags)) if lags else None,
            p75_lag_seconds=_percentile_int(lags, 0.75),
            sector_equal_return_pct=_round(equal_return),
            sector_median_return_pct=_round(median_return),
            ex_leader_return_pct=_round(ex_leader),
            weighted_return_pct=None,
            benchmark_excess_pct=_round(equal_return - _number_or_none(context.indices.get("benchmarkReturnPct"), 0.0)),
            breadth_delta_pct_point=_round(breadth_delta),
            amount_acceleration=None,
            leader_candidates=(leader_evidence,),
            suppressor_candidates=(),
            confidence=_episode_confidence(leader_evidence, coverage, ex_leader, confirmed, context, quality),
            data_quality=quality,
            failure_reasons=() if confirmed else ("完整成分股响应不足",),
            members=fixed_members,
            provenance={"algorithmVersion": cfg.algorithm_version, "thresholdSetId": cfg.threshold_set_id},
        ))
    return sorted(result, key=lambda episode: (episode.started_at, episode.sector_name), reverse=True)


def _build_advice(episode: SectorEpisode, context: MarketContextSnapshot, cfg: TransmissionConfig, now: datetime) -> TradeReferenceAdvice:
    quality_ok = episode.data_quality.grade in {QualityGrade.A, QualityGrade.B}
    if episode.stage is EpisodeStage.EMERGING or episode.confidence.level in {ConfidenceLevel.LOW, ConfidenceLevel.INSUFFICIENT}:
        action = AdviceAction.WAIT_CONFIRMATION
    elif episode.stage is EpisodeStage.CROWDED:
        action = AdviceAction.DO_NOT_CHASE
    elif episode.stage in {EpisodeStage.DECAYING, EpisodeStage.FAILED, EpisodeStage.REVERSED}:
        action = AdviceAction.REDUCE_AVOID_REFERENCE if episode.direction is Direction.RISE else AdviceAction.INVALIDATION_EXIT_REFERENCE
    elif quality_ok and episode.confidence.score >= 0.75 and episode.coverage_ratio >= cfg.minimum_confirmation_coverage:
        action = AdviceAction.PROBE_REFERENCE
    else:
        action = AdviceAction.OBSERVE
    return TradeReferenceAdvice(
        advice_id=_stable_id("advice", f"{episode.episode_id}:{action.value}:{cfg.algorithm_version}"),
        episode_id=episode.episode_id,
        target_type="sector",
        target_code=episode.sector_code,
        target_name=episode.sector_name,
        position_state="both",
        action_class=action,
        generated_at=now,
        as_of=episode.last_evidence_at,
        valid_until=now + timedelta(seconds=cfg.confirmation_window_seconds),
        horizon="intraday",
        confidence=round(episode.confidence.score, 6),
        entry_conditions=("板块完整成分股继续同向响应", "成交确认不低于当前证据",) if action is AdviceAction.PROBE_REFERENCE else (),
        invalidation_conditions=("板块去带头股收益反向", "传播覆盖明显回落",),
        risk_reference={"type": "episode_invalidation", "source": "sector_episode"},
        reward_reference={"type": "forward_excess", "horizon": "5m/15m/30m"},
        market_compatibility={"regime": context.regime, "compatible": context.regime not in {"broad_risk_off"}},
        evidence=tuple(episode.confidence.reasons),
        counter_evidence=tuple(episode.confidence.counter_evidence),
        disclaimer_code="MARKET_REFERENCE_NOT_AUTOMATED_TRADE",
        policy_version=cfg.algorithm_version,
        data_quality=episode.data_quality,
    )


def _leader_evidence(
    leader: StockSignalEvent,
    members: list[EpisodeMemberResponse],
    ex_leader: float | None,
    coverage: float,
    context: MarketContextSnapshot,
    quality: DataQuality,
    direction: Direction,
) -> dict[str, Any]:
    following = [member for member in members if member.code != leader.code]
    follow_score = min(1.0, len(following) / max(3, len(members)))
    timing_score = 1.0
    persistence = 1.0 if ex_leader is not None and (ex_leader >= 0 if direction is Direction.RISE else ex_leader <= 0) else 0.0
    role = "emotional_leader" if follow_score >= 0.5 and persistence >= 0.5 else "independent_move"
    if coverage < 0.15 and role == "emotional_leader":
        role = "weight_driver"
    return {
        "code": leader.code,
        "name": leader.name,
        "role": role,
        "score": round(0.25 * timing_score + 0.20 * _clip01(abs(leader.sector_excess_pct or 0.0) / 2.0) + 0.20 * follow_score + 0.10 * persistence + 0.10 * _clip01(context.regime_strength) + 0.15 * _quality_score(quality), 6),
        "timingLead": timing_score,
        "relativeStrength": _clip01(abs(leader.sector_excess_pct or 0.0) / 2.0),
        "followerResponse": follow_score,
        "exLeaderPersistence": persistence,
    }


def _episode_confidence(
    leader: dict[str, Any],
    coverage: float,
    ex_leader: float | None,
    confirmed: bool,
    context: MarketContextSnapshot,
    quality: DataQuality,
) -> ConfidenceEvidence:
    follower = _clip01(coverage / 0.5)
    persistence = float(leader.get("exLeaderPersistence") or 0.0)
    score = _clip01(
        0.25 * float(leader.get("timingLead") or 0.0)
        + 0.20 * float(leader.get("relativeStrength") or 0.0)
        + 0.15 * _clip01(float(leader.get("activityConfirmation") or 0.5))
        + 0.20 * follower
        + 0.10 * persistence
        + 0.10 * _clip01(context.regime_strength)
        - (0.25 if quality.grade is QualityGrade.C else 0.55 if quality.grade is QualityGrade.D else 0.0)
    )
    level = ConfidenceLevel.HIGH if confirmed and score >= 0.75 and quality.grade in {QualityGrade.A, QualityGrade.B} else ConfidenceLevel.MEDIUM if score >= 0.5 else ConfidenceLevel.LOW if score > 0 else ConfidenceLevel.INSUFFICIENT
    reasons = ["存在时间领先候选", f"完整成分股覆盖 {coverage:.1%}"]
    if ex_leader is not None:
        reasons.append(f"去带头股收益 {ex_leader:+.2f}%")
    counter = [] if confirmed else ["尚未达到完整板块确认门槛"]
    return ConfidenceEvidence(
        score=round(score, 6),
        level=level,
        timing_lead=float(leader.get("timingLead") or 0.0),
        relative_strength=float(leader.get("relativeStrength") or 0.0),
        activity_confirmation=float(leader.get("activityConfirmation") or 0.5),
        follower_response=follower,
        ex_leader_persistence=persistence,
        market_compatibility=_clip01(context.regime_strength),
        quality_penalty=0.25 if quality.grade in {QualityGrade.C, QualityGrade.D} else 0.0,
        reasons=tuple(reasons),
        counter_evidence=tuple(counter),
    )


def _signal_confidence(value: float, sector_excess: float | None, row: dict[str, Any], quality: DataQuality, context: MarketContextSnapshot) -> ConfidenceEvidence:
    relative = _clip01(abs(sector_excess or 0.0) / 2.0)
    activity = 0.7 if _number_or_none(row.get("amount")) not in (None, 0.0) else 0.35
    score = _clip01(0.35 * _clip01(abs(value) / 3.0) + 0.25 * relative + 0.20 * activity + 0.20 * _quality_score(quality))
    return ConfidenceEvidence(
        score=round(score, 6),
        level=ConfidenceLevel.HIGH if score >= 0.75 else ConfidenceLevel.MEDIUM if score >= 0.5 else ConfidenceLevel.LOW,
        timing_lead=0.5,
        relative_strength=relative,
        activity_confirmation=activity,
        follower_response=0.0,
        ex_leader_persistence=0.0,
        market_compatibility=_clip01(context.regime_strength),
        quality_penalty=0.0 if quality.grade in {QualityGrade.A, QualityGrade.B} else 0.25,
        reasons=(f"窗口收益 {value:+.2f}%", "相对板块超额已计算" if sector_excess is not None else "板块基准不足"),
        counter_evidence=() if quality.grade in {QualityGrade.A, QualityGrade.B} else ("数据质量不足",),
    )


def _rows_quality(rows: list[dict[str, Any]], *, eligible_count_override: int | None = None) -> DataQuality:
    observed = len(rows)
    valid = sum(1 for row in rows if _number_or_none(row.get("last_price")) is not None)
    mapped = sum(1 for row in rows if _sector(row)[1] != "未分类")
    missing = sum(1 for row in rows if _number_or_none(row.get("last_price")) is None or not row.get("captured_at"))
    return DataQuality.from_counts(
        eligible_count=eligible_count_override if eligible_count_override is not None else observed,
        observed_count=valid,
        fresh_count=valid,
        mapped_count=mapped,
        missing_count=missing,
        warnings=("仅依据当前采集覆盖评估",) if observed and valid < observed else (),
    )


def _default_context(now: datetime, rows: list[dict[str, Any]]) -> MarketContextSnapshot:
    trade_date = (now.date()).isoformat()
    context_id = _stable_id("context", f"{trade_date}:{now.isoformat()}")
    return MarketContextSnapshot(
        context_id=context_id,
        trade_date=trade_date,
        bucket_at=now,
        as_of=now,
        trading_phase="intraday",
        regime="unknown",
        regime_strength=0.0,
        breadth={"observedCount": len(rows)},
        data_quality=_rows_quality(rows),
        provenance={"source": "market_indicator_capture"},
    )


def _datetime_from_rows(rows: list[dict[str, Any]]) -> datetime | None:
    for row in rows:
        value = _as_datetime(row.get("captured_at"))
        if value:
            return value
    return None


def _capture_time_safe(rows: list[dict[str, Any]]) -> datetime:
    """Return a deterministic capture time for outcome replay ordering."""
    return _datetime_from_rows(rows) or datetime.min.replace(tzinfo=timezone.utc)


def _sector(row: dict[str, Any]) -> tuple[str | None, str]:
    code = row.get("industry_code")
    name = str(row.get("industry") or row.get("industry_name") or "未分类").strip() or "未分类"
    return (str(code).strip() if code else None, name)


def _cohorts(row: dict[str, Any]) -> tuple[str, ...]:
    values = ["market_all"]
    if row.get("pool_tier") == "historical_limit" or row.get("candidate_tier") == "hot":
        values.append("historical_limit")
    if row.get("watchlist"):
        values.append("watchlist")
    return tuple(values)


def _limit_transition(previous: dict[str, Any] | None, current: dict[str, Any]) -> str | None:
    before = str((previous or {}).get("limit_status") or "normal")
    after = str(current.get("limit_status") or "normal")
    if before == after or after == "normal":
        return None
    return f"{before}->{after}"


def _amount_velocity(row: dict[str, Any], previous: dict[str, Any] | None, gap_seconds: int) -> float | None:
    current = _number_or_none(row.get("amount"))
    before = _number_or_none((previous or {}).get("amount"))
    if current is None or before is None or current < before:
        return None
    return round((current - before) / max(1, gap_seconds), 6)


def _percentile_int(values: list[int], percentile: float) -> int | None:
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, int(round((len(ordered) - 1) * percentile))))
    return int(ordered[index])


def _stable_id(prefix: str, value: str) -> str:
    import hashlib

    return f"mi_{prefix}_{hashlib.sha256(value.encode('utf-8')).hexdigest()[:24]}"


def _number_or_none(value: Any, default: float | None = None) -> float | None:
    if value in (None, ""):
        return default
    try:
        number = float(str(value).replace(",", "").replace("%", ""))
        return number if isfinite(number) else default
    except (TypeError, ValueError):
        return default


def _as_datetime(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if value in (None, ""):
        return None
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    except ValueError:
        return None


def _round(value: float | None, digits: int = 6) -> float | None:
    return round(value, digits) if value is not None else None


def _clip01(value: float) -> float:
    return min(1.0, max(0.0, float(value)))


def _quality_score(quality: DataQuality) -> float:
    return {QualityGrade.A: 1.0, QualityGrade.B: 0.85, QualityGrade.C: 0.55, QualityGrade.D: 0.0}[quality.grade]


def _plain_code(value: Any) -> str:
    text = str(value or "").strip().lower()
    if text.startswith(("sh", "sz", "bj")):
        text = text[2:]
    return text.zfill(6)


def to_jsonable(value: Any) -> Any:
    """Serialize contracts without leaking Enum objects or datetime instances."""
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, StrEnum):
        return value.value
    if isinstance(value, tuple):
        return [to_jsonable(item) for item in value]
    if isinstance(value, list):
        return [to_jsonable(item) for item in value]
    if isinstance(value, dict):
        return {str(key): to_jsonable(item) for key, item in value.items()}
    if hasattr(value, "__dataclass_fields__"):
        return to_jsonable(asdict(value))
    return value
