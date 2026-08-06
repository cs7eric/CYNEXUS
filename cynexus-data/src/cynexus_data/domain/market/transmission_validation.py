from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Iterable

from .transmission import build_transmission_snapshot, to_jsonable


@dataclass(frozen=True)
class ValidationIssue:
    code: str
    message: str
    severity: str = "error"
    record_id: str | None = None


@dataclass(frozen=True)
class ValidationReport:
    passed: bool
    checked_captures: int
    checked_signals: int
    checked_episodes: int
    issues: tuple[ValidationIssue, ...] = ()
    metrics: dict[str, float] | None = None


def validate_transmission_snapshot(snapshot: dict[str, Any], *, as_of: datetime | None = None) -> ValidationReport:
    """Validate contract shape and event-time causality for one capture.

    The validator rejects any signal, episode, or advice whose published/as-of
    timestamps are later than the supplied replay cut-off.  It is intentionally
    independent of providers and persistence so it can run in CI and offline
    replay jobs.
    """
    issues: list[ValidationIssue] = []
    cutoff = as_of
    signal_events = list(snapshot.get("signal_events") or [])
    episodes = list(snapshot.get("episodes") or [])
    advice = list(snapshot.get("advice") or [])
    for event in signal_events:
        event_id = str(event.get("signal_event_id") or "")
        event_at = _parse(event.get("event_at"))
        published_at = _parse(event.get("published_at"))
        if event_at is None or published_at is None:
            issues.append(ValidationIssue("missing_event_time", "signal event must include event_at and published_at", record_id=event_id))
        elif published_at < event_at:
            issues.append(ValidationIssue("publish_before_event", "published_at precedes event_at", record_id=event_id))
        if cutoff and published_at and published_at > cutoff:
            issues.append(ValidationIssue("future_leak_signal", "signal was published after replay cut-off", record_id=event_id))
    for episode in episodes:
        episode_id = str(episode.get("episode_id") or "")
        started = _parse(episode.get("started_at"))
        confirmed = _parse(episode.get("confirmed_at"))
        last = _parse(episode.get("last_evidence_at"))
        if started and confirmed and confirmed < started:
            issues.append(ValidationIssue("episode_time_reversal", "confirmed_at precedes started_at", record_id=episode_id))
        if started and last and last < started:
            issues.append(ValidationIssue("episode_evidence_reversal", "last_evidence_at precedes started_at", record_id=episode_id))
        if cutoff and last and last > cutoff:
            issues.append(ValidationIssue("future_leak_episode", "episode evidence is after replay cut-off", record_id=episode_id))
    for item in advice:
        advice_id = str(item.get("advice_id") or "")
        generated = _parse(item.get("generated_at"))
        as_of_time = _parse(item.get("as_of"))
        if generated and as_of_time and generated < as_of_time:
            issues.append(ValidationIssue("advice_time_reversal", "generated_at precedes as_of", record_id=advice_id))
        if cutoff and generated and generated > cutoff:
            issues.append(ValidationIssue("future_leak_advice", "advice is after replay cut-off", record_id=advice_id))
    return ValidationReport(
        passed=not any(issue.severity == "error" for issue in issues),
        checked_captures=1,
        checked_signals=len(signal_events),
        checked_episodes=len(episodes),
        issues=tuple(issues),
    )


def replay_captures(captures: Iterable[list[dict[str, Any]]]) -> ValidationReport:
    """Replay captures in event-time order using only prior quotes as state."""
    previous: dict[str, dict[str, Any]] = {}
    issues: list[ValidationIssue] = []
    captures_checked = 0
    signal_count = 0
    episode_count = 0
    for rows in sorted(captures, key=_capture_time):
        captures_checked += 1
        snapshot = build_transmission_snapshot(rows, previous, detected_at=_capture_time(rows))
        payload = to_jsonable(snapshot)
        report = validate_transmission_snapshot(payload, as_of=_capture_time(rows))
        issues.extend(report.issues)
        signal_count += report.checked_signals
        episode_count += report.checked_episodes
        for row in rows:
            code = str(row.get("code") or "")
            if code:
                previous[code] = dict(row)
    return ValidationReport(
        passed=not any(issue.severity == "error" for issue in issues),
        checked_captures=captures_checked,
        checked_signals=signal_count,
        checked_episodes=episode_count,
        issues=tuple(issues),
        metrics={"signal_rate": signal_count / max(1, captures_checked), "episode_rate": episode_count / max(1, captures_checked)},
    )


def _capture_time(rows: list[dict[str, Any]]) -> datetime:
    for row in rows:
        parsed = _parse(row.get("captured_at"))
        if parsed:
            return parsed
    raise ValueError("capture rows must include captured_at")


def _parse(value: Any) -> datetime | None:
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if value in (None, ""):
        return None
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    except ValueError:
        return None
