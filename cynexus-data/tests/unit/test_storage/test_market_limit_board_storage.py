from datetime import datetime, timezone

from cynexus_data.storage.postgres.market_limit_board import _snapshot_params
from cynexus_data.storage.postgres.market_limit_board_session import _summary_params


def test_limit_board_snapshot_id_is_stable_and_payload_is_json():
    captured_at = datetime(2026, 8, 11, 2, 0, tzinfo=timezone.utc)
    row = {"code": "000001", "captured_at": captured_at, "payload": {"limit_status": "limit_up_sealed"}}

    first = _snapshot_params(row, source="eltdx.quotes")
    second = _snapshot_params(row, source="eltdx.quotes")

    assert first["id"] == second["id"]
    assert first["trade_date"].isoformat() == "2026-08-11"
    assert first["payload"] == '{"limit_status": "limit_up_sealed"}'


def test_limit_board_session_summary_id_is_stable_and_has_no_raw_payload():
    captured_at = datetime(2026, 8, 11, 2, 0, tzinfo=timezone.utc)
    row = {
        "trade_date": captured_at.date(),
        "trade_session": "morning",
        "code": "000001",
        "direction": "up",
        "trend_features": {"sealed_amount_points": [[0, 100]]},
    }

    first = _summary_params(row, source="eltdx.quotes.best_quote")
    second = _summary_params(row, source="eltdx.quotes.best_quote")

    assert first["id"] == second["id"]
    assert first["source"] == "eltdx.quotes.best_quote"
    assert first["trend_features"] == '{"sealed_amount_points": [[0, 100]]}'
    assert "payload" not in first
