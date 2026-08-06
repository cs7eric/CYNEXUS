from datetime import datetime, timedelta, timezone

from cynexus_data.domain.market.transmission_validation import replay_captures, validate_transmission_snapshot


def test_replay_uses_only_prior_capture_and_passes_causality():
    first = datetime(2026, 8, 5, 2, 0, tzinfo=timezone.utc)
    second = first + timedelta(seconds=5)
    captures = [
        [{"code": "000001", "industry": "半导体", "last_price": 10.0, "captured_at": first}],
        [{"code": "000001", "industry": "半导体", "last_price": 10.2, "captured_at": second}],
    ]
    report = replay_captures(captures)
    assert report.passed
    assert report.checked_captures == 2
    assert report.checked_signals == 1


def test_validator_rejects_future_signal():
    cutoff = datetime(2026, 8, 5, 2, 0, tzinfo=timezone.utc)
    report = validate_transmission_snapshot({
        "signal_events": [{
            "signal_event_id": "s1",
            "event_at": cutoff.isoformat(),
            "published_at": (cutoff + timedelta(seconds=1)).isoformat(),
        }],
        "episodes": [],
        "advice": [],
    }, as_of=cutoff)
    assert not report.passed
    assert any(issue.code == "future_leak_signal" for issue in report.issues)
