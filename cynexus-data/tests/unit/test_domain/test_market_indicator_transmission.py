from datetime import datetime, timedelta, timezone

from cynexus_data.domain.market.transmission import (
    AdviceAction,
    EpisodeStage,
    QualityGrade,
    TransmissionConfig,
    build_transmission_snapshot,
    to_jsonable,
)


def _row(code: str, industry: str, previous: float, current: float, captured_at: datetime, *, pool_tier: str = "market_all"):
    return {
        "code": code,
        "name": code,
        "market": "SZ",
        "industry": industry,
        "last_price": current,
        "prev_close": previous,
        "change_pct": (current / previous - 1) * 100,
        "amount": 100_000_000,
        "captured_at": captured_at,
        "pool_tier": pool_tier,
        "limit_status": "normal",
    }


def test_build_transmission_snapshot_identifies_sector_episode_and_leader():
    captured = datetime(2026, 8, 5, 2, 0, tzinfo=timezone.utc)
    rows = [
        _row("000001", "半导体", 10, 10.20, captured, pool_tier="historical_limit"),
        _row("000002", "半导体", 10, 10.08, captured),
        _row("000003", "半导体", 10, 10.06, captured),
        _row("000004", "半导体", 10, 10.05, captured),
    ]
    previous = {row["code"]: {**row, "last_price": 10.0, "captured_at": captured - timedelta(seconds=60)} for row in rows}

    result = build_transmission_snapshot(rows, previous, config=TransmissionConfig(rapid_return_pct=0.5, follower_return_pct=0.3))

    assert len(result["signal_events"]) == 4
    assert len(result["episodes"]) == 1
    episode = result["episodes"][0]
    assert episode.stage is EpisodeStage.CONFIRMED
    assert episode.sector_name == "半导体"
    assert episode.leader_candidates[0]["code"] == "000001"
    assert episode.coverage_ratio == 1.0
    assert result["advice"][0].action_class is AdviceAction.PROBE_REFERENCE


def test_quality_is_explicit_and_jsonable():
    captured = datetime(2026, 8, 5, 2, 0, tzinfo=timezone.utc)
    rows = [_row("000001", "未分类", 10, 10.1, captured)]
    previous = {"000001": {"last_price": 10, "captured_at": captured - timedelta(seconds=60)}}
    result = build_transmission_snapshot(rows, previous)

    assert result["quality"].grade is QualityGrade.B
    encoded = to_jsonable(result)
    assert encoded["context"]["bucket_at"].endswith("+00:00")
    assert encoded["episodes"][0]["stage"] == "emerging"


def test_no_previous_quote_does_not_create_signal():
    captured = datetime(2026, 8, 5, 2, 0, tzinfo=timezone.utc)
    result = build_transmission_snapshot([_row("000001", "银行", 10, 11, captured)], {})
    assert result["signal_events"] == []
    assert result["episodes"] == []


def test_priority_scope_cannot_confirm_against_full_sector_eligibility():
    captured = datetime(2026, 8, 5, 2, 0, tzinfo=timezone.utc)
    rows = [
        _row("000001", "半导体", 10, 10.20, captured, pool_tier="historical_limit"),
        _row("000002", "半导体", 10, 10.08, captured, pool_tier="historical_limit"),
        _row("000003", "半导体", 10, 10.06, captured, pool_tier="historical_limit"),
    ]
    previous = {row["code"]: {**row, "last_price": 10.0, "captured_at": captured - timedelta(seconds=60)} for row in rows}

    result = build_transmission_snapshot(
        rows,
        previous,
        config=TransmissionConfig(
            rapid_return_pct=0.5,
            follower_return_pct=0.3,
            eligible_count_by_sector={"半导体": 30},
            full_confirmation_required=True,
        ),
    )

    assert result["confirmation_scope"]["full_confirmation_required"] is True
    assert result["quality"].coverage_ratio == 0.1
    assert result["episodes"][0].stage is EpisodeStage.EMERGING
    assert result["advice"][0].action_class is AdviceAction.WAIT_CONFIRMATION
