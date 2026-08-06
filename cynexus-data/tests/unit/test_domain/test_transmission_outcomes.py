from datetime import datetime, timedelta, timezone

from cynexus_data.domain.market.transmission import evaluate_episode_outcome, to_jsonable


def _rows(at, leader, follower):
    return [
        {"code": "000001", "name": "Leader", "industry_code": "I1", "industry": "Theme", "last_price": leader, "captured_at": at},
        {"code": "000002", "name": "Follower", "industry_code": "I1", "industry": "Theme", "last_price": follower, "captured_at": at},
    ]


def test_evaluate_episode_outcome_reports_forward_windows_and_sustained_result():
    start = datetime(2026, 8, 5, 1, 30, tzinfo=timezone.utc)
    episode = {
        "episode_id": "episode-1",
        "trade_date": "2026-08-05",
        "sector_code": "I1",
        "sector_name": "Theme",
        "direction": "rise",
        "started_at": start,
        "eligible_count": 2,
        "observed_count": 2,
        "leader_candidates": [{"code": "000001"}],
    }
    captures = [
        _rows(start, 10.0, 10.0),
        _rows(start + timedelta(minutes=5), 10.5, 10.3),
        _rows(start + timedelta(minutes=15), 10.8, 10.6),
        _rows(start + timedelta(minutes=30), 11.0, 10.9),
    ]
    outcome = evaluate_episode_outcome(episode, captures, evaluated_at=start + timedelta(hours=1))
    payload = to_jsonable(outcome)
    assert payload["forward_sector_return_5m_pct"] > 0
    assert payload["forward_sector_return_30m_pct"] > 0
    assert payload["outcome"] == "sustained"


def test_evaluate_episode_outcome_rejects_missing_anchor_data():
    start = datetime(2026, 8, 5, 1, 30, tzinfo=timezone.utc)
    episode = {
        "episode_id": "episode-2",
        "sector_code": "I1",
        "sector_name": "Theme",
        "direction": "rise",
        "started_at": start,
        "eligible_count": 2,
        "leader_candidates": [{"code": "000001"}],
    }
    outcome = evaluate_episode_outcome(episode, [[{"code": "000001", "captured_at": start}]], evaluated_at=start)
    assert outcome.outcome == "insufficient_data"
